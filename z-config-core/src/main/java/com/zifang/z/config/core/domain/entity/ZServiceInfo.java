package com.zifang.z.config.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;


/**
 * 无注释
 */
@TableName("z_service_info")
public class ZServiceInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 服务名（格式：group@@name）
     */
    private String serviceName;

    /**
     * 服务分组
     */
    @TableField("`group`")
    private String group;

    /**
     * 命名空间ID
     */
    @TableField("`namespace`")
    private String namespace;

    /**
     * 集群映射（JSON格式）
     */
    private String clusterMap;

    /**
     * 缓存毫秒数
     */
    private Integer cacheMillis;

    /**
     * 健康检查模式
     */
    private String healthCheckMode;

    /**
     * 健康检查超时时间
     */
    private Integer healthCheckTimeout;

    /**
     * IP删除超时时间
     */
    private Integer ipDeleteTimeout;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 修改时间
     */
    private Date gmtModified;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getClusterMap() {
        return clusterMap;
    }

    public void setClusterMap(String clusterMap) {
        this.clusterMap = clusterMap;
    }

    public Integer getCacheMillis() {
        return cacheMillis;
    }

    public void setCacheMillis(Integer cacheMillis) {
        this.cacheMillis = cacheMillis;
    }

    public String getHealthCheckMode() {
        return healthCheckMode;
    }

    public void setHealthCheckMode(String healthCheckMode) {
        this.healthCheckMode = healthCheckMode;
    }

    public Integer getHealthCheckTimeout() {
        return healthCheckTimeout;
    }

    public void setHealthCheckTimeout(Integer healthCheckTimeout) {
        this.healthCheckTimeout = healthCheckTimeout;
    }

    public Integer getIpDeleteTimeout() {
        return ipDeleteTimeout;
    }

    public void setIpDeleteTimeout(Integer ipDeleteTimeout) {
        this.ipDeleteTimeout = ipDeleteTimeout;
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }
}