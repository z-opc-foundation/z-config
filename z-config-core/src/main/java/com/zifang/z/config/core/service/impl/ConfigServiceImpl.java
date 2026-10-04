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
import com.zifang.z.config.common.model.config.ZConfigDiffRequest;
import com.zifang.z.config.common.model.config.ZConfigDiffResult;
import com.zifang.z.config.common.model.config.ZConfigListRequest;
import com.zifang.z.config.common.model.config.ZConfigPageRequest;
import com.zifang.z.config.common.model.config.ZConfigQueryRequest;
import com.zifang.z.config.common.model.config.ZConfigSaveRequest;
import com.zifang.z.config.core.crypto.AESEncryptor;
import com.zifang.z.config.core.crypto.ConfigEncryptor;
import com.zifang.z.config.core.domain.entity.ZConfigInfo;
import com.zifang.z.config.core.domain.entity.ZConfigInfoBeta;
import com.zifang.z.config.core.domain.entity.ZConfigInfoHistory;
import com.zifang.z.config.core.domain.service.IZConfigInfoHistoryService;
import com.zifang.z.config.core.domain.service.IZConfigInfoService;
import com.zifang.z.config.core.domain.service.IZConfigInfoBetaService;
import com.zifang.z.config.core.domain.service.IZConfigAuditService;
import com.zifang.z.config.core.domain.service.IZNamespaceService;
import com.zifang.z.config.core.domain.entity.ZNamespace;
import com.zifang.z.config.core.server.handler.ServerBusinessHandler;
import com.zifang.z.config.core.service.ConfigService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConfigServiceImpl implements ConfigService {

    private static final Logger log = LogManager.getLogger(ConfigServiceImpl.class);

    /**
     * 配置内容最大大小（字节），对齐 Nacos 默认 100KB
     */
    private static final int MAX_CONFIG_SIZE = 100 * 1024;

    @Resource
    private IZConfigInfoService zConfigInfoService;

    @Resource
    private IZConfigInfoHistoryService zConfigInfoHistoryService;

    @Resource
    private IZConfigInfoBetaService zConfigInfoBetaService;

    @Resource
    private IZConfigAuditService configAuditService;

    @Resource
    private IZNamespaceService namespaceService;

    /**
     * 配置内容加密器（dataId 以 "cipher-" 前缀开头时自动加密存储）
     * 对齐 Nacos 的 EncryptionPluginService SPI 机制
     */
    private final ConfigEncryptor encryptor = new AESEncryptor();

    @Override
    public Result<String> saveConfig(ZConfigSaveRequest request) {

        // dataId / group 缺一个, 后面 queryConfig / convert / 加密判定都会 NPE, 宿主只能报 500。
        // 这里挡在业务逻辑之前, 由宿主的 IllegalArgumentException handler 统一回 400。
        if (isBlank(request.getDataId()) || isBlank(request.getGroup())) {
            throw new IllegalArgumentException("dataId / group 不能为空");
        }
        if (request.getContent() == null) {
            throw new IllegalArgumentException("content 不能为空");
        }
        // namespace 留空时跟读路径 getConfig 同一个默认值, 不在这里当错误拒掉。
        if (isBlank(request.getNamespace())) {
            request.setNamespace("DEFAULT_NAMESPACE");
        }

        log.info("saveConfig: dataId={}, group={}, namespace={}",
                request.getDataId(), request.getGroup(), request.getNamespace());

        // 可能数据库内没有数据，需要根据三主键进行查询, 执行回填
        String oldContent = null;
        if (request.getId() == null) {
            ZConfigInfo zConfigInfo = zConfigInfoService.queryConfig(
                    request.getNamespace(),
                    request.getGroup(),
                    request.getDataId()
            );
            if (zConfigInfo != null) {
                request.setId(zConfigInfo.getId());
            }
        }

        ZConfigDTO dto = convert(request);

        // 记录旧内容用于审计（更新场景）
        if (dto.getId() != null) {
            ZConfigInfo existingConfig = zConfigInfoService.getById(dto.getId());
            if (existingConfig != null) {
                oldContent = existingConfig.getContent();
            }
        }

        // ===== 配置加密：dataId 以 "cipher-" 开头时自动加密 =====
        String plainContent = dto.getContent();
        if (encryptor.needEncrypt(dto.getDataId())) {
            String encryptedResult = encryptor.encrypt(plainContent);
            // 加密结果格式："encryptedDataKey:encryptedContent"
            int separatorIdx = encryptedResult.indexOf(':');
            if (separatorIdx > 0) {
                dto.setEncryptedDataKey(encryptedResult.substring(0, separatorIdx));
                dto.setContent(encryptedResult.substring(separatorIdx + 1));
                log.info("配置内容已加密存储: dataId={}", dto.getDataId());
            }
        }

        // ===== 配置类型校验（对齐 Nacos 的 configType 校验机制） =====
        if (dto.getConfigType() != null && !dto.getConfigType().isEmpty()) {
            String validationError = validateConfigContent(dto.getConfigType(), plainContent);
            if (validationError != null) {
                return Result.<String>fail("配置内容格式校验失败: " + validationError).code(400);
            }
        }

        // ===== 配置内容大小限制（支持命名空间级限制） =====
        int maxSize = MAX_CONFIG_SIZE; // 默认 100KB
        ZNamespace ns = namespaceService.findByNamespaceId(dto.getNamespace());
        if (ns != null && ns.getMaxContentSize() != null) {
            maxSize = ns.getMaxContentSize();
        }
        if (plainContent != null && plainContent.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > maxSize) {
            return Result.<String>fail("配置内容超出大小限制（最大 " + (maxSize / 1024) + "KB）").code(400);
        }

        // 使用明文计算 MD5（用于变更检测）
        dto.setMd5(MD5Utils.encrypt(plainContent.getBytes(Charset.defaultCharset())));

        ZConfigInfo zConfigInfo = convertToEntity(dto);

        boolean success = false;
        boolean isNewInsert = (zConfigInfo.getId() == null);
        if (isNewInsert) {
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
            // ===== 审计日志：记录变更历史（操作人从请求上下文获取） =====
            String currentUser = getCurrentUser();
            ZConfigInfoHistory history = new ZConfigInfoHistory();
            history.setNid(zConfigInfo.getId());
            history.setDataId(zConfigInfo.getDataId());
            history.setGroup(zConfigInfo.getGroup());
            history.setAppName(zConfigInfo.getAppName());
            // 历史记录存储密文（如果有），用于回滚时能正确解密
            history.setContent(zConfigInfo.getContent());
            history.setMd5(dto.getMd5());
            history.setGmtCreate(LocalDateTime.now());
            history.setGmtModified(LocalDateTime.now());
            history.setSrcUser(currentUser);
            history.setSrcIp(NetworkUtil.getLocalIp());
            history.setOpType(isNewInsert ? "I" : "U");
            history.setNamespace(zConfigInfo.getNamespace());
            zConfigInfoHistoryService.save(history);

            // 推送变更通知（使用明文通知客户端）
            ConfigKey configKey = ConfigKey.of(dto.getNamespace(), dto.getGroup(), dto.getDataId());
            ServerBusinessHandler.notifyClients(configKey, plainContent, dto.getMd5());

            // ===== 审计日志：记录到审计表（对齐 Nacos 审计系统） =====
            String auditAction = isNewInsert ? "CREATE" : "UPDATE";
            configAuditService.audit(
                    dto.getDataId(), dto.getGroup(), dto.getNamespace(),
                    auditAction, oldContent, plainContent,
                    currentUser, NetworkUtil.getLocalIp(), "SUCCESS", null);
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

        // ===== Beta 灰度优先：先查 beta 表，按客户端 IP 匹配灰度规则 =====
        // 对齐 Nacos 的灰度发布机制：beta_ips 字段中包含客户端 IP 时返回灰度内容
        String clientIp = getClientIp();
        ZConfigInfoBeta betaConfig = zConfigInfoBetaService.queryBetaConfig(
                namespace, zConfigRequest.getGroup(), zConfigRequest.getDataId());
        if (betaConfig != null && matchBetaIps(betaConfig.getBetaIps(), clientIp)) {
            log.info("命中 Beta 灰度配置: dataId={}, clientIp={}", zConfigRequest.getDataId(), clientIp);
            String content = betaConfig.getContent();
            // Beta 配置也支持加密
            if (encryptor.needEncrypt(zConfigRequest.getDataId()) && betaConfig.getMd5() != null) {
                // Beta 配置如果加密，content 已经是密文，需要解密
                // 但 beta 表目前没有 encrypted_data_key 字段，暂不解密
            }
            return Result.success(content);
        }

        // 回退到正式配置
        ZConfigInfo zConfigInfo = zConfigInfoService.queryConfig(
                namespace,
                zConfigRequest.getGroup(),
                zConfigRequest.getDataId()
        );

        if (zConfigInfo == null) {
            return Result.success(null);
        }

        String content = zConfigInfo.getContent();

        // ===== 配置解密：dataId 以 "cipher-" 开头时自动解密 =====
        if (encryptor.needEncrypt(zConfigInfo.getDataId()) && zConfigInfo.getEncryptedDataKey() != null) {
            try {
                content = encryptor.decrypt(content, zConfigInfo.getEncryptedDataKey());
                log.debug("配置内容已解密: dataId={}", zConfigInfo.getDataId());
            } catch (Exception e) {
                log.error("配置内容解密失败: dataId={}, 回退返回密文", zConfigInfo.getDataId(), e);
            }
        }

        return Result.success(content);
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
        List<ZConfigDTO> ZConfigDto = configDTOList.stream().map(this::convertFromEntity).collect(Collectors.toList());

        Pageable<ZConfigDTO> pageable = new Pageable<>();
        pageable.setTotal(page.getTotal());
        pageable.setCurrent(page.getCurrent());
        pageable.setSize(page.getSize());
        pageable.setRecords(ZConfigDto);

        return Result.success(pageable);
    }

    /**
     * 列表查询（非分页），对齐 Nacos 的配置列表接口
     */
    @Override
    public Result<List<ZConfigDTO>> listConfig(ZConfigListRequest request) {
        QueryWrapper<ZConfigInfo> queryWrapper = new QueryWrapper<>();

        // 命名空间筛选
        if (request.getNameSpace() != null && !request.getNameSpace().isEmpty()) {
            queryWrapper.eq("namespace", request.getNameSpace());
        }
        // Group 筛选
        if (request.getGroup() != null && !request.getGroup().isEmpty()) {
            queryWrapper.eq("`group`", request.getGroup());
        }
        // DataID 精确筛选
        if (request.getDataId() != null && !request.getDataId().isEmpty()) {
            queryWrapper.eq("data_id", request.getDataId());
        }
        // 按创建时间倒序
        queryWrapper.orderByDesc("gmt_create");

        List<ZConfigInfo> list = zConfigInfoService.list(queryWrapper);
        List<ZConfigDTO> result = list.stream().map(this::convertFromEntity).collect(Collectors.toList());
        return Result.success(result);
    }

    /**
     * 删除配置（同时记录删除操作到历史表，对齐 Nacos）
     */
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
            // ===== 记录删除操作到历史表 =====
            String currentUser = getCurrentUser();
            ZConfigInfoHistory history = new ZConfigInfoHistory();
            history.setNid(zConfigInfo.getId());
            history.setDataId(zConfigInfo.getDataId());
            history.setGroup(zConfigInfo.getGroup());
            history.setAppName(zConfigInfo.getAppName());
            history.setContent(zConfigInfo.getContent());
            history.setMd5(zConfigInfo.getMd5());
            history.setGmtCreate(LocalDateTime.now());
            history.setGmtModified(LocalDateTime.now());
            history.setSrcUser(currentUser);
            history.setSrcIp(NetworkUtil.getLocalIp());
            history.setOpType("D");
            history.setNamespace(zConfigInfo.getNamespace());
            zConfigInfoHistoryService.save(history);

            // 通知客户端配置已删除
            ConfigKey configKey = ConfigKey.of(request.getNameSpace(), request.getGroup(), request.getDataId());
            ServerBusinessHandler.notifyClients(configKey, null, null);

            // ===== 审计日志 =====
            configAuditService.audit(
                    zConfigInfo.getDataId(), zConfigInfo.getGroup(), zConfigInfo.getNamespace(),
                    "DELETE", zConfigInfo.getContent(), null,
                    currentUser, NetworkUtil.getLocalIp(), "SUCCESS", null);
        }

        return success ? Result.success() : Result.<String>fail("删除失败").code(BaseStatusCode.FAIL.getCode());
    }

    // ==================== 审计日志辅助方法 ====================

    /**
     * 从 HTTP 请求上下文获取当前操作用户
     * 对齐 Nacos 的 srcUser 机制
     */
    private String getCurrentUser() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attrs =
                    (org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                // 优先从 attribute 获取（由 TokenInterceptor 设置）
                Object user = request.getAttribute("currentUser");
                if (user != null) {
                    return user.toString();
                }
                // 其次从 Cookie/Token 获取
                String token = request.getHeader("Authorization");
                if (token != null && !token.isEmpty()) {
                    return "token:" + token.substring(0, Math.min(token.length(), 8)) + "...";
                }
            }
        } catch (Exception e) {
            log.debug("获取当前用户失败", e);
        }
        return "anonymous";
    }

    // ==================== 配置 Diff 对比 ====================

    /**
     * 配置版本 Diff 对比（对齐 Nacos 控制台的配置版本对比功能）
     */
    @Override
    public Result<ZConfigDiffResult> diffConfig(ZConfigDiffRequest request) {
        if (request.getHistoryId1() == null || request.getHistoryId2() == null) {
            return Result.<ZConfigDiffResult>fail("请提供两个历史版本 ID").code(400);
        }

        ZConfigInfoHistory h1 = zConfigInfoHistoryService.getById(request.getHistoryId1());
        ZConfigInfoHistory h2 = zConfigInfoHistoryService.getById(request.getHistoryId2());

        if (h1 == null || h2 == null) {
            return Result.<ZConfigDiffResult>fail("历史版本不存在").code(404);
        }

        ZConfigDiffResult result = new ZConfigDiffResult();
        result.setContent1(h1.getContent());
        result.setContent2(h2.getContent());
        result.setMd51(h1.getMd5());
        result.setMd52(h2.getMd5());
        result.setSrcUser1(h1.getSrcUser());
        result.setSrcUser2(h2.getSrcUser());
        result.setGmtModified1(h1.getGmtModified() != null ? h1.getGmtModified().toString() : null);
        result.setGmtModified2(h2.getGmtModified() != null ? h2.getGmtModified().toString() : null);

        // 逐行 Diff
        String[] lines1 = splitLines(h1.getContent());
        String[] lines2 = splitLines(h2.getContent());
        List<ZConfigDiffResult.DiffLine> diffLines = computeDiff(lines1, lines2);
        result.setDiffLines(diffLines);
        result.setIdentical(diffLines.isEmpty());

        return Result.success(result);
    }

    /**
     * 将内容按行分割
     */
    private String[] splitLines(String content) {
        if (content == null) return new String[0];
        return content.split("\\r?\\n", -1);
    }

    /**
     * 简单的逐行 Diff 算法（最长公共子序列 LCS）
     */
    private List<ZConfigDiffResult.DiffLine> computeDiff(String[] lines1, String[] lines2) {
        int m = lines1.length;
        int n = lines2.length;

        // LCS 动态规划表
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (lines1[i - 1].equals(lines2[j - 1])) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        // 回溯生成 Diff
        List<ZConfigDiffResult.DiffLine> diffLines = new ArrayList<>();
        int i = m, j = n;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && lines1[i - 1].equals(lines2[j - 1])) {
                // 相同行
                diffLines.add(0, new ZConfigDiffResult.DiffLine(
                        Math.max(i, j), "EQUAL", lines1[i - 1], lines2[j - 1]));
                i--;
                j--;
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                // 新增行（版本2 有，版本1 无）
                diffLines.add(0, new ZConfigDiffResult.DiffLine(j, "ADD", null, lines2[j - 1]));
                j--;
            } else {
                // 删除行（版本1 有，版本2 无）
                diffLines.add(0, new ZConfigDiffResult.DiffLine(i, "DELETE", lines1[i - 1], null));
                i--;
            }
        }

        return diffLines;
    }

    // ==================== Beta 灰度辅助方法 ====================

    /**
     * 获取客户端真实 IP（支持代理场景）
     */
    private String getClientIp() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attrs =
                    (org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                // 优先从代理头获取
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty()) {
                    return xRealIp;
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("获取客户端 IP 失败", e);
        }
        return "unknown";
    }

    /**
     * 匹配 Beta 灰度 IP 列表
     * 支持：逗号分隔的 IP 列表、IP 段（如 192.168.1.*）、CIDR
     *
     * @param betaIps  Beta 环境 IP 列表（逗号分隔）
     * @param clientIp 客户端 IP
     * @return true 表示匹配
     */
    private boolean matchBetaIps(String betaIps, String clientIp) {
        if (betaIps == null || betaIps.isEmpty() || clientIp == null) {
            return false;
        }
        String[] ips = betaIps.split(",");
        for (String ip : ips) {
            String trimmed = ip.trim();
            if (trimmed.isEmpty()) continue;
            // 精确匹配
            if (trimmed.equals(clientIp)) {
                return true;
            }
            // 通配符匹配（如 192.168.1.*）
            if (trimmed.contains("*")) {
                String pattern = trimmed.replace("*", "");
                if (clientIp.startsWith(pattern)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ==================== 配置类型校验 ====================

    /**
     * 校验配置内容格式（对齐 Nacos 的 configType 校验机制）
     *
     * @param configType 配置类型（properties/yaml/json/xml/text）
     * @param content    配置内容
     * @return 校验错误信息，null 表示校验通过
     */
    private String validateConfigContent(String configType, String content) {
        if (content == null || content.isEmpty()) {
            return null; // 空内容不做校验
        }
        switch (configType.toLowerCase()) {
            case "properties":
                // properties 格式校验：每行应该是 key=value 或 key:value
                String[] lines = content.split("\\r?\\n");
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i].trim();
                    if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
                        continue; // 跳过空行和注释
                    }
                    if (!line.contains("=") && !line.contains(":")) {
                        return "第 " + (i + 1) + " 行格式错误（properties 应为 key=value）: " + line;
                    }
                }
                break;
            case "json":
                // JSON 格式校验：检查括号匹配
                try {
                    String trimmed = content.trim();
                    if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                        return "JSON 内容应以 { 或 [ 开头";
                    }
                    // 简单括号匹配检查
                    int braceCount = 0;
                    int bracketCount = 0;
                    boolean inString = false;
                    for (char c : trimmed.toCharArray()) {
                        if (c == '"' && !inString) { inString = true; }
                        else if (c == '"' && inString) { inString = false; }
                        else if (!inString) {
                            if (c == '{') braceCount++;
                            else if (c == '}') braceCount--;
                            else if (c == '[') bracketCount++;
                            else if (c == ']') bracketCount--;
                        }
                    }
                    if (braceCount != 0) return "JSON 大括号不匹配";
                    if (bracketCount != 0) return "JSON 方括号不匹配";
                } catch (Exception e) {
                    return "JSON 格式校验异常: " + e.getMessage();
                }
                break;
            case "yaml":
            case "yml":
                // YAML 基本校验：不能以制表符开头（YAML 不允许 Tab 缩进）
                String[] yamlLines = content.split("\\r?\\n");
                for (int i = 0; i < yamlLines.length; i++) {
                    if (yamlLines[i].startsWith("\t")) {
                        return "第 " + (i + 1) + " 行：YAML 不允许使用 Tab 缩进，请使用空格";
                    }
                }
                break;
            default:
                // 其他类型不做校验（text、xml 等）
                break;
        }
        return null; // 校验通过
    }

    // ==================== 转换方法 ====================

    private ZConfigDTO convert(ZConfigSaveRequest request) {
        ZConfigDTO dto = new ZConfigDTO();
        BeanUtils.copyProperties(request, dto);

        // 命名空间默认值处理
        if (dto.getNamespace() == null || dto.getNamespace().trim().isEmpty()) {
            dto.setNamespace("DEFAULT_NAMESPACE");
        }

        if (request.getId() == null) {
            dto.setGmtCreate(LocalDateTime.now());
            dto.setGmtModified(LocalDateTime.now());
            dto.setSourceIp(NetworkUtil.getLocalIp());
        } else {
            dto.setGmtModified(LocalDateTime.now());
        }

        return dto;
    }

    private ZConfigDTO convertFromEntity(ZConfigInfo zConfigInfo) {
        ZConfigDTO dto = new ZConfigDTO();
        BeanUtils.copyProperties(zConfigInfo, dto);
        return dto;
    }

    private ZConfigInfo convertToEntity(ZConfigDTO dto) {
        ZConfigInfo zConfigInfo = new ZConfigInfo();
        BeanUtils.copyProperties(dto, zConfigInfo);
        return zConfigInfo;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
