package com.zifang.z.config.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 配置审批实体（对齐 Nacos 配置审批工作流）
 */
@BizModel(name = "配置审批实体（对齐 Nacos 配置审批工作流）", module = "z-config", description = "配置审批实体（对齐 Nacos 配置审批工作流）")
@TableName("z_config_approval")
public class ZConfigApproval implements Serializable {

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
    /** 操作类型：PUBLISH/DELETE/ROLLBACK */
    @BizField(name = "操作类型：PUBLISH/DELETE/ROLLBACK", type = BizField.FieldType.STRING)
    private String action;
    @BizField(name = "待审批的配置内容", type = BizField.FieldType.STRING)
    private String content;
    @BizField(name = "内容MD5", type = BizField.FieldType.STRING)
    private String md5;
    @BizField(name = "申请人", type = BizField.FieldType.STRING)
    private String applicant;
    @BizField(name = "申请人IP", type = BizField.FieldType.STRING)
    private String applicantIp;
    @BizField(name = "申请时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime applyTime;
    /** 审批状态：PENDING/APPROVED/REJECTED */
    @BizField(name = "审批状态：PENDING/APPROVED/REJECTED", type = BizField.FieldType.STRING)
    private String status;
    @BizField(name = "审批人", type = BizField.FieldType.STRING)
    private String approver;
    @BizField(name = "审批时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime approveTime;
    @BizField(name = "审批备注", type = BizField.FieldType.STRING)
    private String remark;

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
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getMd5() { return md5; }
    public void setMd5(String md5) { this.md5 = md5; }
    public String getApplicant() { return applicant; }
    public void setApplicant(String applicant) { this.applicant = applicant; }
    public String getApplicantIp() { return applicantIp; }
    public void setApplicantIp(String applicantIp) { this.applicantIp = applicantIp; }
    public LocalDateTime getApplyTime() { return applyTime; }
    public void setApplyTime(LocalDateTime applyTime) { this.applyTime = applyTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getApprover() { return approver; }
    public void setApprover(String approver) { this.approver = approver; }
    public LocalDateTime getApproveTime() { return approveTime; }
    public void setApproveTime(LocalDateTime approveTime) { this.approveTime = approveTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}



