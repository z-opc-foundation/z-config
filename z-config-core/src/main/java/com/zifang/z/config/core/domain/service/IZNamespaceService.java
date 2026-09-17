package com.zifang.z.config.core.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zifang.z.config.core.domain.entity.ZNamespace;

/**
 * 命名空间管理服务接口（对齐 Nacos Namespace CRUD）
 */
public interface IZNamespaceService extends IService<ZNamespace> {

    /**
     * 根据 namespaceId 查询命名空间
     *
     * @param namespaceId 命名空间唯一标识
     * @return 命名空间实体
     */
    ZNamespace findByNamespaceId(String namespaceId);

    /**
     * 检查命名空间是否超出容量限制
     *
     * @param namespaceId 命名空间ID
     * @return true 表示超出限制
     */
    boolean isOverQuota(String namespaceId);

    /**
     * 增加命名空间的配置计数
     *
     * @param namespaceId 命名空间ID
     */
    void incrementConfigCount(String namespaceId);

    /**
     * 减少命名空间的配置计数
     *
     * @param namespaceId 命名空间ID
     */
    void decrementConfigCount(String namespaceId);
}
