package com.zifang.z.config.web.api;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.page.Pageable;
import com.zifang.z.config.core.domain.entity.ZConfigPushHistory;
import com.zifang.z.config.core.domain.mapper.ZConfigPushHistoryMapper;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 配置推送通知历史控制器（对齐 Nacos 推送轨迹管理）
 */
@RestController
@RequestMapping("/api/config/push")
@Tag(name = "014_推送通知历史")
public class ZConfigPushHistoryController {

    @Resource
    private ZConfigPushHistoryMapper pushHistoryMapper;

    /**
     * 分页查询推送历史
     */
    @PostMapping("/page")
    @Operation(summary = "001_分页查询推送历史")
    @BizApi(summary = "分页查询推送历史", tags = { "z-config" })
    public Result<Pageable<ZConfigPushHistory>> page(
            @RequestParam(defaultValue = "1")
            @BizParam(name = "current", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.QUERY) Long current,
            @RequestParam(defaultValue = "20")
            @BizParam(name = "size", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.QUERY) Long size,
            @RequestParam(required = false)
            @BizParam(name = "dataId", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String dataId,
            @RequestParam(required = false)
            @BizParam(name = "group", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String group,
            @RequestParam(required = false)
            @BizParam(name = "namespace", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String namespace) {

        Page<ZConfigPushHistory> page = new Page<>(current, size);
        QueryWrapper<ZConfigPushHistory> queryWrapper = new QueryWrapper<>();

        if (dataId != null && !dataId.isEmpty()) {
            queryWrapper.eq("data_id", dataId);
        }
        if (group != null && !group.isEmpty()) {
            queryWrapper.eq("group_name", group);
        }
        if (namespace != null && !namespace.isEmpty()) {
            queryWrapper.eq("namespace", namespace);
        }

        queryWrapper.orderByDesc("push_time");

        IPage<ZConfigPushHistory> pageData = pushHistoryMapper.selectPage(page, queryWrapper);

        Pageable<ZConfigPushHistory> pageable = new Pageable<>();
        pageable.setTotal(pageData.getTotal());
        pageable.setCurrent(pageData.getCurrent());
        pageable.setSize(pageData.getSize());
        pageable.setRecords(pageData.getRecords());

        return Result.success(pageable);
    }
}









