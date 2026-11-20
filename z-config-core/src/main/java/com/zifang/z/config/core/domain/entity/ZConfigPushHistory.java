package com.zifang.z.config.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 配置推送通知历史实体（对齐 Nacos 推送轨迹）
 */
@BizModel(name = "配置推送通知历史实体（对齐 Nacos 推送轨迹）", module = "z-config", description = "配置推送通知历史实体（对齐 Nacos 推送轨迹）")
@TableName("z_config_push_history")
public class ZConfigPushHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "配置ID", type = BizField.FieldType.STRING)
    private String dataId;
    @BizField(name = "配置分组", type = BizField.FieldType.STRING)
    private String groupName;
    @BizField(name = "命名空间", type = BizField.FieldType.STRING)
    private String namespace;
    @BizField(name = "客户端IP", type = BizField.FieldType.STRING)
    private String clientIp;
    /** 推送类型：CONFIG_CHANGE/HEARTBEAT_TIMEOUT */
    @BizField(name = "推送类型：CONFIG_CHANGE/HEARTBEAT_TIMEOUT", type = BizField.FieldType.STRING)
    private String pushType;
    /** 推送结果：SUCCESS/FAILED/TIMEOUT */
    @BizField(name = "推送结果：SUCCESS/FAILED/TIMEOUT", type = BizField.FieldType.STRING)
    private String pushResult;
    @BizField(name = "推送的配置MD5", type = BizField.FieldType.STRING)
    private String newMd5;
    @BizField(name = "推送时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime pushTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDataId() { return dataId; }
    public void setDataId(String dataId) { this.dataId = dataId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getPushType() { return pushType; }
    public void setPushType(String pushType) { this.pushType = pushType; }
    public String getPushResult() { return pushResult; }
    public void setPushResult(String pushResult) { this.pushResult = pushResult; }
    public String getNewMd5() { return newMd5; }
    public void setNewMd5(String newMd5) { this.newMd5 = newMd5; }
    public LocalDateTime getPushTime() { return pushTime; }
    public void setPushTime(LocalDateTime pushTime) { this.pushTime = pushTime; }
}



