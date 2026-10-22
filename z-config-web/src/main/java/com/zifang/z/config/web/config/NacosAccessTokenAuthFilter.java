package com.zifang.z.config.web.config;

import com.zifang.ctc.sso.JwtUtil;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Nacos 兼容面的 accessToken 闸门（T1 选项①的**预备件**，默认**不注册**）。
 *
 * <p><b>为什么需要它</b>：修掉「三道放行门」之后，{@code /nacos/**} 被收进
 * {@code sso.intercept-paths} ⇒ 匿名请求一律 401/302。但真 Nacos 客户端是用
 * <b>查询参数 accessToken</b> 鉴权的，它不会带 SSO Cookie ⇒ 兼容面在选项①下需要
 * 一条「拿 accessToken 换通行」的通道，而 z-ctc 的 {@code SsoInterceptor}
 * （本仓不可改，见 TASK-20261022-020 的授权边界）只认 Cookie / Authorization 头。
 *
 * <p><b>做法</b>：本过滤器注册在 Servlet 过滤器链上 ⇒ <b>早于</b> Spring MVC 拦截器链执行：
 * <ol>
 *   <li>命中 {@code /nacos/**} 且 {@code accessToken} 校验通过 ⇒ 用**同一个** JwtUtil 实例
 *       （与 SSO 闸门验签同一把密钥）签一枚**短命** JWT，
 *       以 {@code Authorization: Bearer &lt;jwt&gt;} 的形式注入**当次请求**，
 *       于是 z-ctc 的 SSO 闸门把这次请求认成已登录 ⇒ 兼容面可用。</li>
 *   <li>{@code accessToken} 缺失或不匹配 ⇒ 直接 403 短响应，<b>不进**业务链</b>。</li>
 * </ol>
 *
 * <p><b>安全边界</b>：
 * <ul>
 *   <li>签出来的 JWT <b>只存在于当次请求</b>，不写回响应、不落 Cookie ⇒ 客户端拿不到会话。</li>
 *   <li>本过滤器只在 {@code nacos.compat.access-token.enabled=true} 时注册（T1 未拍板 ⇒ 默认关闭）。</li>
 *   <li>启用时若 secret 为空 ⇒ 启动阶段 fail fast（见 {@link NacosCompatAuthConfig}）。</li>
 * </ul>
 *
 * @see NacosAccessTokenVerifier
 * @see NacosCompatAuthConfig
 */
public class NacosAccessTokenAuthFilter implements Filter {

    /** 兼容面路径前缀（本过滤器只管这一段）。 */
    private static final String NACOS_PATH_PREFIX = "/nacos/";

    /** 桥接出来的会话主体名（审计口径：能看出这条请求走的是 accessToken 通道）。 */
    private static final String COMPAT_SUBJECT = "nacos-compat";

    private final NacosAccessTokenVerifier verifier;
    private final JwtUtil jwtUtil;
    private final int ttlSeconds;

    public NacosAccessTokenAuthFilter(NacosAccessTokenVerifier verifier, JwtUtil jwtUtil, int ttlSeconds) {
        this.verifier = verifier;
        this.jwtUtil = jwtUtil;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        if (!request.getRequestURI().startsWith(NACOS_PATH_PREFIX)) {
            chain.doFilter(req, resp);
            return;
        }

        String presented = request.getParameter("accessToken");
        if (!verifier.isValid(presented)) {
            // 与真 Nacos 的失败形态保持一致的错误信封；**不进业务链** ⇒ 没有任何副作用
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"invalid accessToken\",\"data\":null}");
            return;
        }

        chain.doFilter(withSsoAuthorization(request), response);
    }

    /**
     * 把当次请求包装成「带合法 SSO Authorization 头」的请求，让 z-ctc 的闸门放行。
     * <p>只覆盖 {@code Authorization} 头，其余方法原样透传。
     */
    private HttpServletRequest withSsoAuthorization(HttpServletRequest request) {
        final String authorization = "Bearer " + mintToken();
        return new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if ("Authorization".equalsIgnoreCase(name)) {
                    return authorization;
                }
                return super.getHeader(name);
            }
        };
    }

    private String mintToken() {
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("userId", COMPAT_SUBJECT);
        claims.put("username", COMPAT_SUBJECT);
        return jwtUtil.generateToken(claims, ttlSeconds);
    }
}
