package com.zifang.z.config.common.model;

import java.util.Objects;

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

    /**
     * 生成唯一字符串标识，用于 Map 的 key
     * 格式：namespace\0group\0dataId（使用 \0 分隔，避免与内容中的分隔符冲突）
     */
    public String toKey() {
        String ns = nameSpace != null ? nameSpace : "";
        String grp = group != null ? group : "";
        String did = dataId != null ? dataId : "";
        return ns + "\0" + grp + "\0" + did;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigKey configKey = (ConfigKey) o;
        return Objects.equals(nameSpace, configKey.nameSpace)
                && Objects.equals(group, configKey.group)
                && Objects.equals(dataId, configKey.dataId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nameSpace, group, dataId);
    }

    @Override
    public String toString() {
        return "ConfigKey{namespace='" + nameSpace + "', group='" + group + "', dataId='" + dataId + "'}";
    }
}
