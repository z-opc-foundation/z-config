package com.zifang.z.config.common.model;


import java.time.LocalDateTime;


public class ZConfigDTO {
    /**
     * 主键
     */
    private Long id;

    /**
     * 配置ID
     */
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
     * 内容MD5
     */
    private String md5;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间
     */
    private LocalDateTime gmtModified;

    /**
     * 创建人工号
     */
    private String creatorStaffNo;

    /**
     * 创建人昵称
     */
    private String creatorStaffNickMn;

    /**
     * 创建人真名
     */
    private String creatorStaffRealMn;

    /**
     * 创建IP
     */
    private String sourceIp;

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

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public LocalDateTime getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(LocalDateTime gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public LocalDateTime getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(LocalDateTime gmtModified) {
        this.gmtModified = gmtModified;
    }

    public String getCreatorStaffNo() {
        return creatorStaffNo;
    }

    public void setCreatorStaffNo(String creatorStaffNo) {
        this.creatorStaffNo = creatorStaffNo;
    }

    public String getCreatorStaffNickMn() {
        return creatorStaffNickMn;
    }

    public void setCreatorStaffNickMn(String creatorStaffNickMn) {
        this.creatorStaffNickMn = creatorStaffNickMn;
    }

    public String getCreatorStaffRealMn() {
        return creatorStaffRealMn;
    }

    public void setCreatorStaffRealMn(String creatorStaffRealMn) {
        this.creatorStaffRealMn = creatorStaffRealMn;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
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
