package com.zifang.z.config.common.model.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ZConfigSaveRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 配置ID
     * 支持 camelCase (dataId) 和 snake_case (data_id) 两种 JSON 字段名
     */
    @JsonProperty("dataId")
    @JsonAlias("data_id")
    private String dataId;

    /**
     * 配置分组
     */
    private String group;

    /**
     * 配置内容
     */
    private String content;

    /**
     * 应用名
     */
    private String appName;

    /**
     * 命名空间（多租户隔离）
     */
    private String namespace;

    /**
     * 配置描述
     */
    private String configDesc;

    /**
     * 使用说明
     */
    private String configUsage;

    /**
     * 生效规则
     */
    private String configEnableRule;

    /**
     * 配置类型（如properties、yaml）
     */
    private String configType;

    /**
     * 配置JSON schema
     */
    private String configSchema;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getConfigDesc() {
        return configDesc;
    }

    public void setConfigDesc(String configDesc) {
        this.configDesc = configDesc;
    }

    public String getConfigUsage() {
        return configUsage;
    }

    public void setConfigUsage(String configUsage) {
        this.configUsage = configUsage;
    }

    public String getConfigEnableRule() {
        return configEnableRule;
    }

    public void setConfigEnableRule(String configEnableRule) {
        this.configEnableRule = configEnableRule;
    }

    public String getConfigType() {
        return configType;
    }

    public void setConfigType(String configType) {
        this.configType = configType;
    }

    public String getConfigSchema() {
        return configSchema;
    }

    public void setConfigSchema(String configSchema) {
        this.configSchema = configSchema;
    }
}
