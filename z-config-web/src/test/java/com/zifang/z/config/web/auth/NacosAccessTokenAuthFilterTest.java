package com.zifang.z.config.web.auth;

import com.zifang.ctc.sso.JwtUtil;
import com.zifang.ctc.sso.config.SsoInterceptor;
import com.zifang.ctc.sso.config.SsoProperties;
import com.zifang.ctc.sso.config.TokenService;
import com.zifang.z.config.web.config.NacosAccessTokenAuthFilter;
import com.zifang.z.config.web.config.NacosAccessTokenVerifier;
import com.zifang.z.config.web.config.NacosCompatProperties;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * TASK-20261022-020 · T4 选项①（保留兼容面 + 强制 accessToken）的**预备件**回归测试。
 *
 * <p><b>本测试证明什么</b>（T1 未拍板 ⇒ 这套能力默认关闭，测试只在显式打开时验证）：
 * <ol>
 *   <li>accessToken 校验器：未启用 ⇒ 不施加要求；启用 ⇒ 只有**完全一致**的 token 才放行；
 *       启用却没配 secret ⇒ <b>一律拒绝</b>（fail closed，不允许空口令）。</li>
 *   <li>accessToken 闸门过滤器：token 缺失/错误 ⇒ 403 且**不进业务链**（无任何副作用）；
 *       token 正确 ⇒ 放行，且被桥接成 z-ctc 的 SSO 闸门认得的请求（端到端跑一遍真闸门验证）。</li>
 * </ol>
 *
 * <p><b>为什么第 ② 条要用真闸门验</b>：过滤器把 accessToken 换成了「当次请求的
 * {@code Authorization: Bearer <jwt>}」，如果桥接用的 JwtUtil 与 SSO 验签不是同一把密钥，
 * 兼容面就会「认证通过但被闸门挡住」。这里用**上下文里同一个 JwtUtil bean** 签发，
 * 再用**同一个 SsoInterceptor** 放行请求 ⇒ 两端对得上才算数。
 */
class NacosAccessTokenAuthFilterTest {

    private static final String SECRET = "test-only-access-token-secret";

    @SpringBootApplication(exclude = {
            ZConfigWebAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class
    })
    static class AccessTokenTestApp {
    }

    @RestController
    @RequestMapping("/nacos/v1")
    static class FakeNacosController {
        @GetMapping("/cs/configs")
        public String configs() {
            return "[]";
        }
    }

    private static ConfigurableApplicationContext context;
    private static SsoProperties ssoProperties;
    private static TokenService tokenService;
    private static JwtUtil jwtUtil;

    @BeforeAll
    static void setUp() {
        context = new SpringApplicationBuilder(AccessTokenTestApp.class)
                .web(WebApplicationType.NONE)
                .run();
        ssoProperties = context.getBean(SsoProperties.class);
        tokenService = context.getBean(TokenService.class);
        jwtUtil = context.getBean(JwtUtil.class);
    }

    @AfterAll
    static void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    // ----------------------------------------------------------- 校验器语义

    @Test
    @DisplayName("校验器：未启用 ⇒ 不施加任何要求（保护交给 SSO 闸门）")
    void verifierIsPermissiveWhenDisabled() {
        NacosAccessTokenVerifier verifier = new NacosAccessTokenVerifier(props(false, SECRET));
        assertFalse(verifier.isEnabled());
        assertTrue(verifier.isValid(null));
        assertTrue(verifier.isValid("anything"));
    }

    @Test
    @DisplayName("校验器：启用 ⇒ 只有完全一致的 token 才放行")
    void verifierRequiresExactMatchWhenEnabled() {
        NacosAccessTokenVerifier verifier = new NacosAccessTokenVerifier(props(true, SECRET));
        assertTrue(verifier.isEnabled());
        assertTrue(verifier.isValid(SECRET));
        assertFalse(verifier.isValid(null));
        assertFalse(verifier.isValid(""));
        assertFalse(verifier.isValid(SECRET + "x"));
        assertFalse(verifier.isValid("wrong"));
    }

    @Test
    @DisplayName("校验器：启用却没配 secret ⇒ 一律拒绝（fail closed，不允许空口令）")
    void verifierFailsClosedWithoutSecret() {
        NacosAccessTokenVerifier verifier = new NacosAccessTokenVerifier(props(true, "  "));
        assertFalse(verifier.isValid(""));
        assertFalse(verifier.isValid("anything"));
    }

    // ------------------------------------------------------------- 过滤器

    @Test
    @DisplayName("过滤器：accessToken 缺失 ⇒ 403 且不进业务链")
    void filterRejectsMissingAccessToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/nacos/v1/cs/configs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainCalled = {false};

        filter(props(true, SECRET)).doFilter(request, response, (req, resp) -> chainCalled[0] = true);

        assertEquals(403, response.getStatus());
        assertFalse(chainCalled[0], "拒绝时不得进入业务链（否则写端点会有副作用）");
    }

    @Test
    @DisplayName("过滤器：accessToken 错误 ⇒ 403 且不进业务链")
    void filterRejectsWrongAccessToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/nacos/v1/cs/configs");
        request.setParameter("accessToken", "wrong");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainCalled = {false};

        filter(props(true, SECRET)).doFilter(request, response, (req, resp) -> chainCalled[0] = true);

        assertEquals(403, response.getStatus());
        assertFalse(chainCalled[0], "拒绝时不得进入业务链");
    }

    @Test
    @DisplayName("过滤器：非 /nacos/** 路径不受影响（直接透传）")
    void filterPassesThroughOtherPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/config/list");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainCalled = {false};

        filter(props(true, SECRET)).doFilter(request, response, (req, resp) -> chainCalled[0] = true);

        assertEquals(200, response.getStatus());
        assertTrue(chainCalled[0], "非兼容面路径必须透传");
    }

    @Test
    @DisplayName("端到端：accessToken 正确 ⇒ 兼容面经 SSO 闸门放行（200）；没有 token ⇒ 闸门 401")
    void endToEndAccessTokenBridgesToSsoGate() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new FakeNacosController())
                .addFilters(filter(props(true, SECRET)))
                .addInterceptors(ssoInterceptor())
                .build();

        // ① 带正确 accessToken ⇒ 过滤器桥接 ⇒ SSO 闸门放行 ⇒ 端点 200
        mockMvc.perform(get("/nacos/v1/cs/configs")
                        .accept("application/json")
                        .param("accessToken", SECRET))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        // ② 不带 accessToken ⇒ 过滤器先拒 ⇒ 403
        mockMvc.perform(get("/nacos/v1/cs/configs").accept("application/json"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());

        // ③ 只带 Cookie 不带 accessToken ⇒ 过滤器仍拒（选项① 的口径：accessToken 是唯一入口）
        mockMvc.perform(get("/nacos/v1/cs/configs")
                        .accept("application/json")
                        .cookie(new javax.servlet.http.Cookie("sso_token", "whatever")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
    }

    // ------------------------------------------------------------- 工具方法

    private NacosCompatProperties props(boolean enabled, String secret) {
        NacosCompatProperties properties = new NacosCompatProperties();
        properties.setAccessTokenEnabled(enabled);
        properties.setAccessTokenSecret(secret);
        properties.setAccessTokenTtlSeconds(60);
        return properties;
    }

    private NacosAccessTokenAuthFilter filter(NacosCompatProperties properties) {
        return new NacosAccessTokenAuthFilter(
                new NacosAccessTokenVerifier(properties), jwtUtil, properties.getAccessTokenTtlSeconds());
    }

    private SsoInterceptor ssoInterceptor() {
        SsoInterceptor interceptor = new SsoInterceptor(tokenService, ssoProperties);
        List<String> excludes = new ArrayList<String>();
        for (String item : ssoProperties.getExcludePaths()) {
            if (!"/nacos/**".equals(item)) {
                excludes.add(item);
            }
        }
        interceptor.setExcludedPaths(excludes);
        interceptor.setInterceptPaths(ssoProperties.getInterceptPaths());
        return interceptor;
    }
}
