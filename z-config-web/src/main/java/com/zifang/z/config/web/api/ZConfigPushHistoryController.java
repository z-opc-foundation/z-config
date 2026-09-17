package com.zifang.z.config.web.api;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.page.Pageable;
import com.zifang.z.config.core.domain.entity.ZConfigPushHistory;
import com.zifang.z.config.core.domain.mapper.ZConfigPushHistoryMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

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
    public Result<Pageable<ZConfigPushHistory>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size,
            @RequestParam(required = false) String dataId,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String namespace) {

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
