package com.zifang.z.config.web.auth;

import com.zifang.ctc.sso.JwtUtil;
import com.zifang.ctc.sso.config.SsoInterceptor;
import com.zifang.ctc.sso.config.SsoProperties;
import com.zifang.ctc.sso.config.TokenService;
import com.zifang.z.config.web.config.ZConfigWebAutoConfiguration;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * z-config-web 的 SSO 闸门回归测试 (TASK-20261021-012)。
 *
 * <p><b>背景</b>：{@code config.zopc.top/api/config/list} 曾对公网裸奔 200 ——
 * 根因是 z-config-web 只有自带的 {@code TokenInterceptor}，其开关
 * {@code z.config.auth.enabled} 默认 false 且全仓无处配置 ⇒ 拦截器是空操作。
 * 本测试锁住「接入 z-ctc SSO 之后」的四条口径，语义与 opc/ops/oss/ctc 四个域一致：
 * <ol>
 *   <li>未登录 + {@code Accept: application/json} ⇒ <b>401</b>（API 口径）</li>
 *   <li>未登录 + 浏览器口径 ⇒ <b>302</b> 跳登录页（不是"一律拒绝"）</li>
 *   <li>带合法 SSO token ⇒ <b>200</b>（闸门别做成全拒）</li>
 *   <li>exclude 名单里的路径 ⇒ 仍可达</li>
 * </ol>
 *
 * <p><b>为什么用 standaloneSetup 而不是 @SpringBootTest</b>：本模块的完整上下文要连
 * MySQL（{@code ConfigModuleDataSource}），单测里起不来。所以这里只起一个**最小** Spring Boot
 * 上下文 —— 目的只有一个：让 <b>真实的 application.properties</b> 经 Spring Boot 真实的配置加载
 * 装进 {@link SsoProperties}，再照 {@code SsoAutoConfiguration#addInterceptors} 的原样组装
 * {@link SsoInterceptor}。配置口径与拦截器行为都不是测试里编的，是产物里那份。
 *
 * <p>生产行为（公网/内网 401）本轮**未验证**（禁发版），需发版后按人工复测清单复测。
 */
class SsoGateMockMvcTest {

    /**
     * 最小上下文用的应用类：{@code exclude} 掉跟本测试无关、要连库的那几件 ——
     * z-config-web 自己的 {@link ZConfigWebAutoConfiguration}（它 @ComponentScan("com.zifang.z.config")，
     * 会把数据源也扫进来）、Druid / Spring Boot / MyBatis-Plus 的数据源自动配置。
     * <p>SSO 那半边（z-ctc-sso 的 spring.factories 自动装配）保持开启 —— 那正是被测对象。
     */
    @SpringBootApplication(exclude = {
            ZConfigWebAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class
    })
    static class SsoGateTestApp {
    }

    /** 被测接口的替身：路径与真实控制器一致（/api/config/list 等）。 */
    @RestController
    static class FakeApiController {
        @GetMapping("/api/config/list")
        public String list() {
            return "[]";
        }

        @PostMapping("/api/config-auth/login")
        public String login() {
            return "{\"success\":true}";
        }

        @GetMapping("/index.html")
        public String index() {
            return "<html>spa</html>";
        }
    }

    private static ConfigurableApplicationContext context;
    private static MockMvc mockMvc;
    private static SsoProperties ssoProperties;
    private static JwtUtil jwtUtil;

    @BeforeAll
    static void setUp() {
        context = new SpringApplicationBuilder(SsoGateTestApp.class)
                .web(WebApplicationType.NONE)
                .run();

        ssoProperties = context.getBean(SsoProperties.class);
        jwtUtil = context.getBean(JwtUtil.class);

        // 照 SsoAutoConfiguration#addInterceptors 的原样组装（同一份 exclude/intercept 口径）
        SsoInterceptor interceptor = new SsoInterceptor(context.getBean(TokenService.class), ssoProperties);
        interceptor.setExcludedPaths(ssoProperties.getExcludePaths());
        interceptor.setInterceptPaths(ssoProperties.getInterceptPaths());

        mockMvc = MockMvcBuilders.standaloneSetup(new FakeApiController())
                .addInterceptors(interceptor)
                .build();
    }

    @AfterAll
    static void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    @DisplayName("未登录 + Accept: application/json ⇒ 401")
    void unauthenticatedApiRequestIsRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/config/list").accept("application/json"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("未登录 + 浏览器口径 ⇒ 302 跳登录页")
    void unauthenticatedBrowserRequestRedirectsToLogin() throws Exception {
        String location = mockMvc.perform(get("/api/config/list").accept("text/html", "application/xhtml+xml"))
                .andExpect(status().isFound())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        assertNotNull(location, "未登录的浏览器请求应当 302 到登录页");
        assertTrue(location.startsWith(ssoProperties.getLoginUrl()),
                "302 目标应当是 sso.login-url, 实际: " + location);
    }

    @Test
    @DisplayName("带合法 SSO token ⇒ 200（闸门不是一律拒绝）")
    void requestWithValidSsoTokenSucceeds() throws Exception {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "u-1");
        claims.put("username", "sso-user");
        String token = jwtUtil.generateToken(claims, 3600);

        mockMvc.perform(get("/api/config/list")
                        .accept("application/json")
                        .cookie(new Cookie("sso_token", token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("带伪造 token ⇒ 401（不是有 cookie 就放行）")
    void requestWithForgedTokenIsRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/config/list")
                        .accept("application/json")
                        .cookie(new Cookie("sso_token", "not-a-real-jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("exclude 名单里的路径仍可达（/api/config-auth/login）")
    void excludedPathStaysReachable() throws Exception {
        mockMvc.perform(post("/api/config-auth/login").accept("application/json"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("前端静态资源不被闸门拦（/index.html 仍 200）")
    void spaStaticResourceIsNotIntercepted() throws Exception {
        mockMvc.perform(get("/index.html").accept("text/html"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Nacos 兼容面已下线（2026-10-22 T1）：/nacos/v1/** 不再有端点，不是 401/302")
    void retiredNacosCompatSurfaceHasNoEndpointAnymore() throws Exception {
        // ⚠ 2026-10-22 (TASK-20261022-T1 · 用户拍板 T1 = 直接下线)：本用例的**前提变了**。
        //   原断言是「未登录 ⇒ /nacos/v1/cs/configs 401 / 302」—— 那描述的是
        //   「兼容面还在、只是被闸门挡住」。现在兼容面**整段下线**（控制器已删除），
        //   ⇒ 读数必须是「**没有端点**」的形态，而不是「有端点但没权限」。
        //   若哪天这里又读出 401/302，说明兼容面（或类似端点）被加回来了。
        int status = mockMvc.perform(get("/nacos/v1/cs/configs").accept("application/json"))
                .andReturn()
                .getResponse()
                .getStatus();
        System.out.println("[T1 下线] 裸 MVC 上下文里 GET /nacos/v1/cs/configs = " + status
                + " ⇒ 期望 404（端点不存在），不是 401/302（端点存在但没权限）");

        assertEquals(404, status,
                "兼容面已下线 ⇒ 该路径没有任何端点，必须是 404；若读出 401/302 说明端点被加回来了");
    }

    @Test
    @DisplayName("配置自检: application.properties 的 sso 段被真实加载且口径正确")
    void ssoConfigurationIsLoadedFromApplicationProperties() {
        assertTrue(ssoProperties.getInterceptPaths().contains("/api/**"),
                "闸门必须覆盖 /api/**, 实际: " + ssoProperties.getInterceptPaths());

        assertTrue(ssoProperties.getExcludePaths().contains("/api/config-auth/**"),
                "必须放行 z-config 自带登录端点, 实际: " + ssoProperties.getExcludePaths());
        assertTrue(ssoProperties.getExcludePaths().contains("/actuator/**"),
                "必须放行探针, 实际: " + ssoProperties.getExcludePaths());
        // ⚠ 2026-10-22 (TASK-20261022-020 · T2): Nacos 兼容面**不再**被配置豁免。
        //   原断言是「必须放行 nacos 兼容面」—— 那正是裸奔的成因之一（闸门第 ② 条直接放行）。
        assertFalse(ssoProperties.getExcludePaths().contains("/nacos/**"),
                "Nacos 兼容面不得再出现在 sso.exclude-paths 里, 实际: " + ssoProperties.getExcludePaths());
        // ⚠ 2026-10-22 (TASK-20261022-T1 · 用户拍板 T1 = 直接下线)：/nacos/** 也**不在**拦截范围里。
        //   理由与 T2 相反：闸门在 handler 映射**之后**执行，只要 /nacos/** 还在拦截范围，
        //   兼容面路径的落点就永远是 401/302（= 假装下线，还对外宣称"兼容面还在"）。
        //   下线后落点必须是 404（端点不存在）⇒ 防线改由 NacosCompatOfflinePinTest 在构建期承担。
        assertFalse(ssoProperties.getInterceptPaths().contains("/nacos/**"),
                "Nacos 兼容面已下线，不应留在 sso.intercept-paths 里（留着会让落点变成 401/302 而非 404）, 实际: "
                        + ssoProperties.getInterceptPaths());

        assertNotNull(ssoProperties.getLoginUrl());
        assertTrue(ssoProperties.getLoginUrl().startsWith("http"),
                "登录页必须是绝对 URL, 实际: " + ssoProperties.getLoginUrl());

        // 本仓自带的 TokenInterceptor 必须是关着的（它的规则是"token 非空即有效"）
        assertEquals("false", context.getEnvironment().getProperty("z.config.auth.enabled"),
                "z.config.auth.enabled 必须显式为 false");
    }
}
