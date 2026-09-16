package com.zifang.z.config.client.naming;

import com.zifang.z.config.client.naming.listener.ZNamingListener;
import com.zifang.z.config.common.model.ZNamingInstance;

import java.util.List;

/**
 * Naming服务接口
 * 提供服务注册、发现、监听功能
 */
public interface ZNamingService {

    // ===================== 服务注册/注销 =====================

    /**
     * 注册服务实例（简化版本）
     */
    void registerInstance(String serviceName, String ip, int port);

    /**
     * 注册服务实例（带集群名）
     */
    void registerInstance(String serviceName, String ip, int port, String clusterName);

    /**
     * 注销服务实例（简化版本）
     */
    void deregisterInstance(String serviceName, String ip, int port);

    /**
     * 注销服务实例（带集群名）
     */
    void deregisterInstance(String serviceName, String ip, int port, String clusterName);

    // ===================== 服务查询 =====================

    /**
     * 获取服务所有实例
     */
    List<ZNamingInstance> getAllInstances(String serviceName);

    /**
     * 查询健康实例
     */
    List<ZNamingInstance> selectInstances(String serviceName, boolean healthy);

    /**
     * 按集群查询实例
     */
    List<ZNamingInstance> selectInstances(String serviceName, String clusterName, boolean healthy);

    /**
     * 选择一个健康实例（负载均衡）
     */
    ZNamingInstance selectOneHealthyInstance(String serviceName);

    // ===================== 服务监听 =====================

    /**
     * 添加服务监听器
     *
     * @param serviceName 服务名称
     * @param listener    监听器
     */
    void addListener(String serviceName, ZNamingListener listener);

    /**
     * 移除服务监听器
     *
     * @param serviceName 服务名称
     * @param listener    监听器
     */
    void removeListener(String serviceName, ZNamingListener listener);
}
