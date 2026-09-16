package com.zifang.z.config.starter.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Z-Config Spring Boot 配置属性。
 *
 * <pre>
 * z:
 *   config:
 *     enabled: true                       # 启用 ZConfigService + ZNamingService 自动装配
 *     server-addr: localhost:8848         # Z-Config server 地址
 *     namespace: default                  # namespace 隔离
 *     username:                           # 可选, 鉴权
 *     password:
 *     connect-timeout-ms: 5000
 *     long-poll-timeout-ms: 30000
 *     auto-refresh: true                  # 配置变更自动刷新 (ZConfigListenerManager)
 * </pre>
 */
@ConfigurationProperties(prefix = "z.config")
public class ZConfigProperties {

    /** 是否启用 Z-Config 自动装配. 默认 false. */
    private boolean enabled = false;

    /** Z-Config server 地址 (host:port). 必填. */
    private String serverAddr;

    /** Namespace 隔离. 必填. */
    private String namespace = "default";

    /** 鉴权用户名 (可选). */
    private String username;

    /** 鉴权密码 (可选). */
    private String password;

    /** 连接超时 (ms). */
    private long connectTimeoutMs = 5000;

    /** 长轮询超时 (ms) — server hold 住直到配置变更或超时. */
    private long longPollTimeoutMs = 30000;

    /** 是否自动监听配置变更 + 触发回调. */
    private boolean autoRefresh = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getServerAddr() { return serverAddr; }
    public void setServerAddr(String serverAddr) { this.serverAddr = serverAddr; }

    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public long getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(long connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

    public long getLongPollTimeoutMs() { return longPollTimeoutMs; }
    public void setLongPollTimeoutMs(long longPollTimeoutMs) { this.longPollTimeoutMs = longPollTimeoutMs; }

    public boolean isAutoRefresh() { return autoRefresh; }
    public void setAutoRefresh(boolean autoRefresh) { this.autoRefresh = autoRefresh; }
}
