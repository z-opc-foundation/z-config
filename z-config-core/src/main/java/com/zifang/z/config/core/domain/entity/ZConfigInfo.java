package com.zifang.z.config.core.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 无注释
 */
@BizModel(name = "无注释", module = "z-config", description = "无注释")
@TableName("z_config_info")
public class ZConfigInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;

    /**
     * 配置ID
     */
    @TableField(value = "data_id", insertStrategy = FieldStrategy.DEFAULT, updateStrategy = FieldStrategy.DEFAULT)
    @BizField(name = "配置ID", type = BizField.FieldType.STRING)
    private String dataId;

    /**
     * 配置分组
     */
    @TableField(value = "`group`")
    @BizField(name = "配置分组", type = BizField.FieldType.STRING)
    private String group;

    /**
     * 配置内容
     */
    @BizField(name = "配置内容", type = BizField.FieldType.STRING)
    private String content;

    /**
     * 内容MD5
     */
    @BizField(name = "内容MD5", type = BizField.FieldType.STRING)
    private String md5;

    /**
     * 创建时间
     */
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtCreate;

    /**
     * 修改时间
     */
    @BizField(name = "修改时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtModified;

    /**
     * 创建人工号
     */
    @BizField(name = "创建人工号", type = BizField.FieldType.STRING)
    private String creatorStaffNo;

    /**
     * 创建人昵称
     */
    @BizField(name = "创建人昵称", type = BizField.FieldType.STRING)
    private String creatorStaffNickNm;

    /**
     * 创建人真名
     */
    @BizField(name = "创建人真名", type = BizField.FieldType.STRING)
    private String creatorStaffRealNm;

    /**
     * 创建IP
     */
    @BizField(name = "创建IP", type = BizField.FieldType.STRING)
    private String sourceIp;

    /**
     * 应用名
     */
    @BizField(name = "应用名", type = BizField.FieldType.STRING)
    private String appName;

    /**
     * 命名空间（多租户隔离）
     */
    @BizField(name = "命名空间（多租户隔离）", type = BizField.FieldType.STRING)
    private String namespace;

    /**
     * 配置描述
     */
    @BizField(name = "配置描述", type = BizField.FieldType.STRING)
    private String configDesc;

    /**
     * 使用说明
     */
    @BizField(name = "使用说明", type = BizField.FieldType.STRING)
    private String configUsage;

    /**
     * 生效规则
     */
    @BizField(name = "生效规则", type = BizField.FieldType.STRING)
    private String configEnableRule;

    /**
     * 配置类型（如properties、yaml）
     */
    @BizField(name = "配置类型（如properties、yaml）", type = BizField.FieldType.STRING)
    private String configType;

    /**
     * 配置JSON schema
     */
    @BizField(name = "配置JSON schema", type = BizField.FieldType.STRING)
    private String configSchema;

    /**
     * 加密数据密钥（RSA 加密后的 AES 密钥，用于配置内容加密存储）
     */
    @BizField(name = "加密数据密钥（RSA 加密后的 AES 密钥，用于配置内容加密存储）", type = BizField.FieldType.STRING)
    private String encryptedDataKey;

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

    public String getCreatorStaffNickNm() {
        return creatorStaffNickNm;
    }

    public void setCreatorStaffNickNm(String creatorStaffNickNm) {
        this.creatorStaffNickNm = creatorStaffNickNm;
    }

    public String getCreatorStaffRealNm() {
        return creatorStaffRealNm;
    }

    public void setCreatorStaffRealNm(String creatorStaffRealNm) {
        this.creatorStaffRealNm = creatorStaffRealNm;
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

    public String getEncryptedDataKey() {
        return encryptedDataKey;
    }

    public void setEncryptedDataKey(String encryptedDataKey) {
        this.encryptedDataKey = encryptedDataKey;
    }
}

