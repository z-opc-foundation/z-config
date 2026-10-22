package com.zifang.z.config.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nacos 兼容面（{@code /nacos/v1/**}）的鉴权与暴露口径开关。
 *
 * <p><b>来历</b>（TASK-20261022-020 · T4/T5）：兼容面曾对公网<b>完全免鉴权</b> ——
 * 匿名可读中央配置清单与正文，且 {@code POST}/{@code DELETE} 写端点已注册
 * ⇒ 构成「配置注入 / 配置删除」原语。修掉三道放行门之后，剩下的产品姿态问题
 * （T1：保留兼容面并强制 accessToken / 直接下线 / 收进内网）由用户拍板，
 * 本类提供的是<b>两条路都要用到的开关</b>，默认值一律取「不改变现状」：
 * <ul>
 *   <li>{@code accessTokenEnabled=false} ⇒ 不启用 accessToken 校验；兼容面只认 SSO 登录态。</li>
 *   <li>{@code metadataEndpointsEnabled=true} ⇒ 两个只读元信息端点照旧暴露。</li>
 * </ul>
 *
 * <p><b>为什么开关存在而不是直接改代码</b>：T1 未拍板前，任何「把兼容面彻底关掉」或
 * 「给兼容面开 accessToken 口子」的动作都是替用户做产品决策。本类只提供**能力**，
 * 默认值全部落在安全的一侧，且启用 accessToken 时若 secret 为空会**启动失败**（fail fast），
 * 杜绝「以为开着、其实是空口令」这种配置事故。
 *
 * @see NacosAccessTokenVerifier
 * @see NacosAccessTokenAuthFilter
 */
@ConfigurationProperties(prefix = "nacos.compat")
public class NacosCompatProperties {

    /**
     * 是否启用 accessToken 校验（T1 选项①的开关）。
     * <p>默认 false：兼容面此时只认 SSO 登录态（{@code /nacos/**} 已在 {@code sso.intercept-paths} 内）。
     */
    private boolean accessTokenEnabled = false;

    /**
     * accessToken 共享密钥（真 Nacos 语义：客户端用 {@code accessToken} 查询参数携带）。
     * <p>启用 accessToken 校验时<b>必须</b>非空，否则启动失败。生产真值走 Secret/环境变量，
     * 不写进 application.properties。
     */
    private String accessTokenSecret = "";

    /**
     * accessToken 通过校验后，桥接出来的 SSO 会话有效期（秒）。
     * <p>只作用于**当次请求**（不写回响应 Cookie），取小值即可。
     */
    private int accessTokenTtlSeconds = 300;

    /**
     * 是否暴露两个只读元信息端点：
     * {@code GET /nacos/v1/console/namespaces}（命名空间与配置条数）、
     * {@code GET /nacos/v1/ns/operator/servers}（集群节点元信息）。
     * <p>二者只返回**元信息**（不返回配置正文），属信息泄露而非配置读取；
     * T1 判「不需要对外」时置 false 即下线。默认 true = 不改变现状。
     */
    private boolean metadataEndpointsEnabled = true;

    public boolean isAccessTokenEnabled() {
        return accessTokenEnabled;
    }

    public void setAccessTokenEnabled(boolean accessTokenEnabled) {
        this.accessTokenEnabled = accessTokenEnabled;
    }

    public String getAccessTokenSecret() {
        return accessTokenSecret;
    }

    public void setAccessTokenSecret(String accessTokenSecret) {
        this.accessTokenSecret = accessTokenSecret;
    }

    public int getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public void setAccessTokenTtlSeconds(int accessTokenTtlSeconds) {
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public boolean isMetadataEndpointsEnabled() {
        return metadataEndpointsEnabled;
    }

    public void setMetadataEndpointsEnabled(boolean metadataEndpointsEnabled) {
        this.metadataEndpointsEnabled = metadataEndpointsEnabled;
    }
}
