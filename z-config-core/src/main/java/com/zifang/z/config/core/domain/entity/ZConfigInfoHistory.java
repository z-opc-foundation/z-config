package com.zifang.z.config.core.domain.entity;

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
@TableName("z_config_info_history")
public class ZConfigInfoHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;

    /**
     * 配置历史ID
     */
    @BizField(name = "配置历史ID", type = BizField.FieldType.INTEGER)
    private Long nid;

    /**
     * 配置ID
     */
    @BizField(name = "配置ID", type = BizField.FieldType.STRING)
    private String dataId;

    /**
     * 配置分组
     */
    @TableField("`group`")
    @BizField(name = "配置分组", type = BizField.FieldType.STRING)
    private String group;

    /**
     * 应用名
     */
    @BizField(name = "应用名", type = BizField.FieldType.STRING)
    private String appName;

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
     * 操作人
     */
    @BizField(name = "操作人", type = BizField.FieldType.STRING)
    private String srcUser;

    /**
     * 操作IP
     */
    @BizField(name = "操作IP", type = BizField.FieldType.STRING)
    private String srcIp;

    /**
     * 操作类型（新增/修改/删除）
     */
    @BizField(name = "操作类型（新增/修改/删除）", type = BizField.FieldType.STRING)
    private String opType;

    /**
     * 命名空间
     */
    @BizField(name = "命名空间", type = BizField.FieldType.STRING)
    private String namespace;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNid() {
        return nid;
    }

    public void setNid(Long nid) {
        this.nid = nid;
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

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
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

    public String getSrcUser() {
        return srcUser;
    }

    public void setSrcUser(String srcUser) {
        this.srcUser = srcUser;
    }

    public String getSrcIp() {
        return srcIp;
    }

    public void setSrcIp(String srcIp) {
        this.srcIp = srcIp;
    }

    public String getOpType() {
        return opType;
    }

    public void setOpType(String opType) {
        this.opType = opType;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}

