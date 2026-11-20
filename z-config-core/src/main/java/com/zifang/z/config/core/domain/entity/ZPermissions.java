package com.zifang.z.config.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;

/**
 * 无注释
 */
@BizModel(name = "无注释", module = "z-config", description = "无注释")
@TableName("z_permissions")
public class ZPermissions implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;

    /**
     * 关联z_roles.role
     */
    @TableField("role")
    @BizField(name = "关联z_roles.role", type = BizField.FieldType.STRING)
    private String role;

    /**
     * 资源标识（如配置data_id:group）
     */
    @TableField("resource")
    @BizField(name = "资源标识（如配置data_id:group）", type = BizField.FieldType.STRING)
    private String resource;

    /**
     * 权限操作（read/write/delete）
     */
    @TableField("action")
    @BizField(name = "权限操作（read/write/delete）", type = BizField.FieldType.STRING)
    private String action;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}

