package com.zifang.z.config.starter.autoconfigure;

import com.zifang.z.config.client.naming.ZNamingService;
import com.zifang.z.config.starter.properties.ZConfigProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Z-Naming 实例自动注册：应用就绪后把 (spring.application.name, 本机 IP, server.port)
 * 注册到 Z-Config server，容器关闭时注销。
 *
 * <p>注册失败只记日志不阻断启动；关闭时注销失败同样只记日志。
 * 已知局限：Z-Naming 当前无实例级心跳探活（healthy 注册时写死 true），
 * 异常退出的实例会残留在服务端，靠调用方/控制台的转发超时兜底。</p>
 */
public class ZNamingAutoRegistrar implements ApplicationListener<WebServerInitializedEvent>, DisposableBean {

    private static final Log log = LogFactory.getLog(ZNamingAutoRegistrar.class);

    private final ZNamingService zNamingService;
    private final ZConfigProperties props;
    private final Environment environment;

    private volatile String serviceName;
    private volatile String ip;
    private volatile int port;
    private volatile boolean registered;

    public ZNamingAutoRegistrar(ZNamingService zNamingService, ZConfigProperties props, Environment environment) {
        this.zNamingService = zNamingService;
        this.props = props;
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        this.port = event.getWebServer().getPort();
        String name = environment.getProperty("spring.application.name");
        if (name == null || name.isBlank()) {
            log.warn("[z-config-naming] 未配置 spring.application.name，跳过实例自动注册");
            return;
        }
        String configuredIp = props.getNaming().getIp();
        String resolvedIp = (configuredIp == null || configuredIp.isBlank())
                ? resolveLocalIp() : configuredIp;
        if (resolvedIp == null) {
            log.warn("[z-config-naming] 无法解析本机非回环 IPv4 地址，跳过实例自动注册");
            return;
        }
        this.serviceName = name;
        this.ip = resolvedIp;
        try {
            zNamingService.registerInstance(serviceName, ip, port);
            registered = true;
            log.info("[z-config-naming] 实例已注册: " + serviceName + " -> " + ip + ":" + port);
        } catch (Exception e) {
            log.error("[z-config-naming] 实例注册失败（不影响应用启动）: " + serviceName
                    + " -> " + ip + ":" + port + ", 原因: " + e.getMessage());
        }
    }

    @Override
    public void destroy() {
        if (!registered) {
            return;
        }
        try {
            zNamingService.deregisterInstance(serviceName, ip, port);
            log.info("[z-config-naming] 实例已注销: " + serviceName + " -> " + ip + ":" + port);
        } catch (Exception e) {
            log.warn("[z-config-naming] 实例注销失败（忽略）: " + serviceName + ", 原因: " + e.getMessage());
        }
    }

    private static String resolveLocalIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[z-config-naming] 解析本机 IP 失败: " + e.getMessage());
        }
        return null;
    }
}
