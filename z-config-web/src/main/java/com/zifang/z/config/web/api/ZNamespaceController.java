package com.zifang.z.config.web.api;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.util.core.meta.Result;
import com.zifang.z.config.core.domain.entity.ZNamespace;
import com.zifang.z.config.core.domain.service.IZNamespaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 命名空间管理控制器（对齐 Nacos Namespace CRUD API）
 * <p>
 * API 基础路径: /api/namespace
 * <p>
 * 主要端点:
 * <ul>
 *   <li>GET /list — 获取所有命名空间列表</li>
 *   <li>GET /get — 根据 namespaceId 获取单个命名空间</li>
 *   <li>POST /create — 创建命名空间</li>
 *   <li>POST /update — 更新命名空间</li>
 *   <li>POST /delete — 删除命名空间</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/namespace")
@Tag(name = "010_命名空间管理")
public class ZNamespaceController {

    @Resource
    private IZNamespaceService namespaceService;

    /**
     * 获取所有命名空间列表
     */
    @GetMapping("/list")
    @Operation(summary = "001_获取所有命名空间列表")
    public Result<List<ZNamespace>> list() {
        return Result.success(namespaceService.list());
    }

    /**
     * 根据 namespaceId 获取单个命名空间
     */
    @GetMapping("/get")
    @Operation(summary = "002_获取单个命名空间")
    public Result<ZNamespace> get(@RequestParam String namespaceId) {
        ZNamespace ns = namespaceService.findByNamespaceId(namespaceId);
        return ns != null ? Result.success(ns) : Result.fail("命名空间不存在");
    }

    /**
     * 创建命名空间（对齐 Nacos POST /nacos/v1/console/namespaces）
     */
    @PostMapping("/create")
    @Operation(summary = "003_创建命名空间")
    public Result<String> create(@RequestBody ZNamespace namespace) {
        // 校验 namespaceId 唯一性
        if (namespace.getNamespaceId() == null || namespace.getNamespaceId().trim().isEmpty()) {
            return Result.fail("namespaceId 不能为空");
        }
        ZNamespace existing = namespaceService.findByNamespaceId(namespace.getNamespaceId());
        if (existing != null) {
            return Result.fail("命名空间ID已存在: " + namespace.getNamespaceId());
        }
        // 设置默认值
        if (namespace.getNamespaceName() == null) {
            namespace.setNamespaceName(namespace.getNamespaceId());
        }
        if (namespace.getConfigCount() == null) {
            namespace.setConfigCount(0);
        }
        if (namespace.getMaxConfigCount() == null) {
            namespace.setMaxConfigCount(200); // 对齐 Nacos 默认配额
        }
        if (namespace.getEnabled() == null) {
            namespace.setEnabled(true);
        }
        namespace.setGmtCreate(LocalDateTime.now());
        namespace.setGmtModified(LocalDateTime.now());
        namespaceService.save(namespace);
        return Result.success();
    }

    /**
     * 更新命名空间（对齐 Nacos PUT /nacos/v1/console/namespaces）
     */
    @PostMapping("/update")
    @Operation(summary = "004_更新命名空间")
    public Result<String> update(@RequestBody ZNamespace namespace) {
        if (namespace.getId() == null) {
            return Result.fail("缺少命名空间主键 ID");
        }
        ZNamespace existing = namespaceService.getById(namespace.getId());
        if (existing == null) {
            return Result.fail("命名空间不存在");
        }
        namespace.setGmtModified(LocalDateTime.now());
        namespaceService.updateById(namespace);
        return Result.success();
    }

    /**
     * 删除命名空间（对齐 Nacos DELETE /nacos/v1/console/namespaces）
     * 不允许删除 DEFAULT_NAMESPACE
     */
    @PostMapping("/delete")
    @Operation(summary = "005_删除命名空间")
    public Result<String> delete(@RequestParam String namespaceId) {
        if ("DEFAULT_NAMESPACE".equals(namespaceId)) {
            return Result.fail("不能删除默认命名空间");
        }
        ZNamespace ns = namespaceService.findByNamespaceId(namespaceId);
        if (ns == null) {
            return Result.fail("命名空间不存在");
        }
        // 检查是否有配置使用此命名空间
        if (ns.getConfigCount() != null && ns.getConfigCount() > 0) {
            return Result.fail("命名空间下仍有 " + ns.getConfigCount() + " 个配置，不能删除");
        }
        namespaceService.removeById(ns.getId());
        return Result.success();
    }
}
