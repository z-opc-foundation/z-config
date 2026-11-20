package com.zifang.z.config.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 命名空间权限实体（对齐 Nacos 命名空间级 RBAC）
 */
@BizModel(name = "命名空间权限实体（对齐 Nacos 命名空间级 RBAC）", module = "z-config", description = "命名空间权限实体（对齐 Nacos 命名空间级 RBAC）")
@TableName("z_namespace_permission")
public class ZNamespacePermission implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "用户名", type = BizField.FieldType.STRING)
    private String username;
    @BizField(name = "命名空间ID", type = BizField.FieldType.STRING)
    private String namespaceId;
    /** 权限类型：read/write/admin */
    @BizField(name = "权限类型：read/write/admin", type = BizField.FieldType.STRING)
    private String permission;
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtCreate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNamespaceId() { return namespaceId; }
    public void setNamespaceId(String namespaceId) { this.namespaceId = namespaceId; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    public LocalDateTime getGmtCreate() { return gmtCreate; }
    public void setGmtCreate(LocalDateTime gmtCreate) { this.gmtCreate = gmtCreate; }
}



