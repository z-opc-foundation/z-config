package com.zifang.z.config.web.config;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Nacos 兼容面的 accessToken 校验器（T1 选项①的**预备件**，默认不启用）。
 *
 * <p><b>语义</b>：真 Nacos 的 accessToken 走<b>查询参数</b>（{@code ?accessToken=xxx}），
 * 不走 Cookie、不走 Authorization 头。本类按同一语义校验：
 * <ul>
 *   <li>未启用（{@code nacos.compat.access-token.enabled=false}，即当前默认值）
 *       ⇒ {@link #isValid(String)} 一律返回 true —— <b>不施加任何要求</b>，
 *       兼容面的保护完全交给 SSO 闸门（{@code sso.intercept-paths} 含 {@code /nacos/**}）。</li>
 *   <li>已启用 ⇒ 密钥必须非空（否则启动阶段就 fail fast，见 {@link NacosCompatAuthConfig}），
 *       且比较用<b>定长常量时间</b>（{@link MessageDigest#isEqual}），避免逐字符比较的时序侧信道。</li>
 * </ul>
 *
 * @see NacosCompatProperties
 * @see NacosAccessTokenAuthFilter
 */
@Component
public class NacosAccessTokenVerifier {

    private final NacosCompatProperties properties;

    public NacosAccessTokenVerifier(NacosCompatProperties properties) {
        this.properties = properties;
    }

    /** 当前是否启用了 accessToken 校验。 */
    public boolean isEnabled() {
        return properties.isAccessTokenEnabled();
    }

    /**
     * 校验客户端带来的 accessToken。
     *
     * @param presented 客户端 {@code accessToken} 查询参数（可为 null）
     * @return true = 放行；false = 拒绝
     */
    public boolean isValid(String presented) {
        if (!properties.isAccessTokenEnabled()) {
            // 未启用 ⇒ 不施加要求（保护由 SSO 闸门承担）
            return true;
        }
        String expected = properties.getAccessTokenSecret();
        if (expected == null || expected.trim().isEmpty()) {
            // 启用却没配密钥 ⇒ 一律拒绝（fail closed，绝不「空口令即通过」）
            return false;
        }
        if (presented == null || presented.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                presented.getBytes(StandardCharsets.UTF_8));
    }
}
