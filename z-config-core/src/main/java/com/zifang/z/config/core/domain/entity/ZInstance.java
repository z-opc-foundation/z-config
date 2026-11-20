package com.zifang.z.config.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
@TableName("z_instance")
public class ZInstance implements Serializable {


    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;

    /**
     * 关联z_service_info.id
     */
    @BizField(name = "关联z_service_info.id", type = BizField.FieldType.INTEGER)
    private Long serviceId;

    /**
     * 实例唯一ID（格式：serviceId@@ip:port）
     */
    @BizField(name = "实例唯一ID（格式：serviceId@@ip:port）", type = BizField.FieldType.STRING)
    private String instanceId;

    /**
     * 实例IP
     */
    @BizField(name = "实例IP", type = BizField.FieldType.STRING)
    private String ip;

    /**
     * 实例端口
     */
    @BizField(name = "实例端口", type = BizField.FieldType.INTEGER)
    private Integer port;

    /**
     * 权重
     */
    @BizField(name = "权重", type = BizField.FieldType.DECIMAL)
    private Double weight;

    /**
     * 健康状态（1=健康，0=不健康）
     */
    @BizField(name = "健康状态（1=健康，0=不健康）", type = BizField.FieldType.BOOLEAN)
    private Boolean healthy;

    /**
     * 是否启用（1=启用，0=禁用）
     */
    @BizField(name = "是否启用（1=启用，0=禁用）", type = BizField.FieldType.BOOLEAN)
    private Boolean enabled;

    /**
     * 是否临时实例
     */
    @BizField(name = "是否临时实例", type = BizField.FieldType.BOOLEAN)
    private Boolean ephemeral;

    /**
     * 集群名
     */
    @BizField(name = "集群名", type = BizField.FieldType.STRING)
    private String clusterName;

    /**
     * 元数据（JSON格式）
     */
    @BizField(name = "元数据（JSON格式）", type = BizField.FieldType.STRING)
    private String metadata;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Boolean getHealthy() {
        return healthy;
    }

    public void setHealthy(Boolean healthy) {
        this.healthy = healthy;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getEphemeral() {
        return ephemeral;
    }

    public void setEphemeral(Boolean ephemeral) {
        this.ephemeral = ephemeral;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
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
}

