package com.zifang.z.config.common.model;

public class ConfigKey {

    private String nameSpace = ""; // 默认为空租户

    private String group;

    private String dataId;

    public static ConfigKey of(String nameSpace, String group, String dataId) {
        ConfigKey configKey = new ConfigKey();
        configKey.setDataId(dataId);
        configKey.setGroup(group);
        configKey.setNameSpace(nameSpace);
        return configKey;
    }

    public String getNameSpace() {
        return nameSpace;
    }

    public void setNameSpace(String nameSpace) {
        this.nameSpace = nameSpace;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }
}
