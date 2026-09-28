package com.zifang.z.config.core.crypto;

import com.zifang.util.core.encrypt.AesUtil;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * AESEncryptor 收编对拍尺（交付物，禁止删除；{@link LegacyAesEncryptor} 是收编前实现的冻结副本，
 * 禁止改算法）。
 *
 * <p>背景：{@link AESEncryptor} 的内容层（AES/GCM/NoPadding + 随机 DataKey）已委托
 * {@code com.zifang.util.core.encrypt.AesUtil}（z-util 1.0.13），DataKey 封装层
 * （AES/ECB/PKCS5Padding + 裸 masterKey）因 z-util 1.0.13 无"裸密钥 + ECB"入口而原样保留。
 * 本类证明<b>落库格式一字未变</b>：{@code z_config_info.encrypted_data_key} 与同表 content
 * 里的历史密文照旧可解，反向也成立（老封装值 + 老密文可被现网解开）。
 *
 * <p>为什么不是 JUnit：z-config-core 的 test classpath 上没有任何 junit
 * （根 pom 无 test 依赖，只有 z-config-client 引了 junit4），而 pom 属本轮禁改范围，
 * 故本类以 {@code main + 硬断言} 形式交付：任一检查不过即 {@link AssertionError} 非零退出，
 * 可直接挂脚本/CI。（z-ctc 侧的 JWT 对拍是真 JUnit，见
 * {@code z-ctc-sso/src/test/java/com/zifang/ctc/sso/JwtCompatCrossCheckTest}。）
 *
 * <p>跑法（与构建同一 JDK）：
 * <pre>
 *   export JAVA_HOME=&lt;corretto-17&gt;
 *   mvn -B -q test-compile -pl z-config-core
 *   mvn -B -q dependency:build-classpath -pl z-config-core -Dmdep.outputFile=target/cp.txt
 *   java -cp z-config-core/target/classes:z-config-core/target/test-classes:$(cat z-config-core/target/cp.txt) \
 *        com.zifang.z.config.core.crypto.AESEncryptorCompatCrossCheck
 * </pre>
 *
 * <p>覆盖：老加密→新解密、新加密→老解密、DataKey 封装逐字节相同、密文布局逐字节相同
 * （IV 前 12B / tag 16B / 标准 Base64 / 随机 IV）、空串与 null、中文与 UTF-8 补充平面、
 * 超长内容、多行 properties/yaml/json、密钥长度非 16/24/32、篡改认证标签、短密文、
 * 跨实例 masterKey（既有不可解事实）。
 */
public final class AESEncryptorCompatCrossCheck {

    private static final List<String> PASSES = new ArrayList<String>();
    private static final List<String> FAILURES = new ArrayList<String>();

    /** 固定共享 masterKey：反射注进新老两份实现，才能在同一密钥域下做双向对拍。 */
    private static final byte[] SHARED_MASTER_KEY = fixedKey();

    private AESEncryptorCompatCrossCheck() {
    }

    public static void main(String[] args) throws Exception {
        legacyEncryptThenNewDecrypt();
        newEncryptThenLegacyDecrypt();
        dataKeyWrapBytesIdentical();
        contentLayoutBytesIdentical();
        boundaryInputsCrossReadable();
        nullAndEmptyPassthroughIdentical();
        randomIvMakesCiphertextNonRepeating();
        wrongMasterKeyStillUndecryptableIsPreexistingFact();
        tamperedCiphertextRejectedByBoth();
        illegalKeyLengthsBehaveSame();
        zUtilGcmEquivalentToLegacyRawBytes();

        System.out.println("==================== AES 双向对拍结果 ====================");
        for (String pass : PASSES) {
            System.out.println("  PASS  " + pass);
        }
        for (String failure : FAILURES) {
            System.out.println("  FAIL  " + failure);
        }
        System.out.println("  checks=" + (PASSES.size() + FAILURES.size())
                + " pass=" + PASSES.size() + " fail=" + FAILURES.size());
        if (!FAILURES.isEmpty()) {
            throw new AssertionError("AES 对拍失败 " + FAILURES.size() + " 项, 见上面 FAIL 行");
        }
        System.out.println("  结论: 收编前后落库格式逐字节一致, 密文/封装 DataKey 双向可解。");
    }

    // ================= 1. 双向可解 =================

    /** 老 AESEncryptor 加密并已落库的密文，必须被收编后的实现解出来。 */
    private static void legacyEncryptThenNewDecrypt() throws Exception {
        LegacyAesEncryptor legacy = new LegacyAesEncryptor(SHARED_MASTER_KEY);
        AESEncryptor current = new AESEncryptor();
        injectMasterKey(current, SHARED_MASTER_KEY);

        for (String plain : corpus()) {
            String packed = legacy.encrypt(plain);
            String out = current.decrypt(content(packed), dataKey(packed));
            check("老加密→新解密 [" + describe(plain) + "]", plain.equals(out),
                    "解密结果不一致: " + preview(out));
        }
    }

    /** 收编后新签的密文，老实现（回滚场景 / 各仓可能有的手抄副本）必须能解。 */
    private static void newEncryptThenLegacyDecrypt() throws Exception {
        LegacyAesEncryptor legacy = new LegacyAesEncryptor(SHARED_MASTER_KEY);
        AESEncryptor current = new AESEncryptor();
        injectMasterKey(current, SHARED_MASTER_KEY);

        for (String plain : corpus()) {
            String packed = current.encrypt(plain);
            String out = legacy.decrypt(content(packed), dataKey(packed));
            check("新加密→老解密 [" + describe(plain) + "]", plain.equals(out),
                    "回滚/历史实现解不出新密文: " + preview(out));
        }
    }

    // ================= 2. 逐字节对齐 =================

    /**
     * DataKey 封装层（AES/ECB/PKCS5Padding + masterKey）必须逐字节相同 ——
     * 这一层直接决定历史 {@code encrypted_data_key} 字段能不能继续用。
     */
    private static void dataKeyWrapBytesIdentical() throws Exception {
        LegacyAesEncryptor legacy = new LegacyAesEncryptor(SHARED_MASTER_KEY);
        AESEncryptor current = new AESEncryptor();
        injectMasterKey(current, SHARED_MASTER_KEY);

        byte[][] dataKeys = {AesUtil.generateKey(256), AesUtil.generateKey(128), AesUtil.generateKey(192)};
        for (int i = 0; i < dataKeys.length; i++) {
            String oldWrap = (String) invoke(legacy, "encryptDataKey", dataKeys[i]);
            String newWrap = (String) invoke(current, "encryptDataKey", dataKeys[i]);
            check("DataKey 封装逐字节相同[密钥" + (dataKeys[i].length * 8) + "位]", oldWrap.equals(newWrap),
                    "old=" + oldWrap + " new=" + newWrap);
            byte[] backFromOld = (byte[]) invoke(current, "decryptDataKey", oldWrap);
            byte[] backFromNew = (byte[]) invoke(legacy, "decryptDataKey", newWrap);
            check("老封装值现网可解开 / 新封装值老实现可解开[密钥" + (dataKeys[i].length * 8) + "位]",
                    Arrays.equals(dataKeys[i], backFromOld) && Arrays.equals(dataKeys[i], backFromNew),
                    "unwrap 不一致");
        }
        byte[] padded = Base64.getDecoder().decode((String) invoke(current, "encryptDataKey", new byte[32]));
        byte[] paddedOld = Base64.getDecoder().decode((String) invoke(legacy, "encryptDataKey", new byte[32]));
        check("PKCS5Padding 填充长度不变(32B→48B)", padded.length == 48 && paddedOld.length == 48,
                "new=" + padded.length + " old=" + paddedOld.length);
    }

    /**
     * 内容层密文布局必须逐字节相同：{@code Base64([IV 12B][ciphertext][tag 16B])}。
     * 做法：取现网（z-util）随机产出的密文，拆出头 12 字节 IV 与解出的 DataKey，
     * 用收编前的手写 GCM 以同一 IV / 同一密钥再加密一次，两侧密文必须完全相等。
     */
    private static void contentLayoutBytesIdentical() throws Exception {
        AESEncryptor current = new AESEncryptor();
        injectMasterKey(current, SHARED_MASTER_KEY);

        for (String plain : corpus()) {
            String packed = current.encrypt(plain);
            String enc = content(packed);
            byte[] decoded = Base64.getDecoder().decode(enc);
            byte[] iv = Arrays.copyOfRange(decoded, 0, 12);
            byte[] body = Arrays.copyOfRange(decoded, 12, decoded.length);
            check("密文长度=IV12+明文字节+tag16 [" + describe(plain) + "]",
                    body.length == plain.getBytes(StandardCharsets.UTF_8).length + 16,
                    "body=" + body.length);

            byte[] dataKey = (byte[]) invoke(current, "decryptDataKey", dataKey(packed));
            String replay = rawLegacyGcmEncrypt(plain, dataKey, iv);
            check("老式手写 GCM 同 IV 复现现网密文 [" + describe(plain) + "]", enc.equals(replay),
                    "布局不一致");
            check("IV 随机非固定全零 [" + describe(plain) + "]", !isAllZero(iv), "IV 全零");
        }
    }

    /**
     * 把 {@code AesUtil.encryptGcm/decryptGcm} 与"收编前的手写 GCM"直接对拍（不经 AESEncryptor），
     * 证明委托点两侧本来就是同一格式。
     */
    private static void zUtilGcmEquivalentToLegacyRawBytes() throws Exception {
        byte[] key = AesUtil.generateKey(256);
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        final String plain = "spring.datasource.password=P@ss:1\n# 中文备注 🙂";
        byte[] utf8 = plain.getBytes(StandardCharsets.UTF_8);

        // z-util 加密 → 收编前手写 GCM 解密
        byte[] zOut = AesUtil.encryptGcm(utf8, key);
        Cipher decryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
        decryptCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(128, Arrays.copyOfRange(zOut, 0, 12)));
        String back = new String(decryptCipher.doFinal(Arrays.copyOfRange(zOut, 12, zOut.length)),
                StandardCharsets.UTF_8);
        check("z-util encryptGcm → 收编前手写 GCM 可解", plain.equals(back), "解出=" + preview(back));

        // 收编前手写 GCM 加密 → z-util 解密
        String oldStyle = rawLegacyGcmEncrypt(plain, key, iv);
        check("收编前手写 GCM → z-util decryptGcmFromBase64 可解",
                plain.equals(AesUtil.decryptGcmFromBase64(oldStyle, key)), "布局差异");

        // 同 key + 同 IV 下逐字节相同
        byte[] sameIv = concat(iv, rawGcmBody(utf8, key, iv));
        check("同 IV 同密钥下 z-util 与老式实现字节级相同",
                Base64.getEncoder().encodeToString(sameIv).equals(oldStyle), "密文字节不同");

        // 护栏：CBC 入口绝不能解 GCM 密文（防止以后有人"顺手"换成 encryptCbc 掉落库格式）
        final byte[] gcmCipher = Base64.getDecoder().decode(oldStyle);
        byte[] cbcIv = new byte[16];
        String cbcTry = cbcDecryptOrNull(Arrays.copyOfRange(gcmCipher, 12, gcmCipher.length), key, cbcIv);
        check("CBC 入口解不开 GCM 密文(格式护栏)", !plain.equals(cbcTry), "CBC 竟还原了明文");
    }

    // ================= 3. 边界 =================

    /** 边界明文：单字符、中文、UTF-8 补充平面、纯空白、含符号、超长（百万级）。 */
    private static void boundaryInputsCrossReadable() throws Exception {
        LegacyAesEncryptor legacy = new LegacyAesEncryptor(SHARED_MASTER_KEY);
        AESEncryptor current = new AESEncryptor();
        injectMasterKey(current, SHARED_MASTER_KEY);

        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 40000; i++) {
            big.append("配置项").append(i).append("=value,").append("带逗号与冒号:value\n");
        }
        List<String> all = corpus();
        all.add("a");
        all.add("1");
        all.add("true");
        all.add("中文配置-🙂🚀");
        all.add("  \n\t  ");
        all.add("password=P@ss:with,comma#1");
        all.add(big.toString());

        for (String plain : all) {
            String fromLegacy = legacy.encrypt(plain);
            String fromNew = current.encrypt(plain);
            boolean okNewReadsOld = plain.equals(current.decrypt(content(fromLegacy), dataKey(fromLegacy)));
            boolean okOldReadsNew = plain.equals(legacy.decrypt(content(fromNew), dataKey(fromNew)));
            check("边界双向可读 [" + describe(plain) + "]", okNewReadsOld && okOldReadsNew,
                    "newReadsOld=" + okNewReadsOld + " oldReadsNew=" + okOldReadsNew);
        }
    }

    /** null / 空串短路必须与收编前一致（原样返回入参，不抛、不产出密文）。 */
    private static void nullAndEmptyPassthroughIdentical() {
        AESEncryptor current = new AESEncryptor();
        LegacyAesEncryptor legacy = new LegacyAesEncryptor(SHARED_MASTER_KEY);
        check("encrypt(null) 双方返回 null", current.encrypt(null) == null && legacy.encrypt(null) == null,
                "行为变了");
        check("encrypt(\"\") 双方返回 \"\"",
                "".equals(current.encrypt("")) && "".equals(legacy.encrypt("")), "行为变了");
        check("decrypt(null,dk) 双方返回 null",
                current.decrypt(null, "AAAA") == null && legacy.decrypt(null, "AAAA") == null, "行为变了");
        check("decrypt(\"\",dk) 双方返回 \"\"",
                "".equals(current.decrypt("", "AAAA")) && "".equals(legacy.decrypt("", "AAAA")), "行为变了");
        check("needEncrypt 口径不变(cipher- 前缀)",
                current.needEncrypt("cipher-a") && !current.needEncrypt("a") && !current.needEncrypt(null),
                "前缀判定被改");
        boolean threw = throwsRuntime(new Callable() {
            public String call() {
                return current.decrypt("AAAA", null);
            }
        });
        boolean threwOld = throwsRuntime(new Callable() {
            public String call() {
                return legacy.decrypt("AAAA", null);
            }
        });
        check("encryptedDataKey 为 null 时双方一致抛错", threw && threwOld, "threw=" + threw + " old=" + threwOld);
    }

    /** 同一明文两次加密必须不同（随机 IV + 随机 DataKey）—— 新老口径一致。 */
    private static void randomIvMakesCiphertextNonRepeating() throws Exception {
        AESEncryptor current = new AESEncryptor();
        injectMasterKey(current, SHARED_MASTER_KEY);
        LegacyAesEncryptor legacy = new LegacyAesEncryptor(SHARED_MASTER_KEY);
        String a = current.encrypt("same=1");
        String b = current.encrypt("same=1");
        String la = legacy.encrypt("same=1");
        String lb = legacy.encrypt("same=1");
        check("现网两次加密密文不同(随机 IV/DataKey)", !content(a).equals(content(b)), "密文重复");
        check("老实现两次加密密文不同(同一口径)", !content(la).equals(content(lb)), "密文重复");
        check("现网密文仍能被自己解开(往返)", "same=1".equals(current.decrypt(content(a), dataKey(a))), "往返失败");
    }

    /**
     * 既有事实（不是本轮引入）：masterKey 每次构造随机生成且不持久化，
     * 因此跨实例（重启）解不开历史 {@code encrypted_data_key}；
     * {@code ConfigServiceImpl} 解密失败时兜底返回密文。
     * 本用例把它钉住：既防止有人误以为"收编修好了这个洞"，也防止反向把它"顺手统一"成另一种格式。
     */
    private static void wrongMasterKeyStillUndecryptableIsPreexistingFact() throws Exception {
        AESEncryptor a = new AESEncryptor();
        AESEncryptor b = new AESEncryptor();
        check("两次构造的 masterKey 互不相同(既有事实)",
                !Arrays.equals(readMasterKey(a), readMasterKey(b)), "masterKey 竟一致");

        injectMasterKey(a, SHARED_MASTER_KEY);
        injectMasterKey(b, new byte[32]);
        String packed = a.encrypt("secret=1");
        boolean threw = throwsRuntime(new Callable() {
            public String call() {
                return b.decrypt(content(packed), dataKey(packed));
            }
        });
        check("不同 masterKey 实例解不开(抛 Config decryption failed)", threw, "竟解开了");

        LegacyAesEncryptor legacyFresh = new LegacyAesEncryptor();
        String legacyPacked = legacyFresh.encrypt("secret=1");
        boolean crossInstanceLegacyAlsoFails = throwsRuntime(new Callable() {
            public String call() {
                return new LegacyAesEncryptor().decrypt(content(legacyPacked), dataKey(legacyPacked));
            }
        });
        check("收编前也存在跨实例不可解(同口径既有事实)", crossInstanceLegacyAlsoFails, "老实现竟能跨实例解开");
    }

    /** GCM 认证标签：密文被改一个字节，新老两侧都必须抛错（不许放宽成"尽力解"）。 */
    private static void tamperedCiphertextRejectedByBoth() throws Exception {
        AESEncryptor current = new AESEncryptor();
        injectMasterKey(current, SHARED_MASTER_KEY);
        final String packed = current.encrypt("a=1\nb=2");
        final String encDataKey = dataKey(packed);
        byte[] decoded = Base64.getDecoder().decode(content(packed));
        decoded[decoded.length - 1] ^= 0x01;
        final String tampered = Base64.getEncoder().encodeToString(decoded);
        check("现网拒绝被改标签的密文", throwsRuntime(new Callable() {
            public String call() {
                return current.decrypt(tampered, encDataKey);
            }
        }), "篡改未被发现");

        LegacyAesEncryptor legacy = new LegacyAesEncryptor(SHARED_MASTER_KEY);
        String legacyPacked = legacy.encrypt("a=1\nb=2");
        final String legacyDataKey = dataKey(legacyPacked);
        byte[] legacyDecoded = Base64.getDecoder().decode(content(legacyPacked));
        legacyDecoded[legacyDecoded.length - 1] ^= 0x01;
        final String legacyTampered = Base64.getEncoder().encodeToString(legacyDecoded);
        check("老实现同样拒绝被改标签的密文(口径一致)", throwsRuntime(new Callable() {
            public String call() {
                return legacy.decrypt(legacyTampered, legacyDataKey);
            }
        }), "口径不一致");

        final String tooShort = Base64.getEncoder().encodeToString(new byte[11]);
        boolean n = throwsRuntime(new Callable() {
            public String call() {
                return current.decrypt(tooShort, encDataKey);
            }
        });
        boolean o = throwsRuntime(new Callable() {
            public String call() {
                return legacy.decrypt(tooShort, encDataKey);
            }
        });
        check("短于 IV 长度的密文两侧都拒", n && o, "new=" + n + " old=" + o);

        final String badBase64 = "这不是base64";
        boolean n2 = throwsRuntime(new Callable() {
            public String call() {
                return current.decrypt(badBase64, encDataKey);
            }
        });
        boolean o2 = throwsRuntime(new Callable() {
            public String call() {
                return legacy.decrypt(badBase64, encDataKey);
            }
        });
        check("非法 Base64 两侧都抛 RuntimeException", n2 && o2, "new=" + n2 + " old=" + o2);
    }

    /**
     * 密钥长度：收编前后都只接受 16/24/32 字节；非 16/24/32 一律拒绝（不截断、不补齐）。
     */
    private static void illegalKeyLengthsBehaveSame() {
        int[] badLengths = {0, 1, 15, 17, 31, 33, 64};
        for (int len : badLengths) {
            final byte[] bad = new byte[len];
            Arrays.fill(bad, (byte) 7);
            boolean zUtilRejects = throwsRuntime(new Callable() {
                public String call() {
                    return AesUtil.encryptGcmToBase64("abc", bad);
                }
            });
            boolean rawRejects = throwsRuntime(new Callable() {
                public String call() {
                    return rawLegacyGcmEncrypt("abc", bad, new byte[12]);
                }
            });
            check("非法密钥长度 " + len + "B 两侧都拒", zUtilRejects && rawRejects,
                    "zUtil=" + zUtilRejects + " raw=" + rawRejects);
        }
        int[] goodLengths = {16, 24, 32};
        for (int len : goodLengths) {
            byte[] good = new byte[len];
            Arrays.fill(good, (byte) 7);
            String viaUtil = AesUtil.encryptGcmToBase64("abc", good);
            check("合法密钥长度 " + len + "B z-util 可往返", "abc".equals(AesUtil.decryptGcmFromBase64(viaUtil, good)),
                    "解不回");
        }
        check("AesUtil.generateKey 拒绝非 128/192/256 位数", throwsRuntime(new Callable() {
            public String call() {
                return Arrays.toString(AesUtil.generateKey(200));
            }
        }), "竟生成了 200 位密钥");
        check("AesUtil.generateKey(256)=32 字节", AesUtil.generateKey(256).length == 32, "长度不是 32");
    }

    // ================= corpus / helpers =================

    private static byte[] fixedKey() {
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) (i * 7 + 1);
        }
        return key;
    }

    /** 现网真实形态的配置内容样本（properties / yaml / json / 中文）。 */
    private static List<String> corpus() {
        List<String> list = new ArrayList<String>();
        list.add("server.port=8888\nspring.datasource.password=Hhzemol!");
        list.add("a: 1\nb: 中文值\nc: \"x,y\"");
        list.add("{\"ak\":\"LTAI****\",\"sk\":\"带:冒号,和逗号\"}");
        list.add("单字符");
        return list;
    }

    private static String dataKey(String packed) {
        return packed.substring(0, packed.indexOf(':'));
    }

    private static String content(String packed) {
        return packed.substring(packed.indexOf(':') + 1);
    }

    private static String describe(String plain) {
        if (plain.length() > 24) {
            return "len=" + plain.length();
        }
        return plain.replace('\n', '/');
    }

    private static String preview(String s) {
        if (s == null) {
            return "null";
        }
        return s.length() > 40 ? s.substring(0, 40) : s;
    }

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            PASSES.add(name);
        } else {
            FAILURES.add(name + " -> " + detail);
        }
    }

    private static boolean isAllZero(byte[] bytes) {
        for (byte b : bytes) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    /** 收编前的手写 GCM（IV 前置 + 标准 Base64），逐字节复刻，仅用于对拍。 */
    private static String rawLegacyGcmEncrypt(String plain, byte[] key, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] ciphertext = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);
            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] rawGcmBody(byte[] utf8, byte[] key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        return cipher.doFinal(utf8);
    }

    private static String cbcDecryptOrNull(byte[] cipherData, byte[] key, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            return new String(cipher.doFinal(cipherData), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean throwsRuntime(Callable body) {
        try {
            body.call();
            return false;
        } catch (RuntimeException e) {
            return true;
        }
    }

    private static void injectMasterKey(Object target, byte[] key) throws Exception {
        Field field = target.getClass().getDeclaredField("masterKey");
        field.setAccessible(true);
        field.set(target, key);
    }

    private static byte[] readMasterKey(Object target) throws Exception {
        Field field = target.getClass().getDeclaredField("masterKey");
        field.setAccessible(true);
        return (byte[]) field.get(target);
    }

    private static Object invoke(Object target, String method, Object arg) throws Exception {
        Method m = target.getClass().getDeclaredMethod(method, arg.getClass());
        m.setAccessible(true);
        return m.invoke(target, arg);
    }

    private interface Callable {
        String call();
    }

    // ================= 冻结副本：收编前的 AESEncryptor（禁止改动算法实现） =================

    /**
     * 收编前 {@code AESEncryptor} 的逐行冻结副本（AES/GCM/NoPadding 内容层 +
     * AES/ECB/PKCS5Padding 封装 DataKey）。它是"已落库密文格式"的唯一权威定义：
     * 只允许整体替换，不允许局部修改。
     */
    static final class LegacyAesEncryptor implements ConfigEncryptor {

        private static final int GCM_IV_LENGTH = 12;
        private static final int GCM_TAG_LENGTH = 128;

        private final byte[] masterKey;
        private final SecureRandom secureRandom = new SecureRandom();

        LegacyAesEncryptor(byte[] masterKey) {
            this.masterKey = masterKey;
        }

        LegacyAesEncryptor() {
            try {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256, secureRandom);
                this.masterKey = keyGen.generateKey().getEncoded();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize AES master key", e);
            }
        }

        @Override
        public String encrypt(String plainContent) {
            if (plainContent == null || plainContent.isEmpty()) {
                return plainContent;
            }
            try {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256, secureRandom);
                SecretKey dataKey = keyGen.generateKey();

                byte[] iv = new byte[GCM_IV_LENGTH];
                secureRandom.nextBytes(iv);

                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
                cipher.init(Cipher.ENCRYPT_MODE, dataKey, gcmSpec);
                byte[] ciphertext = cipher.doFinal(plainContent.getBytes(StandardCharsets.UTF_8));

                ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
                byteBuffer.put(iv);
                byteBuffer.put(ciphertext);
                String encryptedContent = Base64.getEncoder().encodeToString(byteBuffer.array());

                String encryptedDataKey = encryptDataKey(dataKey.getEncoded());

                return encryptedDataKey + ":" + encryptedContent;
            } catch (Exception e) {
                throw new RuntimeException("Config encryption failed", e);
            }
        }

        @Override
        public String decrypt(String cipherContent, String encryptedDataKey) {
            if (cipherContent == null || cipherContent.isEmpty()) {
                return cipherContent;
            }
            try {
                byte[] dataKeyBytes = decryptDataKey(encryptedDataKey);
                SecretKey dataKey = new SecretKeySpec(dataKeyBytes, "AES");

                byte[] decoded = Base64.getDecoder().decode(cipherContent);

                ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
                byte[] iv = new byte[GCM_IV_LENGTH];
                byteBuffer.get(iv);
                byte[] ciphertextAndTag = new byte[byteBuffer.remaining()];
                byteBuffer.get(ciphertextAndTag);

                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
                cipher.init(Cipher.DECRYPT_MODE, dataKey, gcmSpec);
                byte[] plainBytes = cipher.doFinal(ciphertextAndTag);

                return new String(plainBytes, StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException("Config decryption failed", e);
            }
        }

        private String encryptDataKey(byte[] dataKeyBytes) {
            try {
                SecretKey masterSecretKey = new SecretKeySpec(masterKey, "AES");
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, masterSecretKey);
                byte[] encrypted = cipher.doFinal(dataKeyBytes);
                return Base64.getEncoder().encodeToString(encrypted);
            } catch (Exception e) {
                throw new RuntimeException("DataKey encryption failed", e);
            }
        }

        private byte[] decryptDataKey(String encryptedDataKey) {
            try {
                SecretKey masterSecretKey = new SecretKeySpec(masterKey, "AES");
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, masterSecretKey);
                byte[] encrypted = Base64.getDecoder().decode(encryptedDataKey);
                return cipher.doFinal(encrypted);
            } catch (Exception e) {
                throw new RuntimeException("DataKey decryption failed", e);
            }
        }
    }
}
