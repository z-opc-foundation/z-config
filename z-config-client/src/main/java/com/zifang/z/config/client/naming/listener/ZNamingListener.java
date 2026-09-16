package com.zifang.z.config.client.naming.listener;

import com.zifang.z.config.common.model.ZNamingInstance;

import java.util.List;

/**
 * Naming服务变更监听器接口
 * 用于接收服务实例变更通知
 */
public interface ZNamingListener {

    /**
     * 服务实例变更回调
     *
     * @param serviceName 服务名称
     * @param instances   当前服务实例列表
     */
    void onChange(String serviceName, List<ZNamingInstance> instances);

    /**
     * 服务实例添加回调
     *
     * @param serviceName 服务名称
     * @param instance    新增实例
     */
    default void onInstanceAdded(String serviceName, ZNamingInstance instance) {
    }

    /**
     * 服务实例移除回调
     *
     * @param serviceName 服务名称
     * @param instance    移除的实例
     */
    default void onInstanceRemoved(String serviceName, ZNamingInstance instance) {
    }
}
