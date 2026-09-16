package com.zifang.z.config.common.model;


public class PollRequest {
    private ConfigKey configKey;
    private String clientMd5; // 客户端当前配置的MD5
    private long timeout = 30000; // 长轮询超时时间（默认30s）

    public ConfigKey getConfigKey() {
        return configKey;
    }

    public void setConfigKey(ConfigKey configKey) {
        this.configKey = configKey;
    }

    public String getClientMd5() {
        return clientMd5;
    }

    public void setClientMd5(String clientMd5) {
        this.clientMd5 = clientMd5;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
