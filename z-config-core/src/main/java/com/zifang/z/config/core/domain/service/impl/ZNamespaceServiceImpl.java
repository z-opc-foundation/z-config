package com.zifang.z.config.core.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zifang.z.config.core.domain.entity.ZNamespace;
import com.zifang.z.config.core.domain.mapper.ZNamespaceMapper;
import com.zifang.z.config.core.domain.service.IZNamespaceService;
import com.zifang.z.meta.api.annotation.BizMethod;
import org.springframework.stereotype.Service;

/**
 * 命名空间管理服务实现（对齐 Nacos Namespace CRUD + 容量管理）
 */
@Service
public class ZNamespaceServiceImpl extends ServiceImpl<ZNamespaceMapper, ZNamespace> implements IZNamespaceService {

    @Override
    @BizMethod(name = "findByNamespaceId", module = "z-config", description = "", operation = BizMethod.Operation.QUERY)
    public ZNamespace findByNamespaceId(String namespaceId) {
        return getOne(new QueryWrapper<ZNamespace>()
                .eq("namespace_id", namespaceId));
    }

    @Override
    @BizMethod(name = "isOverQuota", module = "z-config", description = "", operation = BizMethod.Operation.ACTION)
    public boolean isOverQuota(String namespaceId) {
        ZNamespace ns = findByNamespaceId(namespaceId);
        if (ns == null) {
            return true; // 命名空间不存在，视为超出配额
        }
        return ns.getConfigCount() >= ns.getMaxConfigCount();
    }

    @Override
    @BizMethod(name = "incrementConfigCount", module = "z-config", description = "", operation = BizMethod.Operation.ACTION)
    public void incrementConfigCount(String namespaceId) {
        ZNamespace ns = findByNamespaceId(namespaceId);
        if (ns != null) {
            ns.setConfigCount(ns.getConfigCount() + 1);
            updateById(ns);
        }
    }

    @Override
    @BizMethod(name = "decrementConfigCount", module = "z-config", description = "", operation = BizMethod.Operation.ACTION)
    public void decrementConfigCount(String namespaceId) {
        ZNamespace ns = findByNamespaceId(namespaceId);
        if (ns != null && ns.getConfigCount() > 0) {
            ns.setConfigCount(ns.getConfigCount() - 1);
            updateById(ns);
        }
    }
}



