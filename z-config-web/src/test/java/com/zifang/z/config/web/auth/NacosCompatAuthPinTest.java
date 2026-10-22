package com.zifang.z.config.web.auth;

import com.zifang.ctc.sso.config.SsoInterceptor;
import com.zifang.ctc.sso.config.SsoProperties;
import com.zifang.ctc.sso.config.TokenService;
import com.zifang.z.config.web.config.WebConfig;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TASK-20261022-020 · T8：**反向钉**（为 T2/T3 的修复形态钉住「无效修复」不被误判为已修）。
 *
 * <p><b>为什么要有反向钉</b>：这次修复有两个「看起来改了、其实没改」的坑：
 * <ul>
 *   <li><b>钉 a</b>：只把 {@code /nacos/**} 从 {@code sso.exclude-paths} 删掉，<b>不加</b>进
 *       {@code sso.intercept-paths} ⇒ 闸门第 ③ 条（不在拦截范围 ⇒ 放行）照样放行 ⇒ 端点<b>仍</b>匿名可读。</li>
 *   <li><b>钉 b</b>：加了 {@code sso.intercept-paths}，但 {@code sso.exclude-paths} 里<b>保留</b>
 *       {@code /nacos/**} ⇒ 闸门第 ② 条（配置豁免）先放行 ⇒ 端点<b>仍</b>匿名可读。</li>
 *   <li><b>钉 c</b>：完整修复（加进 intercept-paths + 从 exclude-paths 移除 + 去掉
 *       {@code WebConfig} 里的 TokenInterceptor 豁免）⇒ 匿名 GET <b>非 200</b>。</li>
 * </ul>
 *
 * <p><b>本测试怎么做到「钉」而不是「复述」</b>：它<b>不</b>去读配置文件自己推结论，而是
 * 用**生产同款**的 {@link SsoInterceptor}（z-ctc-sso 的真类）分别按 a/b/c 三种口径装配，
 * 用 MockMvc 打一次<b>真实的匿名请求</b>（{@code GET /nacos/v1/cs/configs}），
 * 读**实际状态码**作为判据：
 * <ul>
 *   <li>钉 a / 钉 b ⇒ 期望读到 <b>200</b>（= 端点仍匿名可读 = 「仍红」）——
 *       若哪天这两个场景居然读到 401/302，说明闸门语义变了，本钉必须重新评估。</li>
 *   <li>钉 c ⇒ 期望读到 <b>401</b>（= 匿名被拦 = 「转绿」）。</li>
 *   <li>另外把<b>仓里真实的 application.properties</b> 装进闸门打一次，
 *       断言它落在钉 c 的口径上（= 修复真的落地在配置里）。</li>
 * </ul>
 * 每次跑完都会在 stdout 打印三行读数（PIN a/b/c 的实测状态码 + 判定），可直接当证据留存。
 *
 * <p><b>第三道门（WebConfig 的 TokenInterceptor 豁免）</b>：{@code InterceptorRegistration}
 * 没暴露 getter，这里用反射读它的私有 {@code excludePatterns}；反射路径不可用时
 * 退回「读源码」判定（两条路都断言 /nacos/** 不在排除名单里）。
 */
class NacosCompatAuthPinTest {

    @SpringBootApplication(exclude = {
            ZConfigWebAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class
    })
    static class PinTestApp {
    }

    /** 被测端点替身：路径与真实控制器一致（/nacos/v1/cs/configs）。 */
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

    @BeforeAll
    static void setUp() {
        context = new SpringApplicationBuilder(PinTestApp.class)
                .web(WebApplicationType.NONE)
                .run();
        ssoProperties = context.getBean(SsoProperties.class);
        tokenService = context.getBean(TokenService.class);
    }

    @AfterAll
    static void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    // ------------------------------------------------------------------ 钉

    @Test
    @DisplayName("钉 a：只从 exclude-paths 删掉 /nacos/**、不加进 intercept-paths ⇒ 端点仍匿名可读（仍红）")
    void pinAOnlyRemovedFromExcludePaths_StillAnonymousReadable() throws Exception {
        List<String> excludes = withoutNacos(ssoProperties.getExcludePaths());
        List<String> intercepts = withoutNacos(ssoProperties.getInterceptPaths());

        int status = probe(excludes, intercepts);
        System.out.println("[PIN a] exclude-paths 去掉 /nacos/**, intercept-paths 不加 /nacos/** "
                + "⇒ 匿名 GET /nacos/v1/cs/configs = " + status + " ⇒ " + verdict(status));

        assertEquals(200, status,
                "钉 a 的前提是「无效修复仍匿名可读」；若这里不是 200，说明闸门语义变了，钉必须重评");
        assertNotEquals(401, status, "钉 a 必须仍红（端点仍匿名可读）");
    }

    @Test
    @DisplayName("钉 b：加了 intercept-paths、但 exclude-paths 里保留 /nacos/** ⇒ 端点仍匿名可读（仍红）")
    void pinBKeptInExcludePaths_StillAnonymousReadable() throws Exception {
        List<String> excludes = withNacos(ssoProperties.getExcludePaths());
        List<String> intercepts = withNacos(ssoProperties.getInterceptPaths());

        int status = probe(excludes, intercepts);
        System.out.println("[PIN b] intercept-paths 含 /nacos/**, exclude-paths 仍保留 /nacos/** "
                + "⇒ 匿名 GET /nacos/v1/cs/configs = " + status + " ⇒ " + verdict(status));

        assertEquals(200, status,
                "钉 b 的前提是「豁免优先 ⇒ 端点仍匿名可读」；若这里不是 200，说明闸门语义变了，钉必须重评");
        assertNotEquals(401, status, "钉 b 必须仍红（端点仍匿名可读）");
    }

    @Test
    @DisplayName("钉 c：完整修复 ⇒ 匿名 GET 非 200（转绿）")
    void pinCFullFix_AnonymousReadBlocked() throws Exception {
        List<String> excludes = withoutNacos(ssoProperties.getExcludePaths());
        List<String> intercepts = withNacos(ssoProperties.getInterceptPaths());

        int status = probe(excludes, intercepts);
        System.out.println("[PIN c] intercept-paths 含 /nacos/**, exclude-paths 已移除 /nacos/** "
                + "⇒ 匿名 GET /nacos/v1/cs/configs = " + status + " ⇒ " + verdict(status));

        assertEquals(401, status, "完整修复后匿名请求必须被 SSO 闸门拒掉（Accept: application/json ⇒ 401）");
    }

    @Test
    @DisplayName("钉 c（落到仓里真实的 application.properties）：实配必须是「已修复」口径")
    void repoConfigurationIsOnTheFixedPosture() throws Exception {
        int status = probe(ssoProperties.getExcludePaths(), ssoProperties.getInterceptPaths());
        System.out.println("[PIN c · 实配] 仓里真实 application.properties "
                + "⇒ 匿名 GET /nacos/v1/cs/configs = " + status + " ⇒ " + verdict(status));

        assertTrue(ssoProperties.getInterceptPaths().contains("/nacos/**"),
                "实配：sso.intercept-paths 必须含 /nacos/**");
        assertFalse(ssoProperties.getExcludePaths().contains("/nacos/**"),
                "实配：sso.exclude-paths 不得含 /nacos/**");
        assertEquals(401, status, "实配：匿名必须被拦");
    }

    @Test
    @DisplayName("第三道门：WebConfig 的 TokenInterceptor 排除名单里不得再有 /nacos/**")
    void tokenInterceptorNoLongerExcludesNacosPath() throws Exception {
        List<String> excludes = tokenInterceptorExcludePatterns();
        System.out.println("[第三道门] TokenInterceptor excludePathPatterns = " + excludes
                + " ⇒ 含 /nacos/** ? " + excludes.contains("/nacos/**"));

        assertFalse(excludes.contains("/nacos/**"),
                "TokenInterceptor 的排除名单里不得再有 /nacos/**（那是第三次放行）");
    }

    // ------------------------------------------------------------- 工具方法

    /**
     * 按给定口径装一次真闸门，用匿名请求打一次真实的端点，返回**实际状态码**。
     */
    private int probe(List<String> excludes, List<String> intercepts) throws Exception {
        SsoInterceptor interceptor = new SsoInterceptor(tokenService, ssoProperties);
        interceptor.setExcludedPaths(new ArrayList<String>(excludes));
        interceptor.setInterceptPaths(new ArrayList<String>(intercepts));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new FakeNacosController())
                .addInterceptors(interceptor)
                .build();

        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/nacos/v1/cs/configs").accept("application/json"))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    /** 读出 WebConfig 注册 TokenInterceptor 时的 excludePathPatterns。 */
    @SuppressWarnings("unchecked")
    private List<String> tokenInterceptorExcludePatterns() throws Exception {
        CapturingRegistry registry = new CapturingRegistry();
        // WebConfig 只用到 registry；tokenInterceptor 字段是 @Resource 注入的，这里塞一个
        // 空的实例（InterceptorRegistry.addInterceptor 不接受 null）
        WebConfig webConfig = new WebConfig();
        try {
            Field field = WebConfig.class.getDeclaredField("tokenInterceptor");
            field.setAccessible(true);
            field.set(webConfig, new com.zifang.z.config.web.config.TokenInterceptor());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("读不到 WebConfig#tokenInterceptor 字段", e);
        }
        webConfig.addInterceptors(registry);
        InterceptorRegistration registration = registry.captured;
        assertTrue(registration != null, "WebConfig 必须注册过拦截器");

        try {
            Field field = InterceptorRegistration.class.getDeclaredField("excludePatterns");
            field.setAccessible(true);
            Object value = field.get(registration);
            if (value instanceof List) {
                return new ArrayList<String>((List<String>) value);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 落到源码判定（Spring 换版本时兜底）
        }

        File source = new File("src/main/java/com/zifang/z/config/web/config/WebConfig.java");
        assertTrue(source.exists(), "读不到 WebConfig 源码: " + source.getAbsolutePath());
        String text = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        List<String> found = new ArrayList<String>();
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("\"") && trimmed.contains("/**")) {
                found.add(trimmed.replaceAll("[\",]", ""));
            }
        }
        return found;
    }

    private static List<String> withNacos(List<String> base) {
        List<String> out = new ArrayList<String>(base);
        if (!out.contains("/nacos/**")) {
            out.add("/nacos/**");
        }
        return out;
    }

    private static List<String> withoutNacos(List<String> base) {
        List<String> out = new ArrayList<String>();
        for (String item : base) {
            if (!"/nacos/**".equals(item)) {
                out.add(item);
            }
        }
        return out;
    }

    private static String verdict(int status) {
        if (status == 200) {
            return "端点匿名可读 ⇒ 仍红（RED）";
        }
        if (status == 401 || status == 403) {
            return "匿名被拒 ⇒ 转绿（GREEN）";
        }
        if (status == 302) {
            return "跳转登录页（匿名拿不到内容）⇒ 转绿（GREEN）";
        }
        return "非 200（需人工判读）";
    }

    /** 截获 WebConfig 注册的 InterceptorRegistration。 */
    static class CapturingRegistry extends InterceptorRegistry {
        private InterceptorRegistration captured;

        @Override
        public InterceptorRegistration addInterceptor(HandlerInterceptor interceptor) {
            captured = super.addInterceptor(interceptor);
            return captured;
        }
    }
}
