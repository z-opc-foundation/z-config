package com.zifang.z.config.web.api;

import com.zifang.util.core.meta.Result;
import com.zifang.z.config.core.domain.service.IZClusterService;
import com.zifang.z.config.core.domain.service.IZConfigInfoService;
import com.zifang.z.config.core.domain.service.IZInstanceService;
import com.zifang.z.config.core.domain.service.IZServiceInfoService;
import com.zifang.z.config.core.server.handler.ServerBusinessHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * 仪表盘统计控制器.
 * <p>
 * API 基础路径: /api/dashboard
 * 所属模块: z-config-web
 * 鉴权: 通过统一网关拦截,要求登录态合法
 *
 * <p>主要端点:
 * <ul>
 *   <li>GET /stats — 获取配置中心核心实体的统计计数</li>
 * </ul>
 */
@Tag(name = "配置中心 - 仪表盘")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Resource
    private IZConfigInfoService configInfoService;

    @Resource
    private IZServiceInfoService serviceInfoService;

    @Resource
    private IZInstanceService instanceService;

    @Resource
    private IZClusterService clusterService;

    /**
     * 获取仪表盘统计指标.
     * <p>
     * 聚合配置项、服务、实例、命名空间(集群)等核心实体的总数,用于首页展示.
     *
     * @return 包含 configCount、serviceCount、instanceCount、namespaceCount 四个键的统一返回结构
     */
    @Operation(summary = "获取仪表盘统计指标")
    @GetMapping("/stats")
    public Result<Map<String, Long>> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("configCount", configInfoService.count());
        stats.put("serviceCount", serviceInfoService.count());
        stats.put("instanceCount", instanceService.count());
        stats.put("namespaceCount", clusterService.count());
        stats.put("activeListeners", (long) ServerBusinessHandler.getActiveListenerCount());
        return Result.success(stats);
    }

    /**
     * 获取当前集群节点信息（对齐 Nacos 的集群管理功能）
     * 返回当前实例的 IP、端口、活跃监听者数量等信息
     */
    @Operation(summary = "获取当前节点信息")
    @GetMapping("/node")
    public Result<Map<String, Object>> getNodeInfo() {
        Map<String, Object> info = new HashMap<>();
        try {
            info.put("ip", InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            info.put("ip", "unknown");
        }
        info.put("port", System.getProperty("server.port", "8080"));
        info.put("activeListeners", ServerBusinessHandler.getActiveListenerCount());
        info.put("status", "UP");
        return Result.success(info);
    }
}
