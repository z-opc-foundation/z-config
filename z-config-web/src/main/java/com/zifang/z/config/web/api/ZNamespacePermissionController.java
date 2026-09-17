package com.zifang.z.config.web.api;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.util.core.meta.Result;
import com.zifang.z.config.core.domain.entity.ZNamespacePermission;
import com.zifang.z.config.core.domain.mapper.ZNamespacePermissionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 命名空间权限管理控制器（对齐 Nacos 命名空间级 RBAC）
 */
@RestController
@RequestMapping("/api/namespace/permission")
@Tag(name = "015_命名空间权限管理")
public class ZNamespacePermissionController {

    @Resource
    private ZNamespacePermissionMapper permissionMapper;

    /**
     * 查询用户在某命名空间的权限
     */
    @GetMapping("/check")
    @Operation(summary = "001_查询用户权限")
    public Result<Boolean> check(
            @RequestParam String username,
            @RequestParam String namespaceId,
            @RequestParam(defaultValue = "read") String permission) {

        // admin 用户拥有所有权限
        if ("admin".equals(username)) {
            return Result.success(true);
        }

        ZNamespacePermission perm = permissionMapper.selectOne(
                new QueryWrapper<ZNamespacePermission>()
                        .eq("username", username)
                        .eq("namespace_id", namespaceId)
                        .eq("permission", permission));

        return Result.success(perm != null);
    }

    /**
     * 获取用户的所有命名空间权限
     */
    @GetMapping("/list")
    @Operation(summary = "002_获取用户权限列表")
    public Result<List<ZNamespacePermission>> list(@RequestParam String username) {
        return Result.success(permissionMapper.selectList(
                new QueryWrapper<ZNamespacePermission>()
                        .eq("username", username)));
    }

    /**
     * 授权用户命名空间权限
     */
    @PostMapping("/grant")
    @Operation(summary = "003_授权命名空间权限")
    public Result<String> grant(@RequestBody ZNamespacePermission permission) {
        if (permission.getUsername() == null || permission.getNamespaceId() == null) {
            return Result.fail("用户名和命名空间不能为空");
        }
        permission.setGmtCreate(LocalDateTime.now());
        permissionMapper.insert(permission);
        return Result.success();
    }

    /**
     * 撤销用户命名空间权限
     */
    @PostMapping("/revoke")
    @Operation(summary = "004_撤销命名空间权限")
    public Result<String> revoke(@RequestParam Long id) {
        permissionMapper.deleteById(id);
        return Result.success();
    }
}
