package com.zifang.z.config.core.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

/**
 * 无注释
 */
@TableName("z_roles")
public class ZRoles implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联z_users.username
     */
    @TableField("username")
    private String username;

    /**
     * 角色名
     */
    @TableField("role")
    private String role;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}