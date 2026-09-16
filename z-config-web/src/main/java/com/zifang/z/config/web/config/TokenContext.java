package com.zifang.z.config.web.config;

/**
 * Token上下文：存储当前请求的Token和用户信息
 */
public class TokenContext {
    // 私有静态ThreadLocal，存储Token字符串
    private static final ThreadLocal<String> TOKEN_HOLDER = new ThreadLocal<>();
    // 存储解析后的用户信息（可根据实际需求扩展，如用户ID、用户名）
    private static final ThreadLocal<UserInfo> USER_INFO_HOLDER = new ThreadLocal<>();

    // 获取Token（业务层可直接调用）
    public static String getToken() {
        return TOKEN_HOLDER.get();
    }

    // 设置Token
    public static void setToken(String token) {
        TOKEN_HOLDER.set(token);
    }

    // 获取用户信息（业务层可直接调用）
    public static UserInfo getUserInfo() {
        return USER_INFO_HOLDER.get();
    }

    // 设置用户信息（Token解析后存入）
    public static void setUserInfo(UserInfo userInfo) {
        USER_INFO_HOLDER.set(userInfo);
    }

    // 清除上下文（必须在请求结束后调用，避免内存泄漏）
    public static void clear() {
        TOKEN_HOLDER.remove();
        USER_INFO_HOLDER.remove();
    }

    // 内部类：用户信息封装（根据实际需求扩展字段）
    public static class UserInfo {
        private Long userId;
        private String username;
        // 其他字段：如角色、权限等

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
}