package com.zifang.z.config.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 命名空间管理实体（对齐 Nacos Namespace 管理）
 */
@BizModel(name = "命名空间管理实体（对齐 Nacos Namespace 管理）", module = "z-config", description = "命名空间管理实体（对齐 Nacos Namespace 管理）")
@TableName("z_namespace")
public class ZNamespace implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;

    /** 命名空间ID（唯一标识，如 dev/test/prod） */
    @BizField(name = "命名空间ID（唯一标识，如 dev/test/prod）", type = BizField.FieldType.STRING)
    private String namespaceId;

    /** 命名空间名称（显示名） */
    @BizField(name = "命名空间名称（显示名）", type = BizField.FieldType.STRING)
    private String namespaceName;

    /** 命名空间描述 */
    @BizField(name = "命名空间描述", type = BizField.FieldType.STRING)
    private String namespaceDesc;

    /** 该命名空间下的配置数量 */
    @BizField(name = "该命名空间下的配置数量", type = BizField.FieldType.INTEGER)
    private Integer configCount;

    /** 最大配置数量限制（对齐 Nacos 容量管理） */
    @BizField(name = "最大配置数量限制（对齐 Nacos 容量管理）", type = BizField.FieldType.INTEGER)
    private Integer maxConfigCount;

    /** 最大配置内容大小（字节），默认100KB */
    @BizField(name = "最大配置内容大小（字节），默认100KB", type = BizField.FieldType.INTEGER)
    private Integer maxContentSize;

    /** 是否启用 */
    @BizField(name = "是否启用", type = BizField.FieldType.BOOLEAN)
    private Boolean enabled;

    /** 创建时间 */
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtCreate;

    /** 修改时间 */
    @BizField(name = "修改时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtModified;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNamespaceId() { return namespaceId; }
    public void setNamespaceId(String namespaceId) { this.namespaceId = namespaceId; }

    public String getNamespaceName() { return namespaceName; }
    public void setNamespaceName(String namespaceName) { this.namespaceName = namespaceName; }

    public String getNamespaceDesc() { return namespaceDesc; }
    public void setNamespaceDesc(String namespaceDesc) { this.namespaceDesc = namespaceDesc; }

    public Integer getConfigCount() { return configCount; }
    public void setConfigCount(Integer configCount) { this.configCount = configCount; }

    public Integer getMaxConfigCount() { return maxConfigCount; }
    public void setMaxConfigCount(Integer maxConfigCount) { this.maxConfigCount = maxConfigCount; }

    public Integer getMaxContentSize() { return maxContentSize; }
    public void setMaxContentSize(Integer maxContentSize) { this.maxContentSize = maxContentSize; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getGmtCreate() { return gmtCreate; }
    public void setGmtCreate(LocalDateTime gmtCreate) { this.gmtCreate = gmtCreate; }

    public LocalDateTime getGmtModified() { return gmtModified; }
    public void setGmtModified(LocalDateTime gmtModified) { this.gmtModified = gmtModified; }
}



