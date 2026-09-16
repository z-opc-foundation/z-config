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
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.UUID;
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
 *   <li>POST /login — 用户登录,支持内置默认账号与用户表账号</li>
 *   <li>POST /logout — 注销登录,清除 token Cookie</li>
 *   <li>GET /current — 获取当前登录用户信息</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/config-auth")
@Tag(name = "配置中心认证")
public class ConfigAuthController {

    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin";
    private static final int TOKEN_MAX_AGE = 24 * 60 * 60;
    @Autowired
    private IZUsersService usersService;

    /**
     * 用户登录.
     * <p>
     * 先校验内置默认账号,再回退到用户表查询;登录成功后将 token 写入 HttpOnly Cookie 并返回.
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

        if (DEFAULT_USERNAME.equals(username) && DEFAULT_PASSWORD.equals(password)) {
            String token = generateToken();
            setTokenCookie(response, token);
            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setToken(token);
            loginResponse.setUsername(username);
            loginResponse.setNickname("管理员");
            return Result.success(loginResponse);
        }

        ZUsers user = usersService.getById(username);
        if (user == null || !user.getEnabled()) {
            return Result.fail("用户名或密码错误");
        }
        if (!password.equals(user.getPassword())) {
            return Result.fail("用户名或密码错误");
        }

        String token = generateToken();
        setTokenCookie(response, token);
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(token);
        loginResponse.setUsername(username);
        loginResponse.setNickname(user.getUsername());
        return Result.success(loginResponse);
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
