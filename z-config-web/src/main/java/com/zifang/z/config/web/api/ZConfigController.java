package com.zifang.z.config.web.api;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.page.Pageable;
import com.zifang.z.config.common.model.ZConfigDTO;
import com.zifang.z.config.common.model.config.*;
import com.zifang.z.config.core.domain.entity.ZConfigInfo;
import com.zifang.z.config.core.domain.entity.ZConfigInfoHistory;
import com.zifang.z.config.core.domain.mapper.ZConfigInfoMapper;
import com.zifang.z.config.core.domain.service.IZConfigInfoHistoryService;
import com.zifang.z.config.core.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 配置管理控制器.
 * <p>
 * API 基础路径: /api/config
 * 所属模块: z-config-web
 * 鉴权: 通过统一网关拦截,要求登录态合法
 *
 * <p>主要端点:
 * <ul>
 *   <li>POST /saveConfig — 保存(新增/更新)配置</li>
 *   <li>POST /getConfig — 按键获取配置内容</li>
 *   <li>POST /pageConfig — 分页查询配置列表</li>
 *   <li>POST /listConfig — 列表查询配置</li>
 *   <li>POST /delete — 删除配置</li>
 *   <li>GET /groupList — 获取所有 Group 列表</li>
 *   <li>POST /history/page — 分页查询配置变更历史</li>
 *   <li>POST /rollback — 回滚到指定历史版本</li>
 *   <li>GET /namespaceList — 获取所有命名空间列表</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/config")
@Tag(name = "001_配置管理")

public class ZConfigController {

    @Resource
    private ConfigService configService;

    @Resource
    private ZConfigInfoMapper configInfoMapper;

    @Resource
    private IZConfigInfoHistoryService zConfigInfoHistoryService;


    /**
     * 保存配置(新增或更新).
     *
     * @param zConfigSaveRequest 保存请求参数
     * @return 配置服务的统一返回结果
     */
    @PostMapping("/saveConfig")
    @Operation(summary = "001_保存配置")
    public Result<String> saveConfig(@RequestBody ZConfigSaveRequest zConfigSaveRequest) {
        org.apache.logging.log4j.LogManager.getLogger(getClass()).info("saveConfig called: dataId={}", zConfigSaveRequest.getDataId());
        return configService.saveConfig(zConfigSaveRequest);
    }

    /**
     * 按命名空间/Group/DataId 获取配置内容.
     *
     * @param configRequest 配置查询请求参数
     * @return 配置内容的统一返回结果
     */
    @PostMapping("/getConfig")
    @Operation(summary = "002_获取配置")

    public Result<String> getConfig(@RequestBody ZConfigQueryRequest configRequest) {
        return configService.getConfig(configRequest);
    }


    /**
     * 分页查询配置信息.
     *
     * @param request 分页查询请求参数
     * @return 分页结果封装的统一返回结构
     */
    @PostMapping("/pageConfig")
    @Operation(summary = "003_分页获取配置信息")

    public Result<Pageable<ZConfigDTO>> pageConfig(@RequestBody ZConfigPageRequest request) {
        return configService.pageConfig(request);
    }


    /**
     * 列表查询配置信息.
     *
     * @param request 列表查询请求参数
     * @return 配置 DTO 列表的统一返回结构
     */
    @PostMapping("/listConfig")
    @Operation(summary = "004_列表获取配置信息")

    public Result<List<ZConfigDTO>> listConfig(@RequestBody ZConfigListRequest request) {
        return configService.listConfig(request);
    }

    /**
     * 删除配置.
     *
     * @param request 包含定位条件的删除请求参数
     * @return 操作结果的统一返回结构
     */
    @PostMapping("/delete")
    @Operation(summary = "005_删除配置")

    public Result<String> deleteConfig(@RequestBody ZConfigQueryRequest request) {
        return configService.deleteConfig(request);
    }

    /**
     * 获取所有 Group 列表(去重).
     *
     * @return 包含全部 Group 字符串的统一返回结构
     */
    @GetMapping("/groupList")
    @Operation(summary = "006_获取所有Group列表")

    public Result<List<String>> groupList() {
        QueryWrapper<ZConfigInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("distinct `group`");
        List<ZConfigInfo> list = configInfoMapper.selectList(queryWrapper);
        List<String> groups = list.stream().map(ZConfigInfo::getGroup).collect(Collectors.toList());
        return Result.success(groups);
    }

    /**
     * 分页查询配置变更历史.
     * <p>
     * 支持按命名空间、Group 过滤,以及按 DataID 模糊搜索,结果按创建时间倒序.
     *
     * @param request 分页与过滤条件请求参数
     * @return 变更历史分页结果的统一返回结构
     */
    @PostMapping("/history/page")
    @Operation(summary = "007_分页查询配置变更历史")

    public Result<Pageable<ZConfigInfoHistory>> historyPage(@RequestBody ZConfigHistoryPageRequest request) {
        Page<ZConfigInfoHistory> page = new Page<>(request.getCurrent(), request.getSize());
        QueryWrapper<ZConfigInfoHistory> queryWrapper = new QueryWrapper<>();

        // 命名空间筛选
        if (request.getNamespace() != null) {
            queryWrapper.eq("namespace", request.getNamespace());
        }
        // Group筛选
        if (request.getGroup() != null && !request.getGroup().isEmpty()) {
            queryWrapper.eq("`group`", request.getGroup());
        }
        // DataID搜索
        if (request.getSearch() != null && !request.getSearch().isEmpty()) {
            queryWrapper.like("data_id", request.getSearch());
        }
        // 按创建时间倒序
        queryWrapper.orderByDesc("gmt_create");

        IPage<ZConfigInfoHistory> pageData = zConfigInfoHistoryService.page(page, queryWrapper);

        Pageable<ZConfigInfoHistory> pageable = new Pageable<>();
        pageable.setTotal(page.getTotal());
        pageable.setCurrent(page.getCurrent());
        pageable.setSize(page.getSize());
        pageable.setRecords(pageData.getRecords());

        return Result.success(pageable);
    }

    /**
     * 回滚到指定历史版本[Retrieve]操作.
     * <p>
     * 内部复用保存配置接口,将历史版本内容作为一次新的保存请求写入.
     *
     * @param request 携带目标历史版本定位字段的保存请求
     * @return 操作结果的统一返回结构
     */
    @Operation(summary = "009_回滚到指定历史版本")
    @PostMapping("/rollback")
    public Result<String> rollback(@RequestBody com.zifang.z.config.common.model.config.ZConfigSaveRequest request) {
        return configService.saveConfig(request);
    }

    /**
     * 获取所有命名空间列表(去重).
     *
     * @return 包含全部命名空间字符串的统一返回结构
     */
    @GetMapping("/namespaceList")
    @Operation(summary = "008_获取所有命名空间列表")

    public Result<List<String>> namespaceList() {
        QueryWrapper<ZConfigInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("distinct namespace");
        List<ZConfigInfo> list = configInfoMapper.selectList(queryWrapper);
        List<String> namespaces = list.stream().map(ZConfigInfo::getNamespace).collect(Collectors.toList());
        return Result.success(namespaces);
    }

    // =================================================================
    //  RESTful aliases for frontend (services/api.js → makeApi('config'))
    //  These mirror the existing POST /saveConfig / pageConfig / listConfig
    //  so that `configApi.list() / page() / get() / create() / update() /
    //  delete()` from the FE all work as documented.
    // =================================================================

    /**
     * RESTful alias: GET /api/config/page?page=&size=
     * <p>Mirrors {@link #pageConfig} but uses flat query params so the
     * FE's makeApi('config').page({page,size,...}) can call it via GET.</p>
     */
    @GetMapping("/page")
    @Operation(summary = "101_分页查询配置 (RESTful 别名)")
    public Result<Pageable<ZConfigDTO>> pageAlias(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String nameSpace,
            @RequestParam(required = false) String group) {
        ZConfigPageRequest req = new ZConfigPageRequest();
        req.setCurrent(current);
        req.setSize(size);
        req.setSearch(search);
        req.setNameSpace(nameSpace);
        req.setGroup(group);
        return configService.pageConfig(req);
    }

    /**
     * RESTful alias: GET /api/config/list?...
     */
    @GetMapping("/list")
    @Operation(summary = "102_列表查询配置 (RESTful 别名)")
    public Result<List<ZConfigDTO>> listAlias(
            @RequestParam(required = false) String nameSpace,
            @RequestParam(required = false) String group) {
        ZConfigListRequest req = new ZConfigListRequest();
        req.setNameSpace(nameSpace);
        req.setGroup(group);
        return configService.listConfig(req);
    }

    /**
     * RESTful alias: GET /api/config?dataId=&group=&namespace=
     */
    @GetMapping
    @Operation(summary = "103_按 DataId 查配置 (RESTful 别名)")
    public Result<String> getByIdAlias(
            @RequestParam String dataId,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String nameSpace) {
        ZConfigQueryRequest req = new ZConfigQueryRequest();
        req.setDataId(dataId);
        req.setGroup(group);
        req.setNameSpace(nameSpace);
        return configService.getConfig(req);
    }

    /**
     * RESTful alias: POST /api/config  (create or update)
     */
    @PostMapping
    @Operation(summary = "104_保存配置 (RESTful 别名)")
    public Result<String> createAlias(@RequestBody(required = false) ZConfigSaveRequest req) {
        if (req == null) {
            return Result.<String>fail("请求体不能为空").code(400);
        }
        return configService.saveConfig(req);
    }

    /**
     * RESTful alias: PUT /api/config?dataId=  (update)
     */
    @PutMapping
    @Operation(summary = "105_更新配置 (RESTful 别名)")
    public Result<String> updateAlias(
            @RequestParam String dataId,
            @RequestBody(required = false) ZConfigSaveRequest req) {
        if (req == null) {
            return Result.<String>fail("请求体不能为空").code(400);
        }
        if (req.getDataId() == null) {
            req.setDataId(dataId);
        }
        return configService.saveConfig(req);
    }

    /**
     * RESTful alias: DELETE /api/config?dataId=&group=&namespace=
     */
    @DeleteMapping
    @Operation(summary = "106_删除配置 (RESTful 别名)")
    public Result<String> deleteAlias(
            @RequestParam String dataId,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String nameSpace) {
        ZConfigQueryRequest req = new ZConfigQueryRequest();
        req.setDataId(dataId);
        req.setGroup(group);
        req.setNameSpace(nameSpace);
        return configService.deleteConfig(req);
    }
}
