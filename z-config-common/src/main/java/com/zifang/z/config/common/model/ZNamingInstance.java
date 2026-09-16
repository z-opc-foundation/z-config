package com.zifang.z.config.common.model;

import java.util.Map;

public class ZNamingInstance {

    /**
     * 服务名（纯名称，如order-service，非group@@name）
     */
    private String serviceName;

    /**
     * 实例唯一ID（格式：serviceId@@ip:port）
     */
    private String instanceId;

    /**
     * 服务分组
     */
    private String group = "DEFAULT_GROUP";

    /**
     * 命名空间ID（替换namespaceId为nameSpace）
     */
    private String namespace = "public";

    /**
     * 集群名
     */
    private String clusterName = "DEFAULT";

    /**
     * 实例IP
     */
    private String ip;

    /**
     * 实例端口
     */
    private Integer port;

    /**
     * 权重
     */
    private Double weight = 1.0;

    /**
     * 健康状态（1=健康，0=不健康）
     */
    private Boolean healthy = true;

    /**
     * 是否启用
     */
    private Boolean enabled = true;

    /**
     * 是否临时实例
     */
    private Boolean ephemeral = true;

    /**
     * 元数据
     */
    private Map<String, String> metadata;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
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

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
