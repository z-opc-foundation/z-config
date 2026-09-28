package com.zifang.z.config.client.config;

import com.zifang.util.core.lang.primitive.Bytes;
import com.zifang.util.core.meta.Result;
import com.zifang.z.config.client.config.listener.ZConfigListener;
import com.zifang.z.config.client.config.listener.ZConfigServiceListenerManager;
import com.zifang.z.config.client.support.ConfigCallClient;
import com.zifang.z.config.common.Constance;
import com.zifang.z.config.common.model.ConfigKey;
import com.zifang.z.config.common.model.PollResponse;
import com.zifang.z.config.common.model.config.ZConfigQueryRequest;
import com.zifang.z.config.common.model.config.ZConfigSaveRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Config 服务客户端实现
 * <p>
 * 核心改进（对齐 Nacos）：
 * 1. 多 Server 地址故障转移：支持逗号分隔的多个 serverAddr，轮询选取 + 连接失败自动切换
 * 2. 本地文件快照：每次成功获取/推送后写入磁盘，服务不可用时自动降级到本地快照
 * 3. 真实 HTTP 拉取：fetchConfigFromServer 通过 ConfigCallClient 发起 HTTP 请求
 * 4. 三层缓存：内存缓存 -> 本地快照文件 -> 服务端拉取
 */
@SuppressWarnings("deprecation")
public class ZConfigServiceImpl implements ZConfigService {

    private static final Logger log = LogManager.getLogger(ZConfigServiceImpl.class);

    /**
     * 本地快照根目录：~/.zconfig/snapshot/{namespace}/
     */
    private static final String SNAPSHOT_ROOT = System.getProperty("user.home") + "/.zconfig/snapshot";

    private final String nameSpace;

    // 多 Server 地址列表（支持故障转移）
    private final List<String> serverAddrs;
    // 轮询索引，用于在多个 server 之间选取
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    // 本地内存缓存：配置标识 -> 配置内容
    private final Map<ConfigKey, String> configCache = new ConcurrentHashMap<>();
    // 本地内存缓存MD5：配置标识 -> MD5
    private final Map<ConfigKey, String> configMd5Cache = new ConcurrentHashMap<>();
    // 监听器注册表：配置标识 -> 监听器
    private final Map<ConfigKey, ZConfigListener> listeners = new ConcurrentHashMap<>();

    // 每个 server 地址对应的 HTTP 客户端
    private final List<ServerEndpoint> endpoints = new ArrayList<>();

    // 当前选中的 server 索引（使用 AtomicInteger 保证线程安全）
    private final AtomicInteger activeEndpointIndex = new AtomicInteger(0);

    /**
     * Server 端点封装：host + port + ConfigCallClient
     */
    private static class ServerEndpoint {
        final String host;
        final int port;
        final int sidePort; // Netty 长连接端口
        final ConfigCallClient httpClient;

        ServerEndpoint(String host, int port) {
            this.host = host;
            this.port = port;
            this.sidePort = Constance.serveBindPort;
            this.httpClient = new ConfigCallClient(host, port);
        }
    }

    /**
     * 构造方法：支持单个或多个 server 地址
     *
     * @param serverAddr 服务端地址，多个用逗号分隔（如 "host1:8848,host2:8848"）
     * @param nameSpace  命名空间
     */
    public ZConfigServiceImpl(String serverAddr, String nameSpace) {
        this.nameSpace = nameSpace;
        this.serverAddrs = parseServerAddrs(serverAddr);
        initEndpoints();
        initListenerManager();
    }

    /**
     * 构造方法：直接传入多个 server 地址
     */
    public ZConfigServiceImpl(List<String> serverAddrs, String nameSpace) {
        this.nameSpace = nameSpace;
        this.serverAddrs = new ArrayList<>(serverAddrs);
        initEndpoints();
        initListenerManager();
    }

    /**
     * 解析 serverAddr 字符串，支持逗号分隔的多个地址
     */
    private static List<String> parseServerAddrs(String serverAddr) {
        List<String> addrs = new ArrayList<>();
        if (serverAddr == null || serverAddr.trim().isEmpty()) {
            throw new IllegalArgumentException("serverAddr 不能为空");
        }
        for (String addr : serverAddr.split(",")) {
            String trimmed = addr.trim();
            if (!trimmed.isEmpty()) {
                addrs.add(trimmed);
            }
        }
        if (addrs.isEmpty()) {
            throw new IllegalArgumentException("serverAddr 解析后为空: " + serverAddr);
        }
        return addrs;
    }

    /**
     * 为每个 server 地址创建 ServerEndpoint
     */
    private void initEndpoints() {
        for (String addr : serverAddrs) {
            String[] parts = addr.split(":");
            if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
                throw new IllegalArgumentException("无效的 server 地址格式（应为 host:port）: " + addr);
            }
            String host = parts[0];
            int port;
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("无效的端口号: " + parts[1] + "（地址: " + addr + "）");
            }
            endpoints.add(new ServerEndpoint(host, port));
        }
        log.info("已初始化 {} 个 Server 端点: {}", endpoints.size(), serverAddrs);
    }

    private ZConfigServiceListenerManager listenerManager;

    /**
     * 初始化监听管理器，连接到当前活跃的 server
     */
    private void initListenerManager() {
        ServerEndpoint ep = getActiveEndpoint();
        listenerManager = new ZConfigServiceListenerManager(this, ep.host, ep.sidePort);
        listenerManager.init();
    }

    /**
     * 获取当前活跃的 Server 端点（轮询选取）
     */
    private ServerEndpoint getActiveEndpoint() {
        return endpoints.get(activeEndpointIndex.get() % endpoints.size());
    }

    /**
     * 切换到下一个 Server 端点（故障转移）
     */
    private ServerEndpoint switchToNextEndpoint() {
        int next = (activeEndpointIndex.get() + 1) % endpoints.size();
        activeEndpointIndex.set(next);
        ServerEndpoint ep = endpoints.get(next);
        log.info("切换到下一个 Server 端点: {}:{}", ep.host, ep.port);
        return ep;
    }

    /**
     * 获取配置（三层缓存：内存 -> 本地快照 -> 服务端拉取）
     */
    public String getConfig(String dataId, String group) {
        ConfigKey key = ConfigKey.of(nameSpace, group, dataId);

        // 第一层：内存缓存
        if (configCache.containsKey(key)) {
            return configCache.get(key);
        }

        // 第二层：本地快照文件
        String snapshot = readSnapshot(key);
        if (snapshot != null) {
            log.info("从本地快照获取配置: {}", key);
            configCache.put(key, snapshot);
            configMd5Cache.put(key, md5(snapshot));
            return snapshot;
        }

        // 第三层：从服务端拉取（支持多 Server 故障转移）
        String config = fetchConfigFromServer(key);
        if (config != null) {
            configCache.put(key, config);
            configMd5Cache.put(key, md5(config));
            writeSnapshot(key, config);
            return config;
        }

        log.warn("获取配置失败（所有 Server 不可用且无本地快照）: {}", key);
        return null;
    }

    @Override
    public void addListener(String group, String dataId, ZConfigListener listener) {
        ConfigKey key = ConfigKey.of(nameSpace, group, dataId);
        listeners.put(key, listener);
        listenerManager.startLongPolling(key, configMd5Cache.getOrDefault(key, ""));
    }

    /**
     * 处理服务端推送的配置变更响应
     */
    @Override
    public void handleServerResponse(PollResponse response) {
        ConfigKey key = response.getConfigKey();
        if (response.isChanged()) {
            String newConfig = response.getNewConfig();
            String newMd5 = response.getNewMd5();
            // 更新内存缓存
            configCache.put(key, newConfig);
            configMd5Cache.put(key, newMd5);
            // 持久化到本地快照
            writeSnapshot(key, newConfig);
            // 触发监听器回调
            ZConfigListener listener = listeners.get(key);
            if (listener != null) {
                listener.receiveConfigInfo(newConfig);
            }
        }
        // 继续下一次长轮询
        listenerManager.startLongPolling(key, configMd5Cache.getOrDefault(key, ""));
    }

    @Override
    public void rebuildListenerManager() {
        initListenerManager();
    }

    /**
     * 从服务端 HTTP 拉取配置（支持多 Server 故障转移）
     * 依次尝试每个 Server，直到有一个成功返回
     */
    private String fetchConfigFromServer(ConfigKey key) {
        int startIdx = activeEndpointIndex.get() % endpoints.size();
        for (int i = 0; i < endpoints.size(); i++) {
            int idx = (startIdx + i) % endpoints.size();
            ServerEndpoint ep = endpoints.get(idx);
            try {
                Result<String> result = ep.httpClient.getConfig(
                        ZConfigQueryRequest.of(key.getNameSpace(), key.getGroup(), key.getDataId()));
                if (result.isSuccess() && result.getData() != null) {
                    if (idx != activeEndpointIndex.get()) {
                        activeEndpointIndex.set(idx);
                        log.info("故障转移成功，切换到 Server: {}:{}", ep.host, ep.port);
                    }
                    log.info("从服务端拉取配置成功 [{}:{}]: {}", ep.host, ep.port, key);
                    return result.getData();
                }
                log.debug("Server [{}:{}] 返回配置为空: {}", ep.host, ep.port, key);
            } catch (Exception e) {
                log.warn("从 Server [{}:{}] 拉取配置失败: {}, 原因: {}", ep.host, ep.port, key, e.getMessage());
            }
        }
        return null;
    }

    // ==================== 本地快照（Snapshot）机制 ====================

    /**
     * 获取配置的本地快照目录：~/.zconfig/snapshot/{namespace}/
     */
    private Path getSnapshotDir() {
        Path dir = Paths.get(SNAPSHOT_ROOT, nameSpace != null ? nameSpace : "default");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.error("创建快照目录失败: {}", dir, e);
        }
        return dir;
    }

    /**
     * 生成快照文件名：{group}_{dataId}.snapshot
     */
    private Path getSnapshotPath(ConfigKey key) {
        String safeName = (key.getGroup() + "_" + key.getDataId()).replaceAll("[^a-zA-Z0-9._\\-]", "_");
        return getSnapshotDir().resolve(safeName + ".snapshot");
    }

    /**
     * 从本地快照文件读取配置内容
     */
    private String readSnapshot(ConfigKey key) {
        Path path = getSnapshotPath(key);
        try {
            if (Files.exists(path)) {
                return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.warn("读取本地快照失败: {}", path, e);
        }
        return null;
    }

    /**
     * 将配置内容写入本地快照文件（用于容灾降级）
     */
    private void writeSnapshot(ConfigKey key, String content) {
        if (content == null) {
            return;
        }
        Path path = getSnapshotPath(key);
        try {
            Files.write(path, content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.warn("写入本地快照失败: {}", path, e);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 计算内容的 MD5 摘要（使用标准 MessageDigest，替代 hashCode 实现）
     */
    private String md5(String content) {
        if (content == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return Bytes.toHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(content.hashCode());
        }
    }

    // ==================== HTTP 接口封装 ====================

    @Override
    public Result<String> getConfig(String group, String dataId, long timeout) {
        return getActiveEndpoint().httpClient.getConfig(
                ZConfigQueryRequest.of(nameSpace, group, dataId));
    }

    @Override
    public Result<String> saveConfig(String group, String dataId, String content) {
        ZConfigSaveRequest request = new ZConfigSaveRequest();
        request.setNamespace(nameSpace);
        request.setGroup(group);
        request.setDataId(dataId);
        request.setContent(content);
        return getActiveEndpoint().httpClient.saveConfig(request);
    }
}
