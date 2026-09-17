package com.zifang.z.config.core.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zifang.z.config.core.domain.entity.ZNamespace;
import com.zifang.z.config.core.domain.mapper.ZNamespaceMapper;
import com.zifang.z.config.core.domain.service.IZNamespaceService;
import org.springframework.stereotype.Service;

/**
 * 命名空间管理服务实现（对齐 Nacos Namespace CRUD + 容量管理）
 */
@Service
public class ZNamespaceServiceImpl extends ServiceImpl<ZNamespaceMapper, ZNamespace> implements IZNamespaceService {

    @Override
    public ZNamespace findByNamespaceId(String namespaceId) {
        return getOne(new QueryWrapper<ZNamespace>()
                .eq("namespace_id", namespaceId));
    }

    @Override
    public boolean isOverQuota(String namespaceId) {
        ZNamespace ns = findByNamespaceId(namespaceId);
        if (ns == null) {
            return true; // 命名空间不存在，视为超出配额
        }
        return ns.getConfigCount() >= ns.getMaxConfigCount();
    }

    @Override
    public void incrementConfigCount(String namespaceId) {
        ZNamespace ns = findByNamespaceId(namespaceId);
        if (ns != null) {
            ns.setConfigCount(ns.getConfigCount() + 1);
            updateById(ns);
        }
    }

    @Override
    public void decrementConfigCount(String namespaceId) {
        ZNamespace ns = findByNamespaceId(namespaceId);
        if (ns != null && ns.getConfigCount() > 0) {
            ns.setConfigCount(ns.getConfigCount() - 1);
            updateById(ns);
        }
    }
}
