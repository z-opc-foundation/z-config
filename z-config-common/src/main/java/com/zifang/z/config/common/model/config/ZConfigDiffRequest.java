package com.zifang.z.config.common.model.config;

/**
 * 配置版本 Diff 对比请求
 * 对齐 Nacos 控制台的配置版本对比功能
 */
public class ZConfigDiffRequest {

    /** 配置ID */
    private String dataId;

    /** 配置分组 */
    private String group;

    /** 命名空间 */
    private String namespace;

    /** 第一个历史版本 ID */
    private Long historyId1;

    /** 第二个历史版本 ID */
    private Long historyId2;

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Long getHistoryId1() {
        return historyId1;
    }

    public void setHistoryId1(Long historyId1) {
        this.historyId1 = historyId1;
    }

    public Long getHistoryId2() {
        return historyId2;
    }

    public void setHistoryId2(Long historyId2) {
        this.historyId2 = historyId2;
    }
}
