package com.zifang.z.config.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 国际化消息实体（对齐 Nacos 的多语言支持）
 */
@BizModel(name = "国际化消息实体（对齐 Nacos 的多语言支持）", module = "z-config", description = "国际化消息实体（对齐 Nacos 的多语言支持）")
@TableName("z_i18n_message")
public class ZE18nMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "消息键", type = BizField.FieldType.STRING)
    private String messageKey;
    @BizField(name = "语言代码", type = BizField.FieldType.STRING)
    private String language;
    @BizField(name = "命名空间", type = BizField.FieldType.STRING)
    private String namespace;
    @BizField(name = "消息内容", type = BizField.FieldType.STRING)
    private String messageValue;
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtCreate;
    @BizField(name = "修改时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtModified;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMessageKey() { return messageKey; }
    public void setMessageKey(String messageKey) { this.messageKey = messageKey; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public String getMessageValue() { return messageValue; }
    public void setMessageValue(String messageValue) { this.messageValue = messageValue; }
    public LocalDateTime getGmtCreate() { return gmtCreate; }
    public void setGmtCreate(LocalDateTime gmtCreate) { this.gmtCreate = gmtCreate; }
    public LocalDateTime getGmtModified() { return gmtModified; }
    public void setGmtModified(LocalDateTime gmtModified) { this.gmtModified = gmtModified; }
}



