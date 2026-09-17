package com.zifang.z.config.web.api;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.page.Pageable;
import com.zifang.z.config.core.domain.entity.ZConfigAudit;
import com.zifang.z.config.core.domain.service.IZConfigAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 配置审计日志控制器（对齐 Nacos 审计系统）
 */
@RestController
@RequestMapping("/api/config/audit")
@Tag(name = "011_配置审计日志")
public class ZConfigAuditController {

    @Resource
    private IZConfigAuditService auditService;

    /**
     * 分页查询审计日志
     */
    @PostMapping("/page")
    @Operation(summary = "001_分页查询审计日志")
    public Result<Pageable<ZConfigAudit>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size,
            @RequestParam(required = false) String dataId,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String namespace,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String srcUser) {

        Page<ZConfigAudit> page = new Page<>(current, size);
        QueryWrapper<ZConfigAudit> queryWrapper = new QueryWrapper<>();

        if (dataId != null && !dataId.isEmpty()) {
            queryWrapper.eq("data_id", dataId);
        }
        if (group != null && !group.isEmpty()) {
            queryWrapper.eq("group_name", group);
        }
        if (namespace != null && !namespace.isEmpty()) {
            queryWrapper.eq("namespace", namespace);
        }
        if (action != null && !action.isEmpty()) {
            queryWrapper.eq("action", action);
        }
        if (srcUser != null && !srcUser.isEmpty()) {
            queryWrapper.eq("src_user", srcUser);
        }

        queryWrapper.orderByDesc("gmt_create");

        IPage<ZConfigAudit> pageData = auditService.page(page, queryWrapper);

        Pageable<ZConfigAudit> pageable = new Pageable<>();
        pageable.setTotal(pageData.getTotal());
        pageable.setCurrent(pageData.getCurrent());
        pageable.setSize(pageData.getSize());
        pageable.setRecords(pageData.getRecords());

        return Result.success(pageable);
    }

    /**
     * 查询单条审计详情
     */
    @GetMapping("/detail")
    @Operation(summary = "002_查询审计详情")
    public Result<ZConfigAudit> detail(@RequestParam Long id) {
        ZConfigAudit audit = auditService.getById(id);
        return audit != null ? Result.success(audit) : Result.fail("审计记录不存在");
    }
}
