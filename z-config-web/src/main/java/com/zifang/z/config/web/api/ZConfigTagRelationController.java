package com.zifang.z.config.web.api;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.util.core.meta.Result;
import com.zifang.z.config.core.domain.entity.ZConfigTagRelation;
import com.zifang.z.config.core.domain.mapper.ZConfigTagRelationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
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
    private ZConfigTagRelationMapper relationMapper;

    /**
     * 获取配置的所有标签
     */
    @GetMapping("/list")
    @Operation(summary = "001_获取配置的标签列表")
    public Result<List<ZConfigTagRelation>> list(
            @RequestParam String dataId,
            @RequestParam String group,
            @RequestParam String namespace) {
        return Result.success(relationMapper.selectList(
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
    public Result<String> bind(@RequestBody ZConfigTagRelation relation) {
        if (relation.getDataId() == null || relation.getTagName() == null) {
            return Result.fail("配置ID和标签名不能为空");
        }
        relation.setGmtCreate(LocalDateTime.now());
        try {
            relationMapper.insert(relation);
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
    public Result<String> unbind(@RequestParam Long id) {
        relationMapper.deleteById(id);
        return Result.success();
    }

    /**
     * 获取标签下的所有配置（用于按标签灰度发布）
     */
    @GetMapping("/byTag")
    @Operation(summary = "004_获取标签下的所有配置")
    public Result<List<ZConfigTagRelation>> byTag(@RequestParam String tagName) {
        return Result.success(relationMapper.selectList(
                new QueryWrapper<ZConfigTagRelation>()
                        .eq("tag_name", tagName)));
    }
}
