package com.zifang.z.config.web.offline;

import com.zifang.ctc.sso.JwtUtil;
import com.zifang.ctc.sso.config.SsoProperties;
import com.zifang.util.core.meta.Result;
import com.zifang.z.config.common.model.ZConfigDTO;
import com.zifang.z.config.common.model.config.ZConfigListRequest;
import com.zifang.z.config.core.domain.mapper.ZConfigApprovalMapper;
import com.zifang.z.config.core.domain.mapper.ZConfigInfoMapper;
import com.zifang.z.config.core.domain.mapper.ZConfigPushHistoryMapper;
import com.zifang.z.config.core.domain.mapper.ZConfigTagMapper;
import com.zifang.z.config.core.domain.mapper.ZConfigTagRelationMapper;
import com.zifang.z.config.core.domain.mapper.ZNamespacePermissionMapper;
import com.zifang.z.config.core.domain.mapper.ZE18nMessageMapper;
import com.zifang.z.config.core.domain.service.IZClusterService;
import com.zifang.z.config.core.domain.service.IZConfigAuditService;
import com.zifang.z.config.core.domain.service.IZConfigInfoHistoryService;
import com.zifang.z.config.core.domain.service.IZConfigInfoService;
import com.zifang.z.config.core.domain.service.IZInstanceService;
import com.zifang.z.config.core.domain.service.IZNamespaceService;
import com.zifang.z.config.core.domain.service.IZServiceInfoService;
import com.zifang.z.config.core.domain.service.IZUsersService;
import com.zifang.z.config.core.service.ConfigService;
import com.zifang.z.config.core.service.ZNamingService;
import com.zifang.z.config.web.config.NacosCompatRetiredPathFilter;
import com.zifang.z.config.web.config.ZConfigWebAutoConfiguration;
import com.zifang.z.config.web.config.TokenInterceptor;
import com.zifang.z.config.web.config.WebConfig;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.servlet.Filter;
import javax.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * TASK-20261022-T1 · Nacos 兼容面（{@code /nacos/v1/**}）**下线**的反向钉（钉 a / 钉 b / 钉 c）。
 *
 * <p><b>背景</b>：用户 2026-10-22 拍板 T1 = ②「直接下线」—— 兼容面整段退役，不是"保留但加 accessToken"，
 * 也不是"收进内网"。本测试要证明的是**真下线**：
 * <ol>
 *   <li><b>钉 a（正向）</b>：真实 Spring 上下文里 {@code /nacos/v1/**} <b>没有 handler</b>，
 *       且对读/写/删/两个元信息端点发<b>真实请求</b> ⇒ 一律 <b>404</b>，
 *       且响应体<b>不是</b>业务信封、也不是"被闸门拦"的 401/403/302。</li>
 *   <li><b>钉 b（尺子牙口自证）</b>：把兼容面控制器<b>加回来</b>时，同一套判据必须<b>变红</b>
 *       —— 否则钉 a 就是"永远绿"的空转测试。</li>
 *   <li><b>钉 c（回归）</b>：下线<b>不能误伤</b> z-config 自有 API（{@code /api/config/**}）。</li>
 * </ol>
 *
 * <p><b>为什么不用"读源码断言"</b>：本测试<b>不</b>去 grep 源码里的注解，而是
 * ① 启动**真实**的 Spring 上下文（扫描真实的控制器包 {@code com.zifang.z.config.web.api}，
 * 导入真实的 {@link WebConfig} 与 {@link TokenInterceptor}，SSO 闸门由 z-ctc-sso 自动装配带入，
 * 与生产同一套），② 读**运行时**的 {@link RequestMappingHandlerMapping} handler 表，
 * ③ 用 MockMvc 打**真实请求**。所以"把控制器加回来"这件事会让本测试立刻变红。
 *
 * <p><b>上下文里为什么要挂一堆 mock</b>：本模块的完整上下文要连 MySQL（{@code ConfigModuleDataSource}），
 * 单测里起不来 ⇒ 这里只扫描<b>控制器包</b>，把控制器依赖的 service / mapper 用 mock 顶掉。
 * 代价是：将来新增控制器若引入新依赖，本测试会因缺 bean 而失败（那时补一个 mock 即可）。
 */
@SpringBootTest(classes = NacosCompatOfflinePinTest.OfflineContext.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class NacosCompatOfflinePinTest {

    /**
     * 下线后的**真实**上下文：
     * <ul>
     *   <li>扫描真实的控制器包（真下线要能证明"控制器不在"，就必须让真实控制器有机会进上下文，
     *       而不是拿个替身顶上）；</li>
     *   <li>导入真实的 {@link WebConfig}（SPA 回落口径 + 已退役路径硬闸的注册）与 {@link TokenInterceptor}；</li>
     *   <li>SSO 闸门由 z-ctc-sso 的自动装配带入（口径与生产同一套：application.properties 会被真实加载）。</li>
     * </ul>
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            // 本仓自己的自动装配（它 @ComponentScan("com.zifang.z.config")）必须排掉：
            // 它会把 com.zifang.z.config.web.auth 下的**测试替身控制器**（别的测试类里的
            // @RestController）一起扫进来 ⇒ 与真实控制器抢同一路径 ⇒ Ambiguous mapping。
            // 这里改为**显式**扫描真实控制器包 com.zifang.z.config.web.api。
            ZConfigWebAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class
    })
    @ComponentScan(basePackages = "com.zifang.z.config.web.api")
    @Import({WebConfig.class, TokenInterceptor.class})
    static class OfflineContext {
    }

    // ------------------------------------------------- 控制器依赖（全部 mock，避免连库）

    @MockBean
    private ConfigService configService;
    @MockBean
    private ZNamingService zNamingService;
    @MockBean
    private IZClusterService clusterService;
    @MockBean
    private IZUsersService usersService;
    @MockBean
    private IZConfigInfoService configInfoService;
    @MockBean
    private IZInstanceService instanceService;
    @MockBean
    private IZServiceInfoService serviceInfoService;
    @MockBean
    private IZConfigInfoHistoryService zConfigInfoHistoryService;
    @MockBean
    private IZConfigAuditService auditService;
    @MockBean
    private IZNamespaceService namespaceService;
    @MockBean
    private ZConfigApprovalMapper approvalMapper;
    @MockBean
    private ZConfigInfoMapper configInfoMapper;
    @MockBean
    private ZConfigPushHistoryMapper pushHistoryMapper;
    @MockBean
    private ZConfigTagMapper tagMapper;
    @MockBean
    private ZConfigTagRelationMapper zConfigTagRelationMapper;
    @MockBean
    private ZE18nMessageMapper messageMapper;
    @MockBean
    private ZNamespacePermissionMapper zNamespacePermissionMapper;

    @org.springframework.beans.factory.annotation.Autowired
    private WebApplicationContext context;
    @org.springframework.beans.factory.annotation.Autowired
    private RequestMappingHandlerMapping handlerMapping;
    @org.springframework.beans.factory.annotation.Autowired
    private SsoProperties ssoProperties;
    @org.springframework.beans.factory.annotation.Autowired
    private JwtUtil jwtUtil;

    /** 生产等价：带"已退役路径硬闸"（= 线上真实过滤器链）。 */
    private MockMvc mvc;
    /** 纯 MVC 层：不挂硬闸，用来单独验"SPA 回落不会把兼容面伪装成 200 首页"。 */
    private MockMvc mvcMvcLayerOnly;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(retiredPathFilter())
                .build();
        mvcMvcLayerOnly = MockMvcBuilders.webAppContextSetup(context).build();

        when(configService.listConfig(any(ZConfigListRequest.class)))
                .thenReturn(Result.success(Collections.<ZConfigDTO>emptyList()));
    }

    // ------------------------------------------------------------------ 钉 a

    @Test
    @DisplayName("钉 a：handler 表里没有任何 /nacos/** 映射；读/写/删/元信息端点一律 404（非 401/403/302）")
    void nacosCompatSurfaceIsGoneAndAnswers404() throws Exception {
        List<String> mappings = nacosMappings(handlerMapping);
        System.out.println("[钉 a · handler 表] /nacos/** 映射数 = " + mappings.size() + " ⇒ " + mappings);
        assertTrue(mappings.isEmpty(),
                "Nacos 兼容面必须**没有** handler（真下线），实际映射: " + mappings);

        // 读端点 + 两个只读元信息端点
        for (String path : new String[]{
                "/nacos/v1/cs/configs",
                "/nacos/v1/console/namespaces",
                "/nacos/v1/ns/operator/servers"}) {
            assertNotBusinessEnvelope(mvc.perform(get(path).accept(MediaType.APPLICATION_JSON)).andReturn(),
                    "GET " + path);
        }

        // 写端点（发布 / 删除）—— 下线后不得再有任何"写原语"
        assertNotBusinessEnvelope(mvc.perform(post("/nacos/v1/cs/configs")
                .accept(MediaType.APPLICATION_JSON)
                .param("dataId", "pin-probe")
                .param("group", "DEFAULT_GROUP")
                .param("content", "pin-probe")).andReturn(), "POST /nacos/v1/cs/configs");
        assertNotBusinessEnvelope(mvc.perform(delete("/nacos/v1/cs/configs")
                .accept(MediaType.APPLICATION_JSON)
                .param("dataId", "pin-probe")
                .param("group", "DEFAULT_GROUP")).andReturn(), "DELETE /nacos/v1/cs/configs");
    }

    @Test
    @DisplayName("钉 a（纯 MVC 层，不挂硬闸）：GET /nacos/v1/cs/configs 必须是 404，而不是 SPA 首页 200")
    void mvcLayerAloneAnswers404InsteadOfSpaIndex() throws Exception {
        MvcResult result = mvcMvcLayerOnly.perform(get("/nacos/v1/cs/configs")
                .accept(MediaType.APPLICATION_JSON)).andReturn();
        MockHttpServletResponse response = result.getResponse();
        String body = response.getContentAsString();

        System.out.println("[钉 a · 纯 MVC 层] GET /nacos/v1/cs/configs = " + response.getStatus()
                + ", body长度 = " + body.length()
                + ", 是否 SPA 首页 = " + looksLikeSpaIndex(body));

        assertEquals(404, response.getStatus(),
                "控制器已删除 ⇒ 兼容面路径在纯 MVC 层也必须 404（不能是 405，更不能是 200 + SPA 首页）");
        assertFalse(looksLikeSpaIndex(body),
                "兼容面路径绝不能回落成 SPA 首页（200 + index.html）—— 那是假装下线");
    }

    @Test
    @DisplayName("钉 a（配置口径）：/nacos/** 不得出现在 sso.intercept-paths / sso.exclude-paths 里")
    void nacosPathIsNotConfiguredAnywhereInSsoGate() {
        System.out.println("[钉 a · 配置] intercept-paths = " + ssoProperties.getInterceptPaths()
                + " ; exclude-paths = " + ssoProperties.getExcludePaths());

        assertFalse(ssoProperties.getInterceptPaths().contains("/nacos/**"),
                "/nacos/** 不得留在拦截范围：闸门在 handler 之后执行，留着就会把落点变成 401/302"
                        + "（= 假装下线，且对外宣称兼容面还在）");
        assertFalse(ssoProperties.getExcludePaths().contains("/nacos/**"),
                "/nacos/** 不得出现在豁免名单：给一条已下线的路径显式放行，是给后人留伏笔");
    }

    // ------------------------------------------------------------------ 钉 b

    @Test
    @DisplayName("钉 b（尺子牙口自证）：把兼容面控制器加回来时，同一套判据必须变红")
    void detectorTurnsRedWhenNacosControllerComesBack() throws Exception {
        AnnotationConfigWebApplicationContext fake = new AnnotationConfigWebApplicationContext();
        // @EnableWebMvc 会注册 resourceHandlerMapping，它需要一个 ServletContext（不起真容器）。
        fake.setServletContext(new org.springframework.mock.web.MockServletContext());
        fake.register(FakeNacosApp.class);
        fake.refresh();
        try {
            RequestMappingHandlerMapping fakeMapping = fake.getBean(RequestMappingHandlerMapping.class);
            List<String> found = nacosMappings(fakeMapping);
            System.out.println("[钉 b] 假上下文（把兼容面加回来）里的 /nacos/** 映射 = " + found);

            assertFalse(found.isEmpty(),
                    "判据必须能发现 /nacos/** 映射，否则钉 a 是空转（永远绿，证明不了任何事）");

            // 再验一步：兼容面真的回来时，线上口径就不是 404 了 ⇒ 钉 a 的 404 读数是有意义的
            MockMvc fakeMvc = MockMvcBuilders.webAppContextSetup(fake).build();
            int status = fakeMvc.perform(get("/nacos/v1/cs/configs").accept(MediaType.APPLICATION_JSON))
                    .andReturn().getResponse().getStatus();
            System.out.println("[钉 b] 假上下文里 GET /nacos/v1/cs/configs = " + status
                    + " ⇒ 兼容面回来后钉 a 会变红（期望非 404）");
            assertNotEquals(404, status,
                    "兼容面加回来后请求不应再是 404（若仍是 404，说明钉 a 的判据被别的东西挡住了 = 空转）");
        } finally {
            fake.close();
        }
    }

    /** 假上下文：把兼容面"加回来"，用来证明钉 a 的判据不是空转。 */
    @EnableWebMvc
    static class FakeNacosApp {

        @Bean
        FakeNacosController fakeNacosController() {
            return new FakeNacosController();
        }
    }

    @org.springframework.web.bind.annotation.RestController
    @org.springframework.web.bind.annotation.RequestMapping("/nacos/v1")
    static class FakeNacosController {

        @org.springframework.web.bind.annotation.GetMapping("/cs/configs")
        public String configs() {
            return "[]";
        }
    }

    // ------------------------------------------------------------------ 钉 c

    @Test
    @DisplayName("钉 c（回归）：自有 API /api/config/** 未被误伤 —— 路由仍在，带合法 SSO 登录态时 200")
    void selfApiIsNotBrokenByTheOffline() throws Exception {
        // ① 路由表里自有 API 的映射仍在（真路由，不是被 404 吞掉）
        List<String> apiMappings = mappingsWithPrefix(handlerMapping, "/api/config");
        System.out.println("[钉 c · handler 表] /api/config/** 映射数 = " + apiMappings.size()
                + " ⇒ " + apiMappings);
        assertFalse(apiMappings.isEmpty(), "自有 API 的映射必须还在（下线不能误伤 z-config 自己）");

        // ② 匿名请求仍被 SSO 闸门拦成 401（闸门还在工作，路由没被误删）
        int anonymous = mvc.perform(get("/api/config/groupList").accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getStatus();
        System.out.println("[钉 c] 匿名 GET /api/config/groupList = " + anonymous
                + " ⇒ 被 SSO 闸门拦（说明路由仍在，只是要登录）");
        assertEquals(401, anonymous, "匿名请求自有 API 应仍是 401（闸门口径），不是 404");

        // ③ 带合法 SSO 登录态 ⇒ 200 + 业务信封（自有 API 仍正常）
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("userId", "pin-user");
        claims.put("username", "pin-user");
        String token = jwtUtil.generateToken(claims, 3600);

        MvcResult result = mvc.perform(post("/api/config/listConfig")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new Cookie("sso_token", token))
                        .content("{}"))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        System.out.println("[钉 c] 带 SSO token POST /api/config/listConfig = "
                + result.getResponse().getStatus() + ", body = " + body);

        assertEquals(200, result.getResponse().getStatus(),
                "带合法登录态时自有 API 必须 200（下线不能误伤 z-config 自己的接口）");
        assertTrue(body.contains("\"success\""), "自有 API 应当返回业务信封，实际 body = " + body);
    }

    // ------------------------------------------------------------- 工具方法

    /**
     * 断言：一次真实请求的读数是 <b>404</b>，且<b>不是</b>业务信封，也<b>不是</b>被闸门拦的 401/403/302。
     */
    private void assertNotBusinessEnvelope(MvcResult result, String label) throws Exception {
        MockHttpServletResponse response = result.getResponse();
        int status = response.getStatus();
        String body = response.getContentAsString();

        System.out.println("[钉 a] " + label + " = " + status
                + ", body长度 = " + body.length()
                + ", body = " + (body.length() > 200 ? body.substring(0, 200) + "..." : body));

        assertEquals(404, status, label + " 必须是 404（端点不存在），实际 " + status);
        assertNotEquals(401, status, label + " 不能是 401（那是 SSO 闸门拦的，不是端点不存在）");
        assertNotEquals(403, status, label + " 不能是 403");
        assertNotEquals(302, status, label + " 不能是 302（跳登录页）");
        assertFalse(body.contains("\"success\""), label + " 响应体不得是业务信封，实际 body = " + body);
        assertFalse(body.contains("\"code\""), label + " 响应体不得是业务信封，实际 body = " + body);
        assertFalse(looksLikeSpaIndex(body), label + " 响应体不得是 SPA 首页，实际 body = " + body);
    }

    /** 取出生产里注册的"已退役路径硬闸"过滤器（线上真实过滤器链）。 */
    private Filter retiredPathFilter() {
        @SuppressWarnings("unchecked")
        FilterRegistrationBean<NacosCompatRetiredPathFilter> registration = context.getBean(
                "nacosCompatRetiredPathFilter", FilterRegistrationBean.class);
        return registration.getFilter();
    }

    /** handler 表里所有 /nacos/** 的映射路径（空 = 兼容面没有 handler）。 */
    private static List<String> nacosMappings(RequestMappingHandlerMapping mapping) {
        return mappingsWithPrefix(mapping, "/nacos");
    }

    /** handler 表里所有以 prefix 开头的映射路径。 */
    private static List<String> mappingsWithPrefix(RequestMappingHandlerMapping mapping, String prefix) {
        List<String> found = new ArrayList<String>();
        Map<RequestMappingInfo, org.springframework.web.method.HandlerMethod> handlers = mapping.getHandlerMethods();
        System.out.println("[handler 表] 总映射数 = " + handlers.size()
                + " ; 前 10 条 = " + firstPatterns(handlers, 10));
        for (Map.Entry<RequestMappingInfo, org.springframework.web.method.HandlerMethod> entry : handlers.entrySet()) {
            for (String pattern : patternsOf(entry.getKey())) {
                if (pattern.startsWith(prefix)) {
                    found.add(pattern);
                }
            }
        }
        Collections.sort(found);
        return found;
    }

    /**
     * 取出一条映射的**全部**路径形态。
     *
     * <p>⚠ Spring Boot 2.6+ 默认 {@code spring.mvc.pathmatch.matching-strategy=path_pattern_parser}
     * ⇒ {@link RequestMappingInfo} 里存的是 {@code PathPattern}，
     * {@code getPatternsCondition()} 返回 <b>null</b>（老的那套 String 条件是空的）。
     * 只读老条件会得到"handler 表是空的"这种**假绿** ⇒ 两套条件都要读。
     */
    private static List<String> patternsOf(RequestMappingInfo info) {
        List<String> patterns = new ArrayList<String>();
        if (info.getPatternsCondition() != null) {
            patterns.addAll(info.getPatternsCondition().getPatterns());
        }
        if (info.getPathPatternsCondition() != null) {
            for (org.springframework.web.util.pattern.PathPattern pattern
                    : info.getPathPatternsCondition().getPatterns()) {
                patterns.add(pattern.getPatternString());
            }
        }
        return patterns;
    }

    /** 打印前 n 条映射路径（诊断用：证明 handler 表不是空的）。 */
    private static List<String> firstPatterns(
            Map<RequestMappingInfo, org.springframework.web.method.HandlerMethod> handlers, int n) {
        List<String> sample = new ArrayList<String>();
        for (RequestMappingInfo info : handlers.keySet()) {
            if (sample.size() >= n) {
                break;
            }
            sample.addAll(patternsOf(info));
        }
        return sample;
    }

    /** 响应体看起来是不是 SPA 首页（index.html）。 */
    private static boolean looksLikeSpaIndex(String body) {
        if (body == null) {
            return false;
        }
        String lower = body.toLowerCase();
        return lower.contains("<!doctype html") || lower.contains("<html");
    }

    /** 保留给未来扩展的请求构造入口（避免直接 new 未使用的 builder 告警）。 */
    @SuppressWarnings("unused")
    private static MockHttpServletRequestBuilder jsonGet(String path) {
        return get(path).accept(MediaType.APPLICATION_JSON);
    }
}
