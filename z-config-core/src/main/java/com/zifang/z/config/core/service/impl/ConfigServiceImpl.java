package com.zifang.z.config.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zifang.util.core.encrypt.MD5Utils;
import com.zifang.util.core.meta.BaseStatusCode;
import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.page.Pageable;
import com.zifang.util.core.net.NetworkUtil;
import com.zifang.z.config.common.model.ConfigKey;
import com.zifang.z.config.common.model.ZConfigDTO;
import com.zifang.z.config.common.model.config.ZConfigListRequest;
import com.zifang.z.config.common.model.config.ZConfigPageRequest;
import com.zifang.z.config.common.model.config.ZConfigQueryRequest;
import com.zifang.z.config.common.model.config.ZConfigSaveRequest;
import com.zifang.z.config.core.domain.entity.ZConfigInfo;
import com.zifang.z.config.core.domain.entity.ZConfigInfoHistory;
import com.zifang.z.config.core.domain.service.IZConfigInfoHistoryService;
import com.zifang.z.config.core.domain.service.IZConfigInfoService;
import com.zifang.z.config.core.server.handler.ServerBusinessHandler;
import com.zifang.z.config.core.service.ConfigService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConfigServiceImpl implements ConfigService {

    @Resource
    private IZConfigInfoService zConfigInfoService;

    @Resource
    private IZConfigInfoHistoryService zConfigInfoHistoryService;

    @Override
    public Result<String> saveConfig(ZConfigSaveRequest ZConfigSaveRequest) {

        org.apache.logging.log4j.LogManager.getLogger(getClass()).info("saveConfig: dataId={}, group={}, namespace={}",
                ZConfigSaveRequest.getDataId(), ZConfigSaveRequest.getGroup(), ZConfigSaveRequest.getNamespace());

        // 可能数据库内没有数据，需要根据三主键进行查询, 执行回填
        if (ZConfigSaveRequest.getId() == null) {
            ZConfigInfo zConfigInfo = zConfigInfoService.queryConfig(
                    ZConfigSaveRequest.getNamespace(),
                    ZConfigSaveRequest.getGroup(),
                    ZConfigSaveRequest.getDataId()
            );
            if (zConfigInfo != null) {
                ZConfigSaveRequest.setId(zConfigInfo.getId());
            }
        }

        ZConfigDTO ZConfigDTO = convert(ZConfigSaveRequest);

        ZConfigInfo zConfigInfo = convert(ZConfigDTO);

        boolean success = false;
        if (zConfigInfo.getId() == null) {
            // 解决编辑时id为空导致的唯一键冲突问题：如果相同组合已存在则更新，否则新增
            ZConfigInfo existConfig = zConfigInfoService.getOne(new QueryWrapper<ZConfigInfo>()
                    .eq("data_id", zConfigInfo.getDataId())
                    .eq("`group`", zConfigInfo.getGroup())
                    .eq("namespace", zConfigInfo.getNamespace())
            );
            if (existConfig != null) {
                zConfigInfo.setId(existConfig.getId());
                success = zConfigInfoService.updateById(zConfigInfo);
            } else {
                success = zConfigInfoService.save(zConfigInfo);
            }
        } else {
            success = zConfigInfoService.updateById(zConfigInfo);
        }

        if (success) {
            // 保存变更历史
            ZConfigInfoHistory history = new ZConfigInfoHistory();
            history.setNid(zConfigInfo.getId());
            history.setDataId(zConfigInfo.getDataId());
            history.setGroup(zConfigInfo.getGroup());
            history.setAppName(zConfigInfo.getAppName());
            history.setContent(zConfigInfo.getContent());
            history.setMd5(zConfigInfo.getMd5());
            history.setGmtCreate(LocalDateTime.now());
            history.setGmtModified(LocalDateTime.now());
            history.setSrcUser("sys"); // TODO: 后续从上下文获取登录用户
            history.setSrcIp(NetworkUtil.getLocalIp());
            history.setOpType(zConfigInfo.getId() == null ? "新增" : "修改");
            history.setNamespace(zConfigInfo.getNamespace());
            zConfigInfoHistoryService.save(history);

            ConfigKey configKey = ConfigKey.of(ZConfigDTO.getNamespace(), ZConfigDTO.getGroup(), ZConfigDTO.getDataId());
            ServerBusinessHandler.notifyAllClients(configKey, ZConfigDTO.getContent(), ZConfigDTO.getMd5());
        }

        if (success) {
            return Result.success();
        } else {
            return Result.<String>fail("").code(BaseStatusCode.FAIL.getCode());
        }
    }

    @Override
    public Result<String> getConfig(ZConfigQueryRequest zConfigRequest) {

        // 命名空间默认值处理
        String namespace = zConfigRequest.getNameSpace();
        if (namespace == null || namespace.trim().isEmpty()) {
            namespace = "DEFAULT_NAMESPACE";
        }

        ZConfigInfo zConfigInfo = zConfigInfoService.queryConfig(
                namespace,
                zConfigRequest.getGroup(),
                zConfigRequest.getDataId()
        );

        if (zConfigInfo == null) {
            return Result.success(null);
        } else {
            return Result.success(zConfigInfo.getContent());
        }
    }

    @Override
    public Result<Pageable<ZConfigDTO>> pageConfig(ZConfigPageRequest request) {

        Page<ZConfigInfo> page = new Page<>(request.getCurrent(), request.getSize());
        QueryWrapper<ZConfigInfo> queryWrapper = new QueryWrapper<>();

        // 命名空间筛选
        String namespace = request.getNameSpace();
        if (namespace != null && !namespace.trim().isEmpty()) {
            queryWrapper.eq("namespace", namespace);
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

        IPage<ZConfigInfo> pageData = zConfigInfoService.page(page, queryWrapper);

        List<ZConfigInfo> configDTOList = pageData.getRecords();
        List<ZConfigDTO> ZConfigDto = configDTOList.stream().map(this::convert).collect(Collectors.toList());

        Pageable<ZConfigDTO> pageable = new Pageable<>();
        pageable.setTotal(page.getTotal());
        pageable.setCurrent(page.getCurrent());
        pageable.setSize(page.getSize());
        pageable.setRecords(ZConfigDto);

        return Result.success(pageable);
    }

    @Override
    public Result<List<ZConfigDTO>> listConfig(ZConfigListRequest request) {
        return null;
    }

    @Override
    public Result<String> deleteConfig(ZConfigQueryRequest request) {
        ZConfigInfo zConfigInfo = zConfigInfoService.queryConfig(
                request.getNameSpace(),
                request.getGroup(),
                request.getDataId()
        );

        if (zConfigInfo == null) {
            return Result.<String>fail("配置不存在").code(BaseStatusCode.FAIL.getCode());
        }

        boolean success = zConfigInfoService.removeById(zConfigInfo.getId());

        if (success) {
            ConfigKey configKey = ConfigKey.of(request.getNameSpace(), request.getGroup(), request.getDataId());
            ServerBusinessHandler.notifyAllClients(configKey, null, null);
        }

        return success ? Result.success() : Result.<String>fail("删除失败").code(BaseStatusCode.FAIL.getCode());
    }

    private ZConfigDTO convert(ZConfigSaveRequest ZConfigSaveRequest) {

        ZConfigDTO ZConfigDTO = new ZConfigDTO();
        BeanUtils.copyProperties(ZConfigSaveRequest, ZConfigDTO);

        // 命名空间默认值处理
        if (ZConfigDTO.getNamespace() == null || ZConfigDTO.getNamespace().trim().isEmpty()) {
            ZConfigDTO.setNamespace("DEFAULT_NAMESPACE");
        }

        if (ZConfigSaveRequest.getId() == null) {
            ZConfigDTO.setMd5(MD5Utils.encrypt(ZConfigDTO.getContent().getBytes(Charset.defaultCharset())));
            ZConfigDTO.setGmtCreate(LocalDateTime.now());
            ZConfigDTO.setGmtModified(LocalDateTime.now());
            ZConfigDTO.setCreatorStaffNo("sys");
            ZConfigDTO.setCreatorStaffNickMn("sys");
            ZConfigDTO.setCreatorStaffRealMn("sys");
            ZConfigDTO.setSourceIp(NetworkUtil.getLocalIp());
        } else {
            ZConfigDTO.setMd5(MD5Utils.encrypt(ZConfigDTO.getContent().getBytes(Charset.defaultCharset())));
            ZConfigDTO.setGmtModified(LocalDateTime.now());
        }

        return ZConfigDTO;
    }

    private ZConfigDTO convert(ZConfigInfo zConfigInfo) {
        ZConfigDTO ZConfigDTO = new ZConfigDTO();
        BeanUtils.copyProperties(zConfigInfo, ZConfigDTO);
        return ZConfigDTO;
    }

    private ZConfigInfo convert(ZConfigDTO ZConfigDTO) {
        ZConfigInfo zConfigInfo = new ZConfigInfo();
        BeanUtils.copyProperties(ZConfigDTO, zConfigInfo);
        return zConfigInfo;
    }
}
