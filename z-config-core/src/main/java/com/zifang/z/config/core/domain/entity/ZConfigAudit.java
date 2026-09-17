package com.zifang.z.config.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 配置审计日志实体（对齐 Nacos 审计系统）
 * 记录所有配置变更操作的完整审计轨迹
 */
@TableName("z_config_audit")
public class ZConfigAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String dataId;
    private String groupName;
    private String namespace;
    /** 操作类型：CREATE/UPDATE/DELETE/ROLLBACK/IMPORT/EXPORT/CLONE */
    private String action;
    private String oldContent;
    private String newContent;
    private String oldMd5;
    private String newMd5;
    private String srcUser;
    private String srcIp;
    /** 操作结果：SUCCESS/FAILED */
    private String result;
    private String errorMsg;
    private LocalDateTime gmtCreate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDataId() { return dataId; }
    public void setDataId(String dataId) { this.dataId = dataId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getOldContent() { return oldContent; }
    public void setOldContent(String oldContent) { this.oldContent = oldContent; }
    public String getNewContent() { return newContent; }
    public void setNewContent(String newContent) { this.newContent = newContent; }
    public String getOldMd5() { return oldMd5; }
    public void setOldMd5(String oldMd5) { this.oldMd5 = oldMd5; }
    public String getNewMd5() { return newMd5; }
    public void setNewMd5(String newMd5) { this.newMd5 = newMd5; }
    public String getSrcUser() { return srcUser; }
    public void setSrcUser(String srcUser) { this.srcUser = srcUser; }
    public String getSrcIp() { return srcIp; }
    public void setSrcIp(String srcIp) { this.srcIp = srcIp; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public LocalDateTime getGmtCreate() { return gmtCreate; }
    public void setGmtCreate(LocalDateTime gmtCreate) { this.gmtCreate = gmtCreate; }
}
