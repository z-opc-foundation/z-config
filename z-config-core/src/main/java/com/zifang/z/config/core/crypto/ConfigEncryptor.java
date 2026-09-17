package com.zifang.z.config.core.crypto;

/**
 * 配置内容加密器接口（对齐 Nacos 的 EncryptionPluginService SPI 机制）
 * <p>
 * 当配置的 dataId 以 "cipher-" 前缀开头时，服务端自动对内容进行加密存储。
 * 查询时自动解密返回明文。
 * <p>
 * 默认实现：AES/GCM/NoPadding
 * 扩展方式：实现此接口并通过 Spring Bean 注册即可自动替换
 */
public interface ConfigEncryptor {

    /**
     * 加密配置内容
     *
     * @param plainContent 明文内容
     * @return 密文内容（Base64 编码）
     */
    String encrypt(String plainContent);

    /**
     * 解密配置内容
     *
     * @param cipherContent   密文内容（Base64 编码）
     * @param encryptedDataKey 加密时生成的数据密钥
     * @return 明文内容
     */
    String decrypt(String cipherContent, String encryptedDataKey);

    /**
     * 是否需要加密（检查 dataId 是否符合加密规则）
     * 默认规则：dataId 以 "cipher-" 前缀开头
     *
     * @param dataId 配置标识
     * @return true 表示需要加密
     */
    default boolean needEncrypt(String dataId) {
        return dataId != null && dataId.startsWith("cipher-");
    }
}
