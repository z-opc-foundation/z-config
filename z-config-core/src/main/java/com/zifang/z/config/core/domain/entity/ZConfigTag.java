package com.zifang.z.config.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 配置标签实体（对齐 Nacos Tag 管理）
 */
@BizModel(name = "配置标签实体（对齐 Nacos Tag 管理）", module = "z-config", description = "配置标签实体（对齐 Nacos Tag 管理）")
@TableName("z_config_tag")
public class ZConfigTag implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "标签名称", type = BizField.FieldType.STRING)
    private String tagName;
    @BizField(name = "标签描述", type = BizField.FieldType.STRING)
    private String tagDesc;
    @BizField(name = "标签颜色", type = BizField.FieldType.STRING)
    private String tagColor;
    @BizField(name = "关联配置数量", type = BizField.FieldType.INTEGER)
    private Integer configCount;
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtCreate;
    @BizField(name = "修改时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtModified;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }
    public String getTagDesc() { return tagDesc; }
    public void setTagDesc(String tagDesc) { this.tagDesc = tagDesc; }
    public String getTagColor() { return tagColor; }
    public void setTagColor(String tagColor) { this.tagColor = tagColor; }
    public Integer getConfigCount() { return configCount; }
    public void setConfigCount(Integer configCount) { this.configCount = configCount; }
    public LocalDateTime getGmtCreate() { return gmtCreate; }
    public void setGmtCreate(LocalDateTime gmtCreate) { this.gmtCreate = gmtCreate; }
    public LocalDateTime getGmtModified() { return gmtModified; }
    public void setGmtModified(LocalDateTime gmtModified) { this.gmtModified = gmtModified; }
}



