package com.zifang.z.config.web.config;

import com.zifang.ctc.sso.JwtUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Nacos 兼容面的鉴权装配（T1 选项①的**预备件**，默认**不注册过滤器**）。
 *
 * <p>装配两件事：
 * <ol>
 *   <li>{@link NacosCompatProperties} —— 兼容面的鉴权/暴露口径开关（T1 拍板后生效）。</li>
 *   <li>{@link NacosAccessTokenAuthFilter} —— <b>仅当</b>
 *       {@code nacos.compat.access-token.enabled=true} 时注册，且注册在 Servlet 过滤器链上
 *       ⇒ 早于 z-ctc 的 SSO 拦截器执行（见 {@link NacosAccessTokenAuthFilter} 的类注释）。</li>
 * </ol>
 *
 * <p><b>fail fast</b>：启用了 accessToken 校验却没配 secret ⇒ 启动直接失败。
 * 宁可起不来，也不要出现「以为开着鉴权、其实是空口令」的配置事故。
 *
 * @see NacosCompatProperties
 * @see NacosAccessTokenAuthFilter
 */
@Configuration
@EnableConfigurationProperties(NacosCompatProperties.class)
public class NacosCompatAuthConfig {

    /**
     * accessToken 闸门过滤器。
     *
     * <p>JwtUtil 用 {@code @Lazy} 注入：它由 z-ctc-sso 的自动装配提供（JWT 密钥与 SSO 验签同源），
     * 本仓不能自己再造一把密钥 —— 两把密钥不一致 ⇒ 桥接出来的 token 会被闸门判无效。
     */
    @Bean
    @Lazy
    @ConditionalOnProperty(prefix = "nacos.compat.access-token", name = "enabled", havingValue = "true")
    public FilterRegistrationBean<NacosAccessTokenAuthFilter> nacosAccessTokenAuthFilter(
            NacosAccessTokenVerifier verifier,
            NacosCompatProperties properties,
            JwtUtil jwtUtil) {

        String secret = properties.getAccessTokenSecret();
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException(
                    "nacos.compat.access-token.enabled=true 但 nacos.compat.access-token.secret 为空 —— "
                            + "启用 accessToken 校验必须配共享密钥（生产走 Secret/环境变量），否则拒绝启动");
        }

        FilterRegistrationBean<NacosAccessTokenAuthFilter> registration =
                new FilterRegistrationBean<NacosAccessTokenAuthFilter>(
                        new NacosAccessTokenAuthFilter(verifier, jwtUtil, properties.getAccessTokenTtlSeconds()));
        registration.addUrlPatterns("/nacos/*");
        registration.setOrder(OrderedFilter.HIGHEST_PRECEDENCE + 10);
        registration.setName("nacosAccessTokenAuthFilter");
        return registration;
    }

    /** 过滤器顺序常量（不引 Spring 的 Ordered 接口，直接用数值更直白）。 */
    private static final class OrderedFilter {
        static final int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

        private OrderedFilter() {
        }
    }
}
