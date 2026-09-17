package com.zifang.z.config.core.crypto;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES/GCM/NoPadding 加密器实现
 * <p>
 * 对齐 Nacos 的配置加密机制：
 * - dataId 以 "cipher-" 前缀开头时触发加密
 * - 每次加密生成随机 AES 密钥（DataKey）
 * - DataKey 使用固定密钥加密后存储在 encrypted_data_key 字段
 * - 加密内容格式：Base64(iv + ciphertext + tag)
 */
public class AESEncryptor implements ConfigEncryptor {

    private static final Logger log = LogManager.getLogger(AESEncryptor.class);

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * 固定密钥，用于加密 DataKey（生产环境应从 KMS 获取）
     * 此密钥在应用启动时生成，同一次启动内一致
     */
    private final byte[] masterKey;

    private final SecureRandom secureRandom = new SecureRandom();

    public AESEncryptor() {
        // 生成 256 位主密钥（每次应用启动时生成新的）
        // 注意：生产环境应持久化或从外部 KMS 获取
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
            // 1. 生成随机 AES 密钥（DataKey）
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256, secureRandom);
            SecretKey dataKey = keyGen.generateKey();

            // 2. 生成随机 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // 3. 使用 DataKey + IV 加密内容
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, dataKey, gcmSpec);
            byte[] ciphertext = cipher.doFinal(plainContent.getBytes(StandardCharsets.UTF_8));

            // 4. 组装结果：iv(12) + ciphertext + tag
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);
            String encryptedContent = Base64.getEncoder().encodeToString(byteBuffer.array());

            // 5. 加密 DataKey（使用 MasterKey）
            String encryptedDataKey = encryptDataKey(dataKey.getEncoded());

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
            byte[] dataKeyBytes = decryptDataKey(encryptedDataKey);
            SecretKey dataKey = new SecretKeySpec(dataKeyBytes, "AES");

            // Base64 解码
            byte[] decoded = Base64.getDecoder().decode(cipherContent);

            // 提取 IV 和 ciphertext
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] ciphertextAndTag = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertextAndTag);

            // 解密
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, dataKey, gcmSpec);
            byte[] plainBytes = cipher.doFinal(ciphertextAndTag);

            return new String(plainBytes, StandardCharsets.UTF_8);
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
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
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
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, masterSecretKey);
            byte[] encrypted = Base64.getDecoder().decode(encryptedDataKey);
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("DataKey decryption failed", e);
        }
    }
}
