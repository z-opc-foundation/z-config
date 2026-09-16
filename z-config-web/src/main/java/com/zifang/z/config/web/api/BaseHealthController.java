package com.zifang.z.config.web.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 基础健康检查控制器.
 * <p>
 * API 基础路径: /api/actuator
 * 所属模块: z-config-web
 * 鉴权: 无需鉴权,用于监控系统存活探测
 *
 * <p>主要端点:
 * <ul>
 *   <li>GET /health — 系统健康状态探测</li>
 * </ul>
 */
@RestController
@RequestMapping("/actuator")
@Tag(name = "10000_监控端点")

public class BaseHealthController {

    /**
     * 返回系统健康状态.
     *
     * @return 固定返回字符串 "UP",表示当前服务存活
     */
    @GetMapping("/health")
    @Operation(summary = "10000_健康检查")

    public String health() {
        return "UP";
    }
}
