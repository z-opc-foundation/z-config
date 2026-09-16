package com.zifang.z.config.common.model.naming;

import java.util.Map;

/**
 * 注册服务实例请求参数DTO
 * 功能：接收服务注册接口的入参，封装服务名、IP、端口等核心信息
 */
public class ZNamingInstanceRegisterRequest {
    /**
     * 服务名称（必填）
     * 示例：service-order
     */
    private String serviceName;

    /**
     * 实例IP地址（必填）
     * 示例：192.168.1.100
     */
    private String ip;

    /**
     * 实例端口（必填，范围：1-65535）
     * 示例：8080
     */
    private Integer port;

    /**
     * 服务分组
     * 默认值：DEFAULT_GROUP
     */
    private String group = "DEFAULT_GROUP";

    /**
     * 命名空间
     * 默认值：空字符串
     */
    private String namespace = "";

    /**
     * 集群名称
     * 默认值：DEFAULT
     */
    private String clusterName = "DEFAULT";

    /**
     * 实例权重（负载均衡使用）
     * 默认值：1.0
     */
    private Double weight = 1.0;

    /**
     * 健康状态
     * 默认值：true（健康）
     */
    private Boolean healthy = true;

    /**
     * 是否启用实例
     * 默认值：true（启用）
     */
    private Boolean enabled = true;

    /**
     * 是否为临时实例（临时实例心跳超时会被自动注销）
     * 默认值：true（临时）
     */
    private Boolean ephemeral = true;

    /**
     * 实例元数据（自定义扩展信息）
     * 格式：JSON键值对，示例：{"version":"1.0","env":"prod"}
     */
    private Map<String, String> metadata;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
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

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
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

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}