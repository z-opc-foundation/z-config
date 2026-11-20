package com.zifang.z.config.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 配置标签关联实体（对齐 Nacos Tag 灰度发布）
 * 支持按标签进行配置灰度发布
 */
@BizModel(name = "配置标签关联实体（对齐 Nacos Tag 灰度发布）", module = "z-config", description = "配置标签关联实体（对齐 Nacos Tag 灰度发布） 支持按标签进行配置灰度发布")
@TableName("z_config_tag_relation")
public class ZConfigTagRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "配置ID", type = BizField.FieldType.STRING)
    private String dataId;
    @BizField(name = "配置分组", type = BizField.FieldType.STRING)
    private String groupName;
    @BizField(name = "命名空间", type = BizField.FieldType.STRING)
    private String namespace;
    @BizField(name = "标签名称", type = BizField.FieldType.STRING)
    private String tagName;
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtCreate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDataId() { return dataId; }
    public void setDataId(String dataId) { this.dataId = dataId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }
    public LocalDateTime getGmtCreate() { return gmtCreate; }
    public void setGmtCreate(LocalDateTime gmtCreate) { this.gmtCreate = gmtCreate; }
}



