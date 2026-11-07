package com.zifang.z.config.web.api;

import com.zifang.util.core.meta.Result;
import com.zifang.z.config.common.model.auth.LoginRequest;
import com.zifang.z.config.common.model.auth.LoginResponse;
import com.zifang.z.config.core.domain.entity.ZUsers;
import com.zifang.z.config.core.domain.service.IZUsersService;
import com.zifang.z.config.web.config.TokenContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import com.zifang.util.core.lang.RandomUtil;

/**
 * 配置中心专属认证（临时方案，最终应接入 z-ctc）.
 * <p>
 * API 基础路径: /api/config-auth
 * 所属模块: z-config-web
 * 鉴权: 本控制器自身即处理登录与注销,登录态以 Cookie 中的 token 标识
 *
 * <p>主要端点:
 * <ul>
 *   <li>POST /login — 用户登录,校验用户表账号;仅在宿主环境显式注入引导账号时才额外启用内置管理员</li>
 *   <li>POST /logout — 注销登录,清除 token Cookie</li>
 *   <li>GET /current — 获取当前登录用户信息</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/config-auth")
@Tag(name = "配置中心认证")
public class ConfigAuthController {

    private static final int TOKEN_MAX_AGE = 24 * 60 * 60;

    /**
     * 可选内置管理员引导账号 (A0 S5①)。仅当两个值都由宿主环境显式注入且非空时,
     * 登录分支才生效;默认空 ⇒ 分支完全不生效(fail-closed),绝不回退到任何内置默认值。
     * 真值经 application.properties 的 ${ENV:} 占位注入,不落仓库。
     */
    @Value("${zconfig.bootstrap.username:}")
    private String bootstrapUsername;

    @Value("${zconfig.bootstrap.password:}")
    private String bootstrapPassword;

    @Autowired
    private IZUsersService usersService;

    /**
     * 用户登录.
     * <p>
     * 仅当宿主环境同时注入了非空的内置引导账号与口令时,才先做一次常量时间比对;
     * 未配置(默认空)则整段跳过,只剩用户表校验 —— 不存在任何可匿名使用的默认凭据。
     * 登录成功后将 token 写入 HttpOnly Cookie 并返回;响应与日志都不回显口令.
     *
     * @param request  登录请求参数,包含用户名与密码
     * @param response HTTP 响应对象,用于写入 token Cookie
     * @return 登录成功时返回含 token 的响应,失败时返回错误码
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        String username = request.getUsername();
        String password = request.getPassword();

        if (isConfigured(bootstrapUsername) && isConfigured(bootstrapPassword)
                && constantTimeEquals(username, bootstrapUsername)
                && constantTimeEquals(password, bootstrapPassword)) {
            return issueToken(response, username, "管理员");
        }

        ZUsers user = usersService.getById(username);
        if (user == null || !user.getEnabled()) {
            return Result.fail("用户名或密码错误");
        }
        if (!password.equals(user.getPassword())) {
            return Result.fail("用户名或密码错误");
        }
        return issueToken(response, username, user.getUsername());
    }

    /**
     * 用户登出.
     * <p>
     * 清除 token Cookie 并清理当前线程的用户上下文.
     *
     * @param response HTTP 响应对象,用于覆盖失效的 token Cookie
     * @return 成功标识的统一返回结构
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Result<Object> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("token", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        TokenContext.clear();
        return Result.success();
    }

    /**
     * 获取当前登录用户信息.
     *
     * @return 包含当前用户名与昵称的统一返回结构;未登录时返回失败标识
     */
    @GetMapping("/current")
    @Operation(summary = "获取当前用户信息")
    public Result<LoginResponse> getCurrentUser() {
        TokenContext.UserInfo userInfo = TokenContext.getUserInfo();
        if (userInfo == null) {
            return Result.fail("未登录");
        }
        LoginResponse response = new LoginResponse();
        response.setUsername(userInfo.getUsername());
        response.setNickname(userInfo.getUsername());
        return Result.success(response);
    }

    /** 仅当值由环境显式注入且非空白时才视为「已配置」;null / 空串 / 全空白一律视为未配置。 */
    private static boolean isConfigured(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 常量时间比较,避免按字节短路泄露口令前缀;任一侧为 null 直接判不匹配(不放行)。
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    /** 签发本地 UUID token 并写入 HttpOnly Cookie;响应只回显用户名/昵称/token,绝不携带口令。 */
    private Result<LoginResponse> issueToken(HttpServletResponse response, String username, String nickname) {
        String token = generateToken();
        setTokenCookie(response, token);
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(token);
        loginResponse.setUsername(username);
        loginResponse.setNickname(nickname);
        return Result.success(loginResponse);
    }

    /**
     * 生成随机的登录令牌.
     *
     * @return 去除连字符的 UUID 字符串
     */
    private String generateToken() {
        return RandomUtil.uuidCompact();
    }

    /**
     * 将登录令牌写入 HttpOnly Cookie.
     *
     * @param response HTTP 响应对象
     * @param token    登录令牌
     */
    private void setTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("token", token);
        cookie.setMaxAge(TOKEN_MAX_AGE);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }
}
