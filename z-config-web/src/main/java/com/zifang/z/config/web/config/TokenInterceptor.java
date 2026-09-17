package com.zifang.z.config.web.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Token 鉴权拦截器（对齐 Nacos 的 JWT Token 认证机制）
 * <p>
 * 从请求 Cookie 中提取 token，验证有效性后设置用户上下文。
 * 通过 z.config.auth.enabled 配置开关控制是否启用（默认关闭以保持向后兼容）。
 * <p>
 * 启用后，除公开接口外的所有请求都需要携带有效 token。
 */
@Component("configTokenInterceptor")
public class TokenInterceptor implements HandlerInterceptor {

    private static final Logger log = LogManager.getLogger(TokenInterceptor.class);

    /**
     * 是否启用认证（默认关闭，保持向后兼容）
     * 生产环境建议开启：z.config.auth.enabled=true
     */
    @Value("${z.config.auth.enabled:false}")
    private boolean authEnabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // 认证未启用时直接放行
        if (!authEnabled) {
            setAnonymousContext(request);
            return true;
        }

        // 从 Cookie 中提取 token
        String token = extractTokenFromCookie(request);

        // 也支持从 Authorization Header 提取（Bearer token）
        if (token == null || token.isEmpty()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        // 验证 token 有效性
        if (token == null || token.isEmpty()) {
            log.warn("请求未携带 token: {}", uri);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"未登录或 token 已过期\"}");
            return false;
        }

        // TODO: 生产环境应从 Redis 或数据库验证 token 有效性
        // 当前实现：token 非空即视为有效（UUID token 无过期机制）
        // 后续应接入 z-ctc 统一认证

        // 解析用户名（token 格式约定：前缀标识用户，或通过 token->username 映射）
        String username = resolveUsername(token, request);

        // 设置用户上下文到 ThreadLocal（供业务层使用）
        TokenContext.setToken(token);
        TokenContext.UserInfo userInfo = new TokenContext.UserInfo();
        userInfo.setUsername(username);
        TokenContext.setUserInfo(userInfo);

        // 设置请求属性（供 ConfigServiceImpl 审计日志使用）
        request.setAttribute("currentUser", username);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理 ThreadLocal，避免内存泄漏
        TokenContext.clear();
    }

    /**
     * 从 Cookie 中提取 token
     */
    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 从 token 解析用户名
     * TODO: 后续接入 z-ctc 后，应通过 token 解析 JWT 获取用户名
     */
    private String resolveUsername(String token, HttpServletRequest request) {
        // 当前简单实现：使用 token 前 8 位作为标识
        // 后续应从 TokenContext 或 Redis 中查询 token 对应的用户名
        TokenContext.UserInfo existing = TokenContext.getUserInfo();
        if (existing != null && existing.getUsername() != null) {
            return existing.getUsername();
        }
        return "user:" + token.substring(0, Math.min(token.length(), 8));
    }

    /**
     * 认证未启用时设置匿名用户上下文
     */
    private void setAnonymousContext(HttpServletRequest request) {
        request.setAttribute("currentUser", "anonymous");
        TokenContext.UserInfo userInfo = new TokenContext.UserInfo();
        userInfo.setUsername("anonymous");
        TokenContext.setUserInfo(userInfo);
    }
}
