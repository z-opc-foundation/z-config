package com.zifang.z.config.core.service;

import com.zifang.z.config.common.model.ZNamingInstance;

import java.util.List;

public interface ZNamingService {

    // 基础注册（默认临时实例，权重 1.0）
    void registerInstance(String serviceName, String ip, int port);

    // 带元数据的注册（支持权重、集群、元数据）
    void registerInstance(String serviceName, String ip, int port, String clusterName);

    String registerInstance(ZNamingInstance instance);

    // 查询服务的所有实例（包括健康/不健康）
    List<ZNamingInstance> getAllInstances(String serviceName);

    List<ZNamingInstance> getAllInstances(String serviceName, String group, String namespace);

    // 查询健康的实例（默认只返回健康实例）
    List<ZNamingInstance> selectInstances(String serviceName, boolean healthy);

    // 按集群、健康状态查询实例
    List<ZNamingInstance> selectInstances(String serviceName, String clusterName, boolean healthy);

    // 随机选择一个健康实例（用于负载均衡）
    ZNamingInstance selectOneHealthyInstance(String serviceName);

    // 基础注销（按服务名、IP、端口）
    void deregisterInstance(String serviceName, String ip, int port);

    // 按集群注销
    void deregisterInstance(String serviceName, String ip, int port, String clusterName);

    // 按 Instance 对象注销
    void deregisterInstance(String serviceName, ZNamingInstance instance);

//
//    // 监听服务实例变化（回调函数接收变更通知）
//    void subscribe(String serviceName, EventListener listener) throws NacosException;
//
//    // 按集群监听
//    void subscribe(String serviceName, String clusterName, EventListener listener) throws NacosException;
//
//    // 取消监听
//    void unsubscribe(String serviceName, EventListener listener) throws NacosException;

    // ===================== 新增：消费实例订阅/取消订阅 =====================

    /**
     * 消费实例订阅服务
     *
     * @param consumerInstance     消费实例信息
     * @param subscribeServiceName 被订阅的服务名
     * @param subscribeGroup       被订阅服务的分组
     * @param subscribeNamespace   被订阅服务的命名空间
     * @return 操作结果（成功/失败）
     */
    String subscribeService(ZNamingInstance consumerInstance, String subscribeServiceName, String subscribeGroup, String subscribeNamespace);

    /**
     * 消费实例取消订阅服务
     *
     * @param consumerInstance     消费实例信息
     * @param subscribeServiceName 被订阅的服务名
     * @param subscribeGroup       被订阅服务的分组
     * @param subscribeNamespace   被订阅服务的命名空间
     */
    void unsubscribeService(ZNamingInstance consumerInstance, String subscribeServiceName, String subscribeGroup, String subscribeNamespace);


}
