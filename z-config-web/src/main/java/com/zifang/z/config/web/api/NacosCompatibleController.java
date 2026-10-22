package com.zifang.z.config.web.api;

import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.page.Pageable;
import com.zifang.z.config.common.model.ZConfigDTO;
import com.zifang.z.config.common.model.config.ZConfigPageRequest;
import com.zifang.z.config.common.model.config.ZConfigQueryRequest;
import com.zifang.z.config.common.model.config.ZConfigSaveRequest;
import com.zifang.z.config.core.service.ConfigService;
import com.zifang.z.config.web.config.NacosAccessTokenVerifier;
import com.zifang.z.config.web.config.NacosCompatProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nacos OpenAPI v1 兼容面。
 *
 * <p>让标准 nacos-client（以及所有按 Nacos v1 REST 约定写的配置拉取代码）不改一行就能接 z-config，
 * 这是「z-config 完整替换 nacos」在协议层的前提。自有 API 在 {@code /api/config/**}，
 * 是 POST + JSON body + Result 包装；Nacos 面必须严格复刻它的 GET/POST/DELETE + query/form
 * 参数 + 裸值返回，两者签名不能混用。
 *
 * <p>语义映射：
 * <ul>
 *   <li>{@code tenant} ⇄ {@code namespace}；Nacos 传空串或 {@code public} 都按 public 命名空间处理</li>
 *   <li>{@code group} 缺省为 {@code DEFAULT_GROUP}</li>
 *   <li>{@code pageNo}/{@code pageSize} ⇄ 内部 {@code current}/{@code size}</li>
 * </ul>
 *
 * <p>鉴权：Nacos 走自己的 accessToken 机制，与本服务的 Cookie token 无关，故本路径在
 * {@code WebConfig} 中被排除在 {@code TokenInterceptor} 之外。
 *
 * <p>⚠ 2026-10-22（TASK-20261022-020 · T4/T5）**订正**：上面那句鉴权说明是**目标形态**，
 * 而**当时代码里没有任何 accessToken 校验**（只有这一句注释声称有）⇒ 兼容面曾对公网
 * 完全免鉴权（匿名可读 + 可发布 + 可删配置）。现状：
 * <ul>
 *   <li>{@code /nacos/**} 已收进 {@code sso.intercept-paths} 且已从
 *       {@code sso.exclude-paths} 与 {@code TokenInterceptor} 排除名单中移除
 *       ⇒ 匿名一律 401/302。</li>
 *   <li>accessToken 校验（真 Nacos 语义）由 {@code NacosAccessTokenAuthFilter} +
 *       {@code NacosAccessTokenVerifier} 提供，**默认关闭**（T1 未拍板）；
 *       本类里也做一次同口径复核（纵深防御）。</li>
 *   <li>两个只读元信息端点是否对外，由 {@code nacos.compat.metadata-endpoints.enabled} 控制，
 *       默认 true（不改变现状）。</li>
 * </ul>
 */
@RestController
@RequestMapping("/nacos/v1")
@Tag(name = "900_Nacos兼容面")
public class NacosCompatibleController {

    private static final Logger log = LogManager.getLogger(NacosCompatibleController.class);

    private static final String DEFAULT_GROUP = "DEFAULT_GROUP";
    private static final String PUBLIC_NAMESPACE = "";

    @Resource
    private ConfigService configService;

    /** accessToken 校验器（默认不启用；启用口径见 nacos.compat.access-token.*）。 */
    @Resource
    private NacosAccessTokenVerifier accessTokenVerifier;

    /** 兼容面暴露口径（只读元信息端点开关）。 */
    @Resource
    private NacosCompatProperties compatProperties;

    // ---------------------------------------------------------------- config

    /**
     * 获取配置内容，或按 search 模式列出配置列表。
     *
     * <p>Nacos 用同一个 URL 承载两种语义：带 {@code search} 参数是列表，不带是取内容。
     */
    @GetMapping(value = "/cs/configs")
    @Operation(summary = "900_获取配置内容或配置列表")
    public org.springframework.http.ResponseEntity<?> getConfigs(
            @RequestParam(value = "dataId", required = false) String dataId,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "tenant", required = false) String tenant,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "pageNo", required = false, defaultValue = "1") int pageNo,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
            @RequestParam(value = "accessToken", required = false) String accessToken) {

        if (!accessTokenVerifier.isValid(accessToken)) {
            return accessTokenDeniedJson();
        }

        if (search != null && !search.isEmpty()) {
            // 显式定 Content-Type，不靠内容协商挑 converter。这套 starter 的协商配置下
            // produces=ALL_VALUE + 无 Accept 头会选不出任何 converter，Map 直接抛
            // "No converter for [class java.util.HashMap] with preset Content-Type 'null'"。
            return org.springframework.http.ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(listConfigs(dataId, group, tenant, search, pageNo, pageSize));
        }

        ZConfigQueryRequest request = new ZConfigQueryRequest();
        request.setNameSpace(toNamespace(tenant));
        request.setGroup(emptyToDefault(group));
        request.setDataId(dataId);

        Result<String> result = configService.getConfig(request);
        if (result == null || !result.isSuccess()) {
            // Nacos 约定：内容不存在返回 404 + 纯文本说明，客户端据此判定「未配置」而非报错
            return org.springframework.http.ResponseEntity.status(404)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("config data not exist");
        }
        String content = result.getData();
        if (content == null) {
            return org.springframework.http.ResponseEntity.status(404)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("config data not exist");
        }
        return org.springframework.http.ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }

    @PostMapping(value = "/cs/configs", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "900_发布配置")
    public org.springframework.http.ResponseEntity<?> publishConfig(@RequestParam("dataId") String dataId,
                                 @RequestParam(value = "group", required = false) String group,
                                 @RequestParam(value = "tenant", required = false) String tenant,
                                 @RequestParam("content") String content,
                                 @RequestParam(value = "appName", required = false) String appName,
                                 @RequestParam(value = "type", required = false) String type,
                                 @RequestParam(value = "accessToken", required = false) String accessToken) {

        if (!accessTokenVerifier.isValid(accessToken)) {
            return accessTokenDeniedText();
        }

        ZConfigSaveRequest request = new ZConfigSaveRequest();
        request.setDataId(dataId);
        request.setGroup(emptyToDefault(group));
        request.setContent(content);
        request.setNamespace(toNamespace(tenant));
        request.setAppName(appName);
        request.setConfigType(type);

        Result<String> result = configService.saveConfig(request);
        boolean ok = result != null && result.isSuccess();
        if (!ok) {
            log.warn("nacos publishConfig failed: dataId={}, result={}", dataId, result);
        }
        return org.springframework.http.ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(String.valueOf(ok));
    }

    @DeleteMapping(value = "/cs/configs", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "900_删除配置")
    public org.springframework.http.ResponseEntity<?> removeConfig(@RequestParam("dataId") String dataId,
                               @RequestParam(value = "group", required = false) String group,
                               @RequestParam(value = "tenant", required = false) String tenant,
                               @RequestParam(value = "accessToken", required = false) String accessToken) {

        if (!accessTokenVerifier.isValid(accessToken)) {
            return accessTokenDeniedText();
        }

        ZConfigQueryRequest request = new ZConfigQueryRequest();
        request.setNameSpace(toNamespace(tenant));
        request.setGroup(emptyToDefault(group));
        request.setDataId(dataId);

        Result<String> result = configService.deleteConfig(request);
        return org.springframework.http.ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(String.valueOf(result != null && result.isSuccess()));
    }

    // ------------------------------------------------------------- namespace

    @GetMapping("/console/namespaces")
    @Operation(summary = "900_命名空间列表")
    public org.springframework.http.ResponseEntity<?> namespaces(
            @RequestParam(value = "accessToken", required = false) String accessToken) {

        if (!accessTokenVerifier.isValid(accessToken)) {
            return accessTokenDeniedJson();
        }
        if (!compatProperties.isMetadataEndpointsEnabled()) {
            return metadataEndpointDisabled();
        }

        ZConfigQueryRequest request = new ZConfigQueryRequest();
        request.setNameSpace(PUBLIC_NAMESPACE);

        List<Map<String, Object>> items = new ArrayList<>();
        try {
            Result<Pageable<ZConfigDTO>> result = configService.pageConfig(toPageRequest(request));
            if (result != null && result.isSuccess() && result.getData() != null) {
                Map<String, Long> used = new LinkedHashMap<>();
                for (ZConfigDTO dto : result.getData().getRecords()) {
                    String ns = dto.getNamespace() == null ? PUBLIC_NAMESPACE : dto.getNamespace();
                    used.merge(ns, 1L, Long::sum);
                }
                for (Map.Entry<String, Long> entry : used.entrySet()) {
                    items.add(namespaceItem(entry.getKey(), entry.getValue()));
                }
            }
        } catch (Exception e) {
            log.warn("nacos namespaces list failed", e);
        }
        if (items.isEmpty()) {
            items.add(namespaceItem(PUBLIC_NAMESPACE, 0L));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("data", items);
        return org.springframework.http.ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(nacosEnvelope(data));
    }

    // ------------------------------------------------------------------ misc

    @GetMapping("/ns/operator/servers")
    @Operation(summary = "900_集群节点信息")
    public org.springframework.http.ResponseEntity<?> operatorServers(
            @RequestParam(value = "accessToken", required = false) String accessToken) {

        if (!accessTokenVerifier.isValid(accessToken)) {
            return accessTokenDeniedJson();
        }
        if (!compatProperties.isMetadataEndpointsEnabled()) {
            return metadataEndpointDisabled();
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("version", "2.0.0-zconfig-compat");

        Map<String, Object> server = new HashMap<>();
        server.put("name", "z-config");
        server.put("group", "DEFAULT_GROUP");
        server.put("clusters", "");
        server.put("lastRefTime", System.currentTimeMillis());
        server.put("alive", true);
        server.put("metadata", metadata);
        server.put("extendedData", new ArrayList<>());
        return org.springframework.http.ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(server);
    }

    // --------------------------------------------------------------- helpers

    /**
     * accessToken 校验失败（真 Nacos 语义：{@code code=403}）。
     * <p>默认口径下（{@code nacos.compat.access-token.enabled=false}）本分支**不会**被触发 ——
     * 那时兼容面的保护来自 z-ctc 的 SSO 闸门（{@code /nacos/**} 在 {@code sso.intercept-paths} 内）。
     */
    private org.springframework.http.ResponseEntity<?> accessTokenDeniedJson() {
        return org.springframework.http.ResponseEntity.status(403)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"code\":403,\"message\":\"invalid accessToken\",\"data\":null}");
    }

    /** 写端点（POST/DELETE）沿用 text/plain 形态的拒绝响应。 */
    private org.springframework.http.ResponseEntity<?> accessTokenDeniedText() {
        return org.springframework.http.ResponseEntity.status(403)
                .contentType(MediaType.TEXT_PLAIN)
                .body("invalid accessToken");
    }

    /**
     * 只读元信息端点被关闭（T5：{@code nacos.compat.metadata-endpoints.enabled=false}）时的响应。
     * <p>用 404 而不是 403：端点「不存在」比「存在但没权限」少泄露一条信息。
     */
    private org.springframework.http.ResponseEntity<?> metadataEndpointDisabled() {
        return org.springframework.http.ResponseEntity.status(404)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"code\":404,\"message\":\"endpoint not available\",\"data\":null}");
    }

    private Map<String, Object> listConfigs(String dataId, String group, String tenant,
                                            String search, int pageNo, int pageSize) {
        ZConfigPageRequest request = new ZConfigPageRequest();
        request.setNameSpace(toNamespace(tenant));
        request.setGroup(group);
        request.setDataId(dataId);
        request.setSearch("accurate".equals(search) ? null : search);
        request.setCurrent((long) Math.max(pageNo, 1));
        request.setSize((long) Math.max(pageSize, 1));

        List<Map<String, Object>> items = new ArrayList<>();
        long total = 0;
        try {
            Result<Pageable<ZConfigDTO>> result = configService.pageConfig(request);
            if (result != null && result.isSuccess() && result.getData() != null) {
                Pageable<ZConfigDTO> page = result.getData();
                total = page.getTotal();
                for (ZConfigDTO dto : page.getRecords()) {
                    items.add(configItem(dto, toNamespace(tenant)));
                }
            }
        } catch (Exception e) {
            log.warn("nacos listConfigs failed", e);
        }

        int pagesAvailable = pageSize <= 0 ? 0 : (int) Math.ceil((double) total / (double) pageSize);
        Map<String, Object> out = new HashMap<>();
        out.put("totalCount", total);
        out.put("pageNumber", pageNo);
        out.put("pagesAvailable", pagesAvailable);
        out.put("pageItems", items);
        return out;
    }

    private Map<String, Object> configItem(ZConfigDTO dto, String tenant) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", dto.getId() == null ? null : String.valueOf(dto.getId()));
        item.put("dataId", dto.getDataId());
        item.put("group", dto.getGroup());
        item.put("content", dto.getContent());
        item.put("md5", dto.getMd5());
        item.put("tenant", tenant);
        item.put("appName", dto.getAppName());
        item.put("type", dto.getConfigType());
        return item;
    }

    private Map<String, Object> namespaceItem(String namespace, long configCount) {
        Map<String, Object> item = new HashMap<>();
        item.put("namespace", namespace);
        item.put("namespaceShow", namespace.isEmpty() ? "public" : namespace);
        item.put("namespaceDesc", PUBLIC_NAMESPACE.equals(namespace) ? "默认命名空间" : namespace);
        item.put("quota", 200L);
        item.put("configCount", configCount);
        item.put("type", 0);
        return item;
    }

    private Map<String, Object> nacosEnvelope(Map<String, Object> data) {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("code", 0);
        envelope.put("message", "success");
        envelope.put("data", data.get("data"));
        return envelope;
    }

    private ZConfigPageRequest toPageRequest(ZConfigQueryRequest query) {
        ZConfigPageRequest request = new ZConfigPageRequest();
        request.setNameSpace(query.getNameSpace());
        request.setGroup(query.getGroup());
        request.setDataId(query.getDataId());
        request.setCurrent(1L);
        request.setSize(1000L);
        return request;
    }

    private String emptyToDefault(String group) {
        return group == null || group.isEmpty() ? DEFAULT_GROUP : group;
    }

    private String toNamespace(String tenant) {
        if (tenant == null || tenant.isEmpty() || "public".equals(tenant)) {
            return PUBLIC_NAMESPACE;
        }
        return tenant;
    }
}
