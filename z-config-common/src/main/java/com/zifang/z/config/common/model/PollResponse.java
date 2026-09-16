package com.zifang.z.config.common.model;


public class PollResponse {
    private ConfigKey configKey;
    private boolean changed; // 是否变更
    private String newMd5; // 新MD5
    private String newConfig; // 变更后的配置（可选，也可后续单独拉取）

    public ConfigKey getConfigKey() {
        return configKey;
    }

    public void setConfigKey(ConfigKey configKey) {
        this.configKey = configKey;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public String getNewMd5() {
        return newMd5;
    }

    public void setNewMd5(String newMd5) {
        this.newMd5 = newMd5;
    }

    public String getNewConfig() {
        return newConfig;
    }

    public void setNewConfig(String newConfig) {
        this.newConfig = newConfig;
    }
}
