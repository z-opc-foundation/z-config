package com.zifang.z.config.web.config;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 已退役路径硬闸：Nacos 兼容面前缀 {@code /nacos/**}（TASK-20261022-T1 · 用户 2026-10-22 拍板 T1 = ② 直接下线）。
 *
 * <p><b>它是什么</b>：一个 Servlet 过滤器，对 {@code /nacos} 与 {@code /nacos/**} 的**任何**请求
 * 直接回 <b>404</b>，**不进** Spring MVC 的业务链。它**不是**一个端点、不是一个控制器、
 * 也不注册任何 {@code @RequestMapping} ⇒ Spring 的 handler 表里依然**没有**兼容面。
 *
 * <p><b>为什么需要它（为什么不靠"删掉控制器"就完事）</b>：兼容面的控制器已整个删除，
 * 请求会落到 {@link WebConfig} 注册的 {@code /**} 静态资源处理器上，而那个处理器：
 * <ul>
 *   <li>GET/HEAD ⇒ 资源不存在 ⇒ <b>404</b>（这一层已由 WebConfig 的 {@code nacos/} 回落口径保证）；</li>
 *   <li>POST/PUT/DELETE 等其它方法 ⇒ 静态资源处理器<b>只支持 GET/HEAD</b>
 *       ⇒ 回 <b>405 Method Not Allowed</b>。</li>
 * </ul>
 * 也就是说，只删控制器的话，**读**端点是 404、**写**端点是 405 ⇒ 口径不统一，
 * 而 405 这种"路径存在、只是方法不允许"的形态恰恰会让人误以为"这里还挂着个端点" ——
 * 正是本卡要消灭的「假装下线」。本过滤器把整段前缀**统一**成 404。
 *
 * <p><b>它带来的第二道保险</b>：即便将来有人把兼容面控制器加回来（绕过代码评审），
 * 这个过滤器仍会让对外行为停在 404；真正把"加回来"这件事**变成构建失败**的是
 * {@code NacosCompatOfflinePinTest}（断言真实 handler 表里没有 {@code /nacos/**} 映射）。
 * 两层职责不同：过滤器管**线上行为**，钉子管**构建期**。
 *
 * <p><b>为什么不做成"保留端点但返回 404"</b>：那样 Spring 里就**有** handler 了，
 * handler 表里会有 {@code /nacos/**} 的映射 ⇒ "真下线"这句话就是假的。
 * 本过滤器在 MVC 之下，不产生 handler ⇒ 口径一致。
 *
 * @see WebConfig
 */
public class NacosCompatRetiredPathFilter implements Filter {

    /** 已退役前缀（兼容面整段下线）。 */
    private static final String RETIRED_PREFIX = "/nacos";

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        if (isRetiredPath(request.getRequestURI())) {
            // 404 而非 401/403：端点不存在，不需要鉴权。
            // sendError 交给容器/Spring Boot 的错误页机制渲染（本仓 /error 在 sso.exclude-paths 内，不会被闸门再拦一次）。
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        chain.doFilter(req, resp);
    }

    /** 该路径是否属于已退役前缀。 */
    private boolean isRetiredPath(String requestUri) {
        if (requestUri == null) {
            return false;
        }
        return RETIRED_PREFIX.equals(requestUri) || requestUri.startsWith(RETIRED_PREFIX + "/");
    }
}
