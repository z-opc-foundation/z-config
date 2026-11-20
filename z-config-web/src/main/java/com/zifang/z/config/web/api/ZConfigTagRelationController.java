package com.zifang.z.config.web.api;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.util.core.meta.Result;
import com.zifang.z.config.core.domain.entity.ZConfigTagRelation;
import com.zifang.z.config.core.domain.mapper.ZConfigTagRelationMapper;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 配置标签关联控制器（对齐 Nacos Tag 灰度发布）
 * 支持按标签进行配置灰度发布
 */
@RestController
@RequestMapping("/api/config/tag/relation")
@Tag(name = "016_配置标签灰度")
public class ZConfigTagRelationController {

    @Resource
    private ZConfigTagRelationMapper zConfigTagRelationMapper;

    /**
     * 获取配置的所有标签
     */
    @GetMapping("/list")
    @Operation(summary = "001_获取配置的标签列表")
    @BizApi(summary = "获取配置的所有标签", tags = { "z-config" })
    public Result<List<ZConfigTagRelation>> list(
            @RequestParam @BizParam(name = "dataId", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String dataId,
            @RequestParam @BizParam(name = "group", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String group,
            @RequestParam @BizParam(name = "namespace", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String namespace) {
        return Result.success(zConfigTagRelationMapper.selectList(
                new QueryWrapper<ZConfigTagRelation>()
                        .eq("data_id", dataId)
                        .eq("group_name", group)
                        .eq("namespace", namespace)));
    }

    /**
     * 给配置打标签（用于灰度发布）
     */
    @PostMapping("/bind")
    @Operation(summary = "002_给配置打标签")
    @BizApi(summary = "给配置打标签（用于灰度发布）", tags = { "z-config" })
    public Result<String> bind(@RequestBody @BizParam(name = "relation", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.BODY) ZConfigTagRelation relation) {
        if (relation.getDataId() == null || relation.getTagName() == null) {
            return Result.fail("配置ID和标签名不能为空");
        }
        relation.setGmtCreate(LocalDateTime.now());
        try {
            zConfigTagRelationMapper.insert(relation);
        } catch (Exception e) {
            // 唯一键冲突，忽略
        }
        return Result.success();
    }

    /**
     * 移除配置标签
     */
    @PostMapping("/unbind")
    @Operation(summary = "003_移除配置标签")
    @BizApi(summary = "移除配置标签", tags = { "z-config" })
    public Result<String> unbind(@RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.QUERY) Long id) {
        zConfigTagRelationMapper.deleteById(id);
        return Result.success();
    }

    /**
     * 获取标签下的所有配置（用于按标签灰度发布）
     */
    @GetMapping("/byTag")
    @Operation(summary = "004_获取标签下的所有配置")
    @BizApi(summary = "获取标签下的所有配置（用于按标签灰度发布）", tags = { "z-config" })
    public Result<List<ZConfigTagRelation>> byTag(@RequestParam @BizParam(name = "tagName", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String tagName) {
        return Result.success(zConfigTagRelationMapper.selectList(
                new QueryWrapper<ZConfigTagRelation>()
                        .eq("tag_name", tagName)));
    }
}









