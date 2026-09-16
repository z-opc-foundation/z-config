package com.zifang.z.config.common.model.naming;


/**
 * 消费实例取消订阅服务请求参数DTO
 * 功能：接收消费实例取消订阅的入参，解除消费实例与被订阅服务的关联
 * 说明：移除了javax.validation和Swagger注解，仅保留核心字段和默认值
 *
 * @author 开发者
 * @date 2026-02-12
 */
public class ZNamingUnsubscribeRequest {
    /**
     * 消费实例IP地址（必填）
     * 示例：192.168.1.200
     */
    private String consumerIp;

    /**
     * 消费实例端口（必填，范围：1-65535）
     * 示例：8081
     */
    private Integer consumerPort;

    /**
     * 订阅的服务名称（必填）
     * 示例：service-order
     */
    private String subscribeServiceName;

    /**
     * 消费实例所属服务名（双角色实例必填，纯消费实例可选）
     * 示例：service-pay
     */
    private String consumerServiceName;

    /**
     * 消费实例所属分组
     * 默认值：DEFAULT_GROUP
     */
    private String consumerGroup = "DEFAULT_GROUP";

    /**
     * 消费实例所属命名空间
     * 默认值：空字符串
     */
    private String consumerNamespace = "";

    /**
     * 订阅的服务分组
     * 默认值：DEFAULT_GROUP
     */
    private String subscribeGroup = "DEFAULT_GROUP";

    /**
     * 订阅的服务命名空间
     * 默认值：空字符串
     */
    private String subscribeNamespace = "";

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

    public String getSubscribeServiceName() {
        return subscribeServiceName;
    }

    public void setSubscribeServiceName(String subscribeServiceName) {
        this.subscribeServiceName = subscribeServiceName;
    }

    public String getConsumerServiceName() {
        return consumerServiceName;
    }

    public void setConsumerServiceName(String consumerServiceName) {
        this.consumerServiceName = consumerServiceName;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public String getConsumerNamespace() {
        return consumerNamespace;
    }

    public void setConsumerNamespace(String consumerNamespace) {
        this.consumerNamespace = consumerNamespace;
    }

    public String getSubscribeGroup() {
        return subscribeGroup;
    }

    public void setSubscribeGroup(String subscribeGroup) {
        this.subscribeGroup = subscribeGroup;
    }

    public String getSubscribeNamespace() {
        return subscribeNamespace;
    }

    public void setSubscribeNamespace(String subscribeNamespace) {
        this.subscribeNamespace = subscribeNamespace;
    }
}