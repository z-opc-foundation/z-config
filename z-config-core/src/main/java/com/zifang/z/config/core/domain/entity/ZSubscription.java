package com.zifang.z.config.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 无注释
 */
@TableName("z_subscription")
public class ZSubscription implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消费实例ID（同instance_id格式）
     */
    private String consumerInstanceId;

    /**
     * 消费实例IP
     */
    private String consumerIp;

    /**
     * 消费实例端口
     */
    private Integer consumerPort;

    /**
     * 订阅的服务ID（关联z_service_info.id）
     */
    private Long subscribeServiceId;

    /**
     * 订阅服务的命名空间
     */
    private String subscribeNamespace;

    /**
     * 订阅的集群名
     */
    private String subscribeCluster;

    /**
     * 订阅时间
     */
    private LocalDateTime subscribeTime;

    /**
     * 取消订阅时间
     */
    private LocalDateTime unsubscribeTime;

    /**
     * 订阅状态（1=有效，0=取消）
     */
    private Boolean status;

    /**
     * 消费端自定义元数据（JSON格式）
     */
    private String metadata;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间
     */
    private LocalDateTime gmtModified;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConsumerInstanceId() {
        return consumerInstanceId;
    }

    public void setConsumerInstanceId(String consumerInstanceId) {
        this.consumerInstanceId = consumerInstanceId;
    }

    public String getConsumerIp() {
        return consumerIp;
    }

    public void setConsumerIp(String consumerIp) {
        this.consumerIp = consumerIp;
    }

    public Integer getConsumerPort() {
        return consumerPort;
    }

    public void setConsumerPort(Integer consumerPort) {
        this.consumerPort = consumerPort;
    }

    public Long getSubscribeServiceId() {
        return subscribeServiceId;
    }

    public void setSubscribeServiceId(Long subscribeServiceId) {
        this.subscribeServiceId = subscribeServiceId;
    }

    public String getSubscribeNamespace() {
        return subscribeNamespace;
    }

    public void setSubscribeNamespace(String subscribeNamespace) {
        this.subscribeNamespace = subscribeNamespace;
    }

    public String getSubscribeCluster() {
        return subscribeCluster;
    }

    public void setSubscribeCluster(String subscribeCluster) {
        this.subscribeCluster = subscribeCluster;
    }

    public LocalDateTime getSubscribeTime() {
        return subscribeTime;
    }

    public void setSubscribeTime(LocalDateTime subscribeTime) {
        this.subscribeTime = subscribeTime;
    }

    public LocalDateTime getUnsubscribeTime() {
        return unsubscribeTime;
    }

    public void setUnsubscribeTime(LocalDateTime unsubscribeTime) {
        this.unsubscribeTime = unsubscribeTime;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
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