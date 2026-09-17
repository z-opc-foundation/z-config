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
import com.zifang.z.config.core.server.handler.ServerBusinessHandler;
import com.zifang.z.config.common.model.ConfigKey;
import com.zifang.z.config.core.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
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
     * 回滚前预览对比（对齐 Nacos 的回滚确认机制）
     * 返回当前配置与历史版本的 Diff 对比，供用户确认后再执行回滚
     *
     * @param historyId 历史版本 ID
     * @return 当前配置与历史版本的对比结果
     */
    @PostMapping("/rollback/preview")
    @Operation(summary = "010_回滚前预览对比")
    public Result<ZConfigDiffResult> rollbackPreview(@RequestParam Long historyId) {
        ZConfigInfoHistory history = zConfigInfoHistoryService.getById(historyId);
        if (history == null) {
            return Result.fail("历史版本不存在");
        }

        // 查询当前配置
        ZConfigInfo currentConfig = configInfoMapper.selectOne(
                new QueryWrapper<ZConfigInfo>()
                        .eq("data_id", history.getDataId())
                        .eq("`group`", history.getGroup())
                        .eq("namespace", history.getNamespace()));

        ZConfigDiffResult result = new ZConfigDiffResult();
        if (currentConfig != null) {
            result.setContent1(currentConfig.getContent());
            result.setMd51(currentConfig.getMd5());
            result.setGmtModified1(currentConfig.getGmtModified() != null ? currentConfig.getGmtModified().toString() : null);
        }
        result.setContent2(history.getContent());
        result.setMd52(history.getMd5());
        result.setGmtModified2(history.getGmtModified() != null ? history.getGmtModified().toString() : null);
        result.setSrcUser2(history.getSrcUser());

        return Result.success(result);
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

    // ==================== 版本管理增强（对齐 Nacos 版本管理） ====================

    /**
     * 获取配置的版本时间线（对齐 Nacos 的版本管理功能）
     * 返回配置的所有历史版本，按时间倒序排列
     *
     * @param dataId    配置ID
     * @param group     分组
     * @param namespace 命名空间
     * @return 版本时间线
     */
    @PostMapping("/version/timeline")
    @Operation(summary = "118_获取配置版本时间线")
    public Result<List<ZConfigInfoHistory>> versionTimeline(
            @RequestParam String dataId,
            @RequestParam String group,
            @RequestParam String namespace) {

        QueryWrapper<ZConfigInfoHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("data_id", dataId)
                .eq("`group`", group)
                .eq("namespace", namespace)
                .orderByDesc("gmt_create");

        List<ZConfigInfoHistory> history = zConfigInfoHistoryService.list(queryWrapper);
        return Result.success(history);
    }

    /**
     * 版本对比（任意两个版本之间）
     *
     * @param version1Id 第一个版本 ID
     * @param version2Id 第二个版本 ID
     * @return 对比结果
     */
    @PostMapping("/version/compare")
    @Operation(summary = "119_版本对比")
    public Result<ZConfigDiffResult> versionCompare(
            @RequestParam Long version1Id,
            @RequestParam Long version2Id) {

        ZConfigInfoHistory v1 = zConfigInfoHistoryService.getById(version1Id);
        ZConfigInfoHistory v2 = zConfigInfoHistoryService.getById(version2Id);

        if (v1 == null || v2 == null) {
            return Result.fail("版本不存在");
        }

        ZConfigDiffResult result = new ZConfigDiffResult();
        result.setContent1(v1.getContent());
        result.setContent2(v2.getContent());
        result.setMd51(v1.getMd5());
        result.setMd52(v2.getMd5());
        result.setSrcUser1(v1.getSrcUser());
        result.setSrcUser2(v2.getSrcUser());
        result.setGmtModified1(v1.getGmtModified() != null ? v1.getGmtModified().toString() : null);
        result.setGmtModified2(v2.getGmtModified() != null ? v2.getGmtModified().toString() : null);

        return Result.success(result);
    }

    // ==================== 配置全文搜索（对齐 Nacos 搜索能力） ====================

    /**
     * 全文搜索配置（对齐 Nacos 的配置搜索功能）
     * 支持按 dataId、group、内容关键字搜索
     *
     * @param search    搜索关键字
     * @param nameSpace 命名空间（可选）
     * @param group     分组（可选）
     * @return 匹配的配置列表
     */
    @PostMapping("/search")
    @Operation(summary = "113_全文搜索配置")
    public Result<List<ZConfigDTO>> search(
            @RequestParam String search,
            @RequestParam(required = false) String nameSpace,
            @RequestParam(required = false) String group) {

        QueryWrapper<ZConfigInfo> queryWrapper = new QueryWrapper<>();

        // 多字段模糊搜索：dataId、group、content
        queryWrapper.and(wrapper -> wrapper
                .like("data_id", search)
                .or().like("`group`", search)
                .or().like("content", search)
        );

        if (nameSpace != null && !nameSpace.isEmpty()) {
            queryWrapper.eq("namespace", nameSpace);
        }
        if (group != null && !group.isEmpty()) {
            queryWrapper.eq("`group`", group);
        }

        queryWrapper.last("LIMIT 100"); // 限制返回数量

        List<ZConfigInfo> list = configInfoMapper.selectList(queryWrapper);
        List<ZConfigDTO> result = list.stream()
                .map(this::convertSearchResult)
                .collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 搜索结果转换（高亮匹配的关键字）
     */
    private ZConfigDTO convertSearchResult(ZConfigInfo info) {
        ZConfigDTO dto = new ZConfigDTO();
        dto.setId(info.getId());
        dto.setDataId(info.getDataId());
        dto.setGroup(info.getGroup());
        dto.setNamespace(info.getNamespace());
        dto.setConfigType(info.getConfigType());
        dto.setConfigDesc(info.getConfigDesc());
        dto.setGmtCreate(info.getGmtCreate());
        dto.setGmtModified(info.getGmtModified());
        // 搜索结果不返回完整内容，只返回摘要（前 200 字符）
        if (info.getContent() != null && info.getContent().length() > 200) {
            dto.setContent(info.getContent().substring(0, 200) + "...");
        } else {
            dto.setContent(info.getContent());
        }
        return dto;
    }

    // ==================== 配置备份恢复（对齐 Nacos 灾备能力） ====================

    /**
     * 全量备份所有配置（对齐 Nacos 的配置备份功能）
     * 返回 JSON 格式的完整配置快照，可用于灾难恢复
     *
     * @return 包含所有配置的 JSON 数据
     */
    @PostMapping("/backup")
    @Operation(summary = "114_全量备份配置")
    public Result<Map<String, Object>> backup() {
        QueryWrapper<ZConfigInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("namespace", "`group`", "data_id");
        List<ZConfigInfo> allConfigs = configInfoMapper.selectList(queryWrapper);

        Map<String, Object> backup = new java.util.LinkedHashMap<>();
        backup.put("version", "1.0");
        backup.put("timestamp", System.currentTimeMillis());
        backup.put("totalCount", allConfigs.size());
        backup.put("configs", allConfigs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()));

        return Result.success(backup);
    }

    /**
     * 从备份恢复配置（对齐 Nacos 的配置恢复功能）
     * 支持选择性恢复：可以指定只恢复特定命名空间的配置
     *
     * @param backupData 备份数据
     * @param targetNamespace 目标命名空间（可选，为空则恢复到原命名空间）
     * @return 恢复结果
     */
    @PostMapping("/restore")
    @Operation(summary = "115_从备份恢复配置")
    @SuppressWarnings("unchecked")
    public Result<String> restore(
            @RequestBody Map<String, Object> backupData,
            @RequestParam(required = false) String targetNamespace) {

        if (backupData == null || !backupData.containsKey("configs")) {
            return Result.fail("备份数据格式错误");
        }

        List<Map<String, Object>> configs = (List<Map<String, Object>>) backupData.get("configs");
        if (configs == null || configs.isEmpty()) {
            return Result.fail("备份中无配置数据");
        }

        int success = 0;
        int failed = 0;
        for (Map<String, Object> config : configs) {
            try {
                ZConfigSaveRequest req = new ZConfigSaveRequest();
                req.setDataId((String) config.get("dataId"));
                req.setGroup((String) config.get("group"));
                req.setNamespace(targetNamespace != null && !targetNamespace.isEmpty()
                        ? targetNamespace : (String) config.get("namespace"));
                req.setContent((String) config.get("content"));
                req.setConfigType((String) config.get("configType"));
                req.setConfigDesc((String) config.get("configDesc"));

                Result<String> result = configService.saveConfig(req);
                if (result.isSuccess()) {
                    success++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
            }
        }
        return Result.success("恢复完成: 成功 " + success + " 个, 失败 " + failed + " 个");
    }

    private ZConfigDTO convertToDTO(ZConfigInfo info) {
        ZConfigDTO dto = new ZConfigDTO();
        dto.setId(info.getId());
        dto.setDataId(info.getDataId());
        dto.setGroup(info.getGroup());
        dto.setNamespace(info.getNamespace());
        dto.setContent(info.getContent());
        dto.setConfigType(info.getConfigType());
        dto.setConfigDesc(info.getConfigDesc());
        dto.setMd5(info.getMd5());
        dto.setGmtCreate(info.getGmtCreate());
        dto.setGmtModified(info.getGmtModified());
        return dto;
    }

    // ==================== 配置订阅管理（对齐 Nacos 订阅管理） ====================

    /**
     * 获取配置的订阅者列表（对齐 Nacos 的配置订阅管理功能）
     * 返回当前正在监听指定配置的客户端连接信息
     *
     * @param dataId    配置ID
     * @param group     分组
     * @param namespace 命名空间
     * @return 订阅者列表（客户端地址 + 连接时间）
     */
    @GetMapping("/subscriber/list")
    @Operation(summary = "116_获取配置订阅者列表")
    public Result<List<Map<String, Object>>> subscriberList(
            @RequestParam String dataId,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String namespace) {

        ConfigKey configKey = ConfigKey.of(
                namespace != null ? namespace : "DEFAULT_NAMESPACE",
                group != null ? group : "DEFAULT_GROUP",
                dataId);

        List<String> clients = ServerBusinessHandler.getListenerClients(configKey);
        List<Map<String, Object>> subscribers = new java.util.ArrayList<>();

        for (String client : clients) {
            Map<String, Object> subscriber = new java.util.LinkedHashMap<>();
            subscriber.put("clientAddress", client);
            subscriber.put("configKey", configKey.toString());
            subscriber.put("subscribeTime", System.currentTimeMillis());
            subscribers.add(subscriber);
        }

        return Result.success(subscribers);
    }

    /**
     * 获取所有配置的订阅统计（对齐 Nacos 的订阅监控）
     *
     * @return 各配置的订阅者数量
     */
    @GetMapping("/subscriber/stats")
    @Operation(summary = "117_获取订阅统计")
    public Result<Map<String, Object>> subscriberStats() {
        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("totalActiveListeners", ServerBusinessHandler.getActiveListenerCount());
        stats.put("timestamp", System.currentTimeMillis());
        return Result.success(stats);
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

    /**
     * 配置版本 Diff 对比（对齐 Nacos 控制台的配置版本对比功能）
     * <p>
     * 对比两个历史版本的差异，返回逐行 Diff 结果。
     *
     * @param request Diff 对比请求（包含两个历史版本 ID）
     * @return Diff 对比结果
     */
    @PostMapping("/history/diff")
    @Operation(summary = "107_配置版本 Diff 对比")
    public Result<com.zifang.z.config.common.model.config.ZConfigDiffResult> diffHistory(
            @RequestBody com.zifang.z.config.common.model.config.ZConfigDiffRequest request) {
        return configService.diffConfig(request);
    }

    // ==================== 配置导入导出（对齐 Nacos 批量操作） ====================

    /**
     * 配置导出：按命名空间和分组导出所有配置（对齐 Nacos 导出功能）
     * 返回 JSON 格式的配置列表，可下载为文件
     *
     * @param nameSpace 命名空间
     * @param group     分组（可选，不传则导出所有分组）
     * @return 配置导出数据
     */
    @PostMapping("/export")
    @Operation(summary = "108_配置导出")
    public Result<List<ZConfigDTO>> exportConfigs(
            @RequestParam(required = false) String nameSpace,
            @RequestParam(required = false) String group) {
        ZConfigListRequest req = new ZConfigListRequest();
        req.setNameSpace(nameSpace);
        req.setGroup(group);
        return configService.listConfig(req);
    }

    /**
     * 配置导入：批量导入配置（对齐 Nacos 导入功能）
     * 支持 JSON 格式的配置列表导入
     *
     * @param configs 配置列表
     * @return 导入结果（成功数/失败数）
     */
    @PostMapping("/import")
    @Operation(summary = "109_配置导入")
    public Result<String> importConfigs(@RequestBody List<ZConfigDTO> configs) {
        int success = 0;
        int failed = 0;
        for (ZConfigDTO dto : configs) {
            try {
                ZConfigSaveRequest req = new ZConfigSaveRequest();
                req.setDataId(dto.getDataId());
                req.setGroup(dto.getGroup());
                req.setNamespace(dto.getNamespace());
                req.setContent(dto.getContent());
                req.setConfigType(dto.getConfigType());
                req.setConfigDesc(dto.getConfigDesc());
                Result<String> result = configService.saveConfig(req);
                if (result.isSuccess()) {
                    success++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
            }
        }
        return Result.success("导入完成: 成功 " + success + " 个, 失败 " + failed + " 个");
    }

    /**
     * 配置克隆：将一个命名空间的配置克隆到另一个命名空间（对齐 Nacos 克隆功能）
     *
     * @param sourceNamespace 源命名空间
     * @param targetNamespace 目标命名空间
     * @return 克隆结果
     */
    @PostMapping("/clone")
    @Operation(summary = "110_配置克隆")
    public Result<String> cloneConfigs(
            @RequestParam String sourceNamespace,
            @RequestParam String targetNamespace) {
        ZConfigListRequest req = new ZConfigListRequest();
        req.setNameSpace(sourceNamespace);
        Result<List<ZConfigDTO>> listResult = configService.listConfig(req);

        if (!listResult.isSuccess() || listResult.getData() == null) {
            return Result.fail("源命名空间无配置或查询失败");
        }

        int success = 0;
        int failed = 0;
        for (ZConfigDTO dto : listResult.getData()) {
            try {
                ZConfigSaveRequest saveReq = new ZConfigSaveRequest();
                saveReq.setDataId(dto.getDataId());
                saveReq.setGroup(dto.getGroup());
                saveReq.setNamespace(targetNamespace);
                saveReq.setContent(dto.getContent());
                saveReq.setConfigType(dto.getConfigType());
                Result<String> result = configService.saveConfig(saveReq);
                if (result.isSuccess()) {
                    success++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
            }
        }
        return Result.success("克隆完成: 成功 " + success + " 个, 失败 " + failed + " 个");
    }

    // ==================== 配置监听者查询（对齐 Nacos 监听者管理） ====================

    /**
     * 查询配置的监听者列表（对齐 Nacos 的配置监听者查询功能）
     * 返回当前正在监听指定配置的客户端连接列表
     *
     * @param dataId    配置ID
     * @param group     分组
     * @param namespace 命名空间
     * @return 监听者客户端地址列表
     */
    @GetMapping("/listener/list")
    @Operation(summary = "111_查询配置监听者列表")
    public Result<List<String>> listenerList(
            @RequestParam String dataId,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String namespace) {
        ConfigKey configKey = ConfigKey.of(
                namespace != null ? namespace : "DEFAULT_NAMESPACE",
                group != null ? group : "DEFAULT_GROUP",
                dataId);
        List<String> listeners = ServerBusinessHandler.getListenerClients(configKey);
        return Result.success(listeners);
    }

    /**
     * 获取所有活跃监听者数量（对齐 Nacos 的监控指标）
     */
    @GetMapping("/listener/count")
    @Operation(summary = "112_获取活跃监听者数量")
    public Result<Integer> listenerCount() {
        return Result.success(ServerBusinessHandler.getActiveListenerCount());
    }
}
