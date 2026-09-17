package com.zifang.z.config.web.api;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zifang.util.core.encrypt.MD5Utils;
import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.page.Pageable;
import com.zifang.z.config.common.model.config.ZConfigSaveRequest;
import com.zifang.z.config.core.domain.entity.ZConfigApproval;
import com.zifang.z.config.core.domain.mapper.ZConfigApprovalMapper;
import com.zifang.z.config.core.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 配置审批控制器（对齐 Nacos 配置审批工作流）
 * <p>
 * 工作流程：
 * 1. 申请人提交配置变更审批
 * 2. 审批人查看待审批列表
 * 3. 审批人批准或拒绝
 * 4. 批准后自动执行配置变更
 */
@RestController
@RequestMapping("/api/config/approval")
@Tag(name = "017_配置审批工作流")
public class ZConfigApprovalController {

    @Resource
    private ZConfigApprovalMapper approvalMapper;

    @Resource
    private ConfigService configService;

    /**
     * 提交配置审批
     */
    @PostMapping("/submit")
    @Operation(summary = "001_提交配置审批")
    public Result<String> submit(@RequestBody ZConfigApproval approval) {
        if (approval.getDataId() == null || approval.getAction() == null) {
            return Result.fail("配置ID和操作类型不能为空");
        }
        approval.setStatus("PENDING");
        approval.setApplyTime(LocalDateTime.now());
        if (approval.getContent() != null) {
            approval.setMd5(MD5Utils.encrypt(approval.getContent().getBytes(StandardCharsets.UTF_8)));
        }
        approvalMapper.insert(approval);
        return Result.success("审批申请已提交，等待审批");
    }

    /**
     * 审批通过
     */
    @PostMapping("/approve")
    @Operation(summary = "002_审批通过")
    public Result<String> approve(
            @RequestParam Long id,
            @RequestParam String approver,
            @RequestParam(required = false) String remark) {

        ZConfigApproval approval = approvalMapper.selectById(id);
        if (approval == null) {
            return Result.fail("审批记录不存在");
        }
        if (!"PENDING".equals(approval.getStatus())) {
            return Result.fail("该审批已处理");
        }

        // 更新审批状态
        approval.setStatus("APPROVED");
        approval.setApprover(approver);
        approval.setApproveTime(LocalDateTime.now());
        approval.setRemark(remark);
        approvalMapper.updateById(approval);

        // 执行配置变更
        try {
            ZConfigSaveRequest req = new ZConfigSaveRequest();
            req.setDataId(approval.getDataId());
            req.setGroup(approval.getGroupName());
            req.setNamespace(approval.getNamespace());
            req.setContent(approval.getContent());
            Result<String> result = configService.saveConfig(req);
            if (result.isSuccess()) {
                return Result.success("审批通过，配置已发布");
            } else {
                return Result.fail("审批通过，但配置发布失败: " + result.getMessage());
            }
        } catch (Exception e) {
            return Result.fail("审批通过，但配置发布异常: " + e.getMessage());
        }
    }

    /**
     * 审批拒绝
     */
    @PostMapping("/reject")
    @Operation(summary = "003_审批拒绝")
    public Result<String> reject(
            @RequestParam Long id,
            @RequestParam String approver,
            @RequestParam(required = false) String remark) {

        ZConfigApproval approval = approvalMapper.selectById(id);
        if (approval == null) {
            return Result.fail("审批记录不存在");
        }
        if (!"PENDING".equals(approval.getStatus())) {
            return Result.fail("该审批已处理");
        }

        approval.setStatus("REJECTED");
        approval.setApprover(approver);
        approval.setApproveTime(LocalDateTime.now());
        approval.setRemark(remark);
        approvalMapper.updateById(approval);

        return Result.success("审批已拒绝");
    }

    /**
     * 查询待审批列表
     */
    @PostMapping("/pending")
    @Operation(summary = "004_查询待审批列表")
    public Result<Pageable<ZConfigApproval>> pending(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size) {

        Page<ZConfigApproval> page = new Page<>(current, size);
        IPage<ZConfigApproval> pageData = approvalMapper.selectPage(page,
                new QueryWrapper<ZConfigApproval>()
                        .eq("status", "PENDING")
                        .orderByDesc("apply_time"));

        Pageable<ZConfigApproval> pageable = new Pageable<>();
        pageable.setTotal(pageData.getTotal());
        pageable.setCurrent(pageData.getCurrent());
        pageable.setSize(pageData.getSize());
        pageable.setRecords(pageData.getRecords());

        return Result.success(pageable);
    }

    /**
     * 查询审批历史
     */
    @PostMapping("/history")
    @Operation(summary = "005_查询审批历史")
    public Result<Pageable<ZConfigApproval>> history(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size,
            @RequestParam(required = false) String status) {

        Page<ZConfigApproval> page = new Page<>(current, size);
        QueryWrapper<ZConfigApproval> queryWrapper = new QueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq("status", status);
        }
        queryWrapper.orderByDesc("apply_time");

        IPage<ZConfigApproval> pageData = approvalMapper.selectPage(page, queryWrapper);

        Pageable<ZConfigApproval> pageable = new Pageable<>();
        pageable.setTotal(pageData.getTotal());
        pageable.setCurrent(pageData.getCurrent());
        pageable.setSize(pageData.getSize());
        pageable.setRecords(pageData.getRecords());

        return Result.success(pageable);
    }
}
