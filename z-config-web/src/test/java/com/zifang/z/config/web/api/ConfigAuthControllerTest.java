package com.zifang.z.config.web.api;

import com.zifang.util.core.meta.Result;
import com.zifang.z.config.common.model.auth.LoginRequest;
import com.zifang.z.config.common.model.auth.LoginResponse;
import com.zifang.z.config.core.domain.service.IZUsersService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ConfigAuthController 内置管理员引导账号 (A0 S5①) 回归测试.
 *
 * <p>修复前 {@code login()} 用两个 {@code private static final} 常量做直比放行,
 * 任何人都能用内置默认凭据拿 token。现在默认凭据分支改为「环境变量注入的可选引导账号」,
 * 未注入时整段不可达 (fail-closed)。
 *
 * <p>关于「原默认凭据」的覆盖口径: 修复后代码里已不存在任何内置账号/口令常量,
 * 因此历史上的默认账号(无论其具体值是什么)都不可能被内置分支命中,
 * 只会落入「用户表查无此人」分支 ⇒ 用例 ① 用「用户表查无此人」的输入覆盖该回归点,
 * 并断言确实回退到了用户表校验、没有提前签发 token; 用例 ④ 用源码守卫钉死
 * 「代码里不存在任何硬编码默认账号/口令常量」。全程不在测试或报告里写出任何真实凭据值。
 */
class ConfigAuthControllerTest {

    // 测试夹具: 均为本测试自造的示例串, 不是任何真实凭据。
    private static final String BOOTSTRAP_USER = "bootstrap-admin";
    private static final String BOOTSTRAP_PASS = "unit-test-only-pass";

    private ConfigAuthController controller;
    private IZUsersService usersService;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        controller = new ConfigAuthController();
        usersService = mock(IZUsersService.class);
        response = mock(HttpServletResponse.class);
        ReflectionTestUtils.setField(controller, "usersService", usersService);
        // 默认等价于 Spring 解析 ${ENV:} 后的空值: 未注入任何引导账号。
        setBootstrap("", "");
    }

    private void setBootstrap(String u, String p) {
        ReflectionTestUtils.setField(controller, "bootstrapUsername", u);
        ReflectionTestUtils.setField(controller, "bootstrapPassword", p);
    }

    private static LoginRequest req(String u, String p) {
        LoginRequest r = new LoginRequest();
        r.setUsername(u);
        r.setPassword(p);
        return r;
    }

    @Test
    @DisplayName("① 未配置内置账号时, 原内置默认凭据落入「用户表查无此人」必须登录失败(fail-closed)")
    void formerBuiltinCredentialsFailWhenBootstrapNotConfigured() {
        Result<LoginResponse> r = controller.login(
                req("former-builtin-account", "former-builtin-pass"), response);

        assertFalse(r.isSuccess(), "未配置引导账号时不得签发 token");
        assertNull(r.getData(), "失败响应不应携带登录数据");
        verify(usersService).getById("former-builtin-account");
        verify(response, never()).addCookie(any(Cookie.class));
    }

    @Test
    @DisplayName("①b 半配置(只给用户名 / 只给口令)同样 fail-closed")
    void halfConfiguredBootstrapStillFailsClosed() {
        setBootstrap(BOOTSTRAP_USER, "");
        assertFalse(controller.login(req(BOOTSTRAP_USER, BOOTSTRAP_PASS), response).isSuccess(),
                "只配用户名不得启用内置账号");

        setBootstrap("", BOOTSTRAP_PASS);
        assertFalse(controller.login(req(BOOTSTRAP_USER, BOOTSTRAP_PASS), response).isSuccess(),
                "只配口令不得启用内置账号");
    }

    @Test
    @DisplayName("② 配置了内置账号且账号口令正确时可登录, 响应不回显口令")
    void configuredBootstrapCanLoginAndNeverEchoesPassword() {
        setBootstrap(BOOTSTRAP_USER, BOOTSTRAP_PASS);

        Result<LoginResponse> r = controller.login(req(BOOTSTRAP_USER, BOOTSTRAP_PASS), response);

        assertTrue(r.isSuccess(), "显式注入引导账号后应可登录");
        assertNotNull(r.getData());
        assertNotNull(r.getData().getToken(), "应签发 token");
        assertFalse(r.getData().getToken().isBlank());
        assertEquals(BOOTSTRAP_USER, r.getData().getUsername());
        // 口令不得出现在响应的任何字段
        assertNotEquals(BOOTSTRAP_PASS, r.getData().getToken());
        assertNotEquals(BOOTSTRAP_PASS, r.getData().getUsername());
        assertNotEquals(BOOTSTRAP_PASS, r.getData().getNickname());
        verify(response).addCookie(any(Cookie.class));
        verifyNoInteractions(usersService);   // 命中引导分支, 不回退用户表
    }

    @Test
    @DisplayName("③ 配置了内置账号但口令错误时必须失败, 且不得回退放行")
    void configuredBootstrapWrongPasswordFails() {
        setBootstrap(BOOTSTRAP_USER, BOOTSTRAP_PASS);

        Result<LoginResponse> r = controller.login(req(BOOTSTRAP_USER, "wrong-pass"), response);

        assertFalse(r.isSuccess(), "口令错误必须失败");
        assertNull(r.getData());
        verify(usersService).getById(BOOTSTRAP_USER);   // 回退到用户表, 查无此人 → 失败
        verify(response, never()).addCookie(any(Cookie.class));
    }

    @Test
    @DisplayName("④ 源码守卫: 不存在任何硬编码默认账号/口令常量, 也不存在字符串字面量直比")
    void noHardcodedDefaultCredentialsInSource() throws Exception {
        String src = Files.readString(controllerSource(), StandardCharsets.UTF_8);

        assertFalse(src.contains("DEFAULT_USERNAME"), "不得再出现 DEFAULT_USERNAME 常量");
        assertFalse(src.contains("DEFAULT_PASSWORD"), "不得再出现 DEFAULT_PASSWORD 常量");
        assertFalse(Pattern.compile(
                "private\\s+static\\s+final\\s+String\\s+(DEFAULT_\\w+|\\w*USERNAME\\w*|\\w*PASSWORD\\w*)\\s*=")
                .matcher(src).find(), "不得声明任何账号/口令类 static final 常量");
        assertFalse(Pattern.compile("\\\"[^\\\"]*\\\"\\s*\\.equals\\s*\\(\\s*(username|password)")
                .matcher(src).find(), "不得再用字符串字面量直比登录口令");
    }

    @Test
    @DisplayName("④b 配置自检: bootstrap 键是 ${ENV:} 空默认占位, 真值不落仓库")
    void bootstrapPropertiesAreEnvPlaceholdersOnly() throws Exception {
        String props = Files.readString(applicationProperties(), StandardCharsets.UTF_8);

        assertTrue(Pattern.compile("zconfig\\.bootstrap\\.username=\\$\\{ZCONFIG_BOOTSTRAP_USERNAME:\\}\\s*$",
                Pattern.MULTILINE).matcher(props).find(), "username 必须是 ${ENV:} 空默认占位");
        assertTrue(Pattern.compile("zconfig\\.bootstrap\\.password=\\$\\{ZCONFIG_BOOTSTRAP_PASSWORD:\\}\\s*$",
                Pattern.MULTILINE).matcher(props).find(), "password 必须是 ${ENV:} 空默认占位");
    }

    private static Path controllerSource() {
        return locate("src/main/java/com/zifang/z/config/web/api/ConfigAuthController.java");
    }

    private static Path applicationProperties() {
        return locate("src/main/resources/application.properties");
    }

    /** 从 user.dir 起向上找模块内的相对文件(surefire 默认 cwd = 模块 basedir)。 */
    private static Path locate(String rel) {
        Path p = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        for (Path cur = p; cur != null; cur = cur.getParent()) {
            Path c = cur.resolve(rel);
            if (Files.isRegularFile(c)) {
                return c;
            }
        }
        throw new IllegalStateException("找不到 " + rel + "(起始 " + p + ")");
    }
}
