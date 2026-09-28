package com.zifang.z.config.core.crypto;

import com.zifang.util.core.encrypt.AesUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * AES/GCM/NoPadding 加密器实现
 * <p>
 * 对齐 Nacos 的配置加密机制：
 * - dataId 以 "cipher-" 前缀开头时触发加密
 * - 每次加密生成随机 AES 密钥（DataKey）
 * - DataKey 使用固定密钥加密后存储在 encrypted_data_key 字段
 * - 加密内容格式：Base64(iv + ciphertext + tag)
 * <p>
 * 2026-09-28 收编：内容层的 AES/GCM/NoPadding（12 字节随机 IV、128bit 认证标签、
 * 密文布局 {@code [IV 12B][ciphertext][tag 16B]}、标准 Base64、UTF-8 取字节）与 DataKey 生成
 * 全部委托 {@code com.zifang.util.core.encrypt.AesUtil}（z-util 1.0.13）——
 * 委托前后**逐字节同格式**，历史落库密文照旧可解，对拍尺见
 * {@code src/test/java/com/zifang/z/config/core/crypto/AESEncryptorCompatCrossCheck}。
 * <p>
 * 未收编部分（z-util 1.0.13 无对等能力，按"宁可保留不许顺手统一"原则原样留下）：
 * DataKey 的封装用的是 <b>AES/ECB/PKCS5Padding + 裸 masterKey 字节</b>，
 * 而 {@code AesUtil.encrypt(byte[], String password)} 的密钥是 SHA1PRNG 从口令派生的，
 * 密钥来源不同、换过去就读不出历史 {@code encrypted_data_key} 字段，故保留本类内的实现。
 * <p>
 * 既有事实（非本次改动引入）：{@code masterKey} 每次构造随机生成，不持久化，
 * 所以跨进程/重启解不开 {@code encrypted_data_key}（解密失败时
 * {@code ConfigServiceImpl.getConfigInner} 兜底返回密文）。本类只保证格式不变。
 */
public class AESEncryptor implements ConfigEncryptor {

    private static final Logger log = LogManager.getLogger(AESEncryptor.class);

    /**
     * DataKey / MasterKey 位数（AES-256，与收编前一致）
     */
    private static final int KEY_BITS = 256;

    /**
     * DataKey 封装用的变换：z-util 1.0.13 没有"裸密钥 + ECB"入口，保留本地实现
     */
    private static final String DATA_KEY_TRANSFORMATION = "AES/ECB/PKCS5Padding";

    /**
     * 固定密钥，用于加密 DataKey（生产环境应从 KMS 获取）
     * 此密钥在应用启动时生成，同一次启动内一致
     */
    private final byte[] masterKey;

    public AESEncryptor() {
        // 生成 256 位主密钥（每次应用启动时生成新的）
        // 注意：生产环境应持久化或从外部 KMS 获取
        this.masterKey = AesUtil.generateKey(KEY_BITS);
    }

    @Override
    public String encrypt(String plainContent) {
        if (plainContent == null || plainContent.isEmpty()) {
            return plainContent;
        }
        try {
            // 1. 生成随机 AES 密钥（DataKey）
            byte[] dataKey = AesUtil.generateKey(KEY_BITS);

            // 2~4. AES/GCM/NoPadding + 随机 12 字节 IV，输出 Base64([IV 12B][ciphertext][tag 16B])
            String encryptedContent = AesUtil.encryptGcmToBase64(plainContent, dataKey);

            // 5. 加密 DataKey（使用 MasterKey）
            String encryptedDataKey = encryptDataKey(dataKey);

            log.debug("配置内容加密成功，明文长度={}, 密文长度={}", plainContent.length(), encryptedContent.length());

            // 返回格式："encryptedDataKey:encryptedContent"
            return encryptedDataKey + ":" + encryptedContent;
        } catch (Exception e) {
            log.error("配置内容加密失败", e);
            throw new RuntimeException("Config encryption failed", e);
        }
    }

    @Override
    public String decrypt(String cipherContent, String encryptedDataKey) {
        if (cipherContent == null || cipherContent.isEmpty()) {
            return cipherContent;
        }
        try {
            // 解密 DataKey
            byte[] dataKey = decryptDataKey(encryptedDataKey);

            // Base64([IV 12B][ciphertext][tag 16B]) → 明文（UTF-8）
            return AesUtil.decryptGcmFromBase64(cipherContent, dataKey);
        } catch (Exception e) {
            log.error("配置内容解密失败", e);
            throw new RuntimeException("Config decryption failed", e);
        }
    }

    /**
     * 使用 MasterKey 加密 DataKey
     */
    private String encryptDataKey(byte[] dataKeyBytes) {
        try {
            SecretKey masterSecretKey = new SecretKeySpec(masterKey, "AES");
            Cipher cipher = Cipher.getInstance(DATA_KEY_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, masterSecretKey);
            byte[] encrypted = cipher.doFinal(dataKeyBytes);
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("DataKey encryption failed", e);
        }
    }

    /**
     * 使用 MasterKey 解密 DataKey
     */
    private byte[] decryptDataKey(String encryptedDataKey) {
        try {
            SecretKey masterSecretKey = new SecretKeySpec(masterKey, "AES");
            Cipher cipher = Cipher.getInstance(DATA_KEY_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, masterSecretKey);
            byte[] encrypted = Base64.getDecoder().decode(encryptedDataKey);
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("DataKey decryption failed", e);
        }
    }
}
