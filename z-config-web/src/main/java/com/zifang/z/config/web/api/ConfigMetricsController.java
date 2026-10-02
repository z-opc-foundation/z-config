package com.zifang.z.config.web.api;

import com.zifang.util.core.meta.Result;
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
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 监控指标端点（对齐 Nacos 的监控系统）
 * <p>
 * 提供 Prometheus 兼容的指标数据，可用于 Grafana 等监控平台
 */
@RestController
@RequestMapping("/api/metrics")
@Tag(name = "012_监控指标")
public class ConfigMetricsController {

    @Resource
    private IZConfigInfoService configInfoService;

    @Resource
    private IZServiceInfoService serviceInfoService;

    @Resource
    private IZInstanceService instanceService;

    /**
     * 获取核心监控指标（Prometheus 兼容格式的简化版）
     */
    @GetMapping("/core")
    @Operation(summary = "001_获取核心监控指标")
    public Result<Map<String, Object>> coreMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        // 业务指标
        metrics.put("z_config_total", configInfoService.count());
        metrics.put("z_service_total", serviceInfoService.count());
        metrics.put("z_instance_total", instanceService.count());
        metrics.put("z_active_listeners", ServerBusinessHandler.getActiveListenerCount());

        // JVM 指标
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        Runtime runtime = Runtime.getRuntime();

        metrics.put("jvm_memory_used_bytes", memoryBean.getHeapMemoryUsage().getUsed());
        metrics.put("jvm_memory_max_bytes", memoryBean.getHeapMemoryUsage().getMax());
        metrics.put("jvm_threads_current", threadBean.getThreadCount());
        metrics.put("jvm_threads_daemon", threadBean.getDaemonThreadCount());
        metrics.put("jvm_memory_free_bytes", runtime.freeMemory());
        metrics.put("jvm_memory_total_bytes", runtime.totalMemory());

        // 运行时间
        metrics.put("process_uptime_seconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);

        return Result.success(metrics);
    }

    /**
     * Prometheus 文本格式指标（可直接对接 Prometheus 拉取）
     */
    @GetMapping(value = "/prometheus", produces = "text/plain")
    @Operation(summary = "002_Prometheus格式指标")
    public String prometheusMetrics() {
        StringBuilder sb = new StringBuilder();

        sb.append("# HELP z_config_total Total number of configurations\n");
        sb.append("# TYPE z_config_total gauge\n");
        sb.append("z_config_total ").append(configInfoService.count()).append("\n\n");

        sb.append("# HELP z_service_total Total number of services\n");
        sb.append("# TYPE z_service_total gauge\n");
        sb.append("z_service_total ").append(serviceInfoService.count()).append("\n\n");

        sb.append("# HELP z_instance_total Total number of instances\n");
        sb.append("# TYPE z_instance_total gauge\n");
        sb.append("z_instance_total ").append(instanceService.count()).append("\n\n");

        sb.append("# HELP z_active_listeners Active long-poll listeners\n");
        sb.append("# TYPE z_active_listeners gauge\n");
        sb.append("z_active_listeners ").append(ServerBusinessHandler.getActiveListenerCount()).append("\n\n");

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        sb.append("# HELP jvm_memory_used_bytes JVM heap memory used\n");
        sb.append("# TYPE jvm_memory_used_bytes gauge\n");
        sb.append("jvm_memory_used_bytes ").append(memoryBean.getHeapMemoryUsage().getUsed()).append("\n\n");

        sb.append("# HELP jvm_threads_current Current thread count\n");
        sb.append("# TYPE jvm_threads_current gauge\n");
        sb.append("jvm_threads_current ").append(ManagementFactory.getThreadMXBean().getThreadCount()).append("\n");

        return sb.toString();
    }
}
