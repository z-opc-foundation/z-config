package com.zifang.z.config.common.model.naming;


/**
 * 注销服务实例请求参数DTO
 * 功能：接收服务注销接口的入参，定位需要注销的具体实例
 * 说明：移除了javax.validation和Swagger注解，仅保留核心字段和默认值
 *
 * @author 开发者
 * @date 2026-02-12
 */
public class ZNamingInstanceDeregisterRequest {
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
}