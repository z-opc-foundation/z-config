package com.zifang.z.config.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;

/**
 * 无注释
 */
@BizModel(name = "无注释", module = "z-config", description = "无注释")
@TableName("z_users")
public class ZUsers implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    @TableId(type = IdType.INPUT)
    @BizField(name = "用户名", type = BizField.FieldType.STRING)
    private String username;

    /**
     * 密码（BCrypt加密）
     */
    @BizField(name = "密码（BCrypt加密）", type = BizField.FieldType.STRING)
    private String password;

    /**
     * 是否启用（1-启用，0-禁用）
     */
    @BizField(name = "是否启用（1-启用，0-禁用）", type = BizField.FieldType.BOOLEAN)
    private Boolean enabled;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}



