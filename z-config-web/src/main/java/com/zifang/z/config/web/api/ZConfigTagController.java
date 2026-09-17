package com.zifang.z.config.web.api;

import com.zifang.util.core.meta.Result;
import com.zifang.z.config.core.domain.entity.ZConfigTag;
import com.zifang.z.config.core.domain.mapper.ZConfigTagMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 配置标签管理控制器（对齐 Nacos Tag 管理）
 */
@RestController
@RequestMapping("/api/config/tag")
@Tag(name = "013_配置标签管理")
public class ZConfigTagController {

    @Resource
    private ZConfigTagMapper tagMapper;

    @GetMapping("/list")
    @Operation(summary = "001_获取所有标签")
    public Result<List<ZConfigTag>> list() {
        return Result.success(tagMapper.selectList(null));
    }

    @PostMapping("/create")
    @Operation(summary = "002_创建标签")
    public Result<String> create(@RequestBody ZConfigTag tag) {
        if (tag.getTagName() == null || tag.getTagName().trim().isEmpty()) {
            return Result.fail("标签名称不能为空");
        }
        tag.setConfigCount(0);
        tag.setGmtCreate(LocalDateTime.now());
        tag.setGmtModified(LocalDateTime.now());
        tagMapper.insert(tag);
        return Result.success();
    }

    @PostMapping("/update")
    @Operation(summary = "003_更新标签")
    public Result<String> update(@RequestBody ZConfigTag tag) {
        if (tag.getId() == null) return Result.fail("缺少标签ID");
        tag.setGmtModified(LocalDateTime.now());
        tagMapper.updateById(tag);
        return Result.success();
    }

    @PostMapping("/delete")
    @Operation(summary = "004_删除标签")
    public Result<String> delete(@RequestParam Long id) {
        tagMapper.deleteById(id);
        return Result.success();
    }
}
