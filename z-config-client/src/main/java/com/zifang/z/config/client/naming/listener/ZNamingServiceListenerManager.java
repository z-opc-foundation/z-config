package com.zifang.z.config.client.naming.listener;

import com.zifang.z.config.client.support.NamingCallClient;
import com.zifang.z.config.common.model.ZNamingInstance;
import com.zifang.z.config.common.model.naming.ZNamingSubscribeRequest;
import com.zifang.z.config.common.model.naming.ZNamingUnsubscribeRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Naming服务监听管理器
 * 负责服务变更订阅、接收推送通知、回调监听器
 * <p>
 * 注：使用被 @Deprecated 标记的 HttpRequestProxy，后续专项迁移。
 */
@SuppressWarnings("deprecation")
public class ZNamingServiceListenerManager {

    private final Logger log = LogManager.getLogger(this.getClass());


    private final String serverHost;
    private final int serverPort;
    private final String namespace;
    private final String consumerIp;
    private final int consumerPort;
    private final String consumerServiceName;
    // 服务名 -> 监听器列表
    private final Map<String, List<ZNamingListener>> listeners = new ConcurrentHashMap<>();
    // 服务名 -> 当前实例列表缓存
    private final Map<String, List<ZNamingInstance>> instanceCache = new ConcurrentHashMap<>();
    // 服务名 -> 是否已订阅
    private final Set<String> subscribedServices = ConcurrentHashMap.newKeySet();
    // 线程池：用于执行监听器回调
    private final ExecutorService callbackExecutor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "naming-listener-callback-" + System.nanoTime());
        t.setDaemon(true);
        return t;
    });
    // 定时任务：轮询检测服务变更（备用机制）
    private final ScheduledExecutorService pollExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "naming-poll-thread");
        t.setDaemon(true);
        return t;
    });
    private NamingCallClient namingClient;
    // 本地UDP监听端口（用于接收服务端推送）
    private int udpPort;

    public ZNamingServiceListenerManager(String serverHost, int serverPort, String namespace,
                                         String consumerIp, int consumerPort, String consumerServiceName) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.namespace = namespace;
        this.consumerIp = consumerIp;
        this.consumerPort = consumerPort;
        this.consumerServiceName = consumerServiceName;
        initClient();
    }

    private void initClient() {
        this.namingClient = new NamingCallClient(serverHost, serverPort);
    }

    /**
     * 添加服务监听器
     */
    public void addListener(String serviceName, ZNamingListener listener) {
        listeners.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>()).add(listener);
        // 首次监听，触发订阅
        if (!subscribedServices.contains(serviceName)) {
            subscribe(serviceName);
        }
        // 立即触发一次回调（如果已有缓存）
        List<ZNamingInstance> cachedInstances = instanceCache.get(serviceName);
        if (cachedInstances != null && !cachedInstances.isEmpty()) {
            callbackExecutor.execute(() -> {
                try {
                    listener.onChange(serviceName, cachedInstances);
                } catch (Exception e) {
                    log.error("监听器回调异常", e);
                }
            });
        }
    }

    /**
     * 移除服务监听器
     */
    public void removeListener(String serviceName, ZNamingListener listener) {
        List<ZNamingListener> listenerList = listeners.get(serviceName);
        if (listenerList != null) {
            listenerList.remove(listener);
            // 如果没有监听器了，取消订阅
            if (listenerList.isEmpty()) {
                unsubscribe(serviceName);
            }
        }
    }

    /**
     * 向服务端订阅服务变更
     */
    private void subscribe(String serviceName) {
        try {
            ZNamingSubscribeRequest request = new ZNamingSubscribeRequest();
            request.setConsumerIp(consumerIp);
            request.setConsumerPort(consumerPort);
            request.setConsumerServiceName(consumerServiceName);
            request.setConsumerGroup("DEFAULT_GROUP");
            request.setConsumerNamespace(namespace);
            request.setSubscribeServiceName(serviceName);
            request.setSubscribeGroup("DEFAULT_GROUP");
            request.setSubscribeNamespace(namespace);
            request.setSubscribeCluster("DEFAULT");

            // 调用订阅接口（需要在NamingCallClient中添加）
            // Result<String> result = namingClient.subscribe(request);
            // if (result.isSuccess()) {
            //     subscribedServices.add(serviceName);
            //     log.info("订阅服务成功: {}", serviceName);
            // }

            // 临时：直接标记为已订阅，启动轮询
            subscribedServices.add(serviceName);
            startPolling();
            log.info("订阅服务成功: {}", serviceName);

        } catch (Exception e) {
            log.error("订阅服务失败: {}", serviceName, e);
        }
    }

    /**
     * 取消服务订阅
     */
    private void unsubscribe(String serviceName) {
        try {
            ZNamingUnsubscribeRequest request = new ZNamingUnsubscribeRequest();
            request.setConsumerIp(consumerIp);
            request.setConsumerPort(consumerPort);
            request.setConsumerServiceName(consumerServiceName);
            request.setSubscribeServiceName(serviceName);
            request.setSubscribeGroup("DEFAULT_GROUP");
            request.setSubscribeNamespace(namespace);

            // 调用取消订阅接口（需要在NamingCallClient中添加）
            // Result<String> result = namingClient.unsubscribe(request);

            subscribedServices.remove(serviceName);
            instanceCache.remove(serviceName);
            log.info("取消订阅服务: {}", serviceName);

        } catch (Exception e) {
            log.error("取消订阅服务失败: {}", serviceName, e);
        }
    }

    /**
     * 接收服务端推送的变更通知
     * （由UDP或HTTP回调触发）
     */
    public void onServiceChanged(String serviceName, List<ZNamingInstance> newInstances) {
        List<ZNamingInstance> oldInstances = instanceCache.get(serviceName);
        instanceCache.put(serviceName, newInstances);

        // 触发监听器回调
        List<ZNamingListener> listenerList = listeners.get(serviceName);
        if (listenerList != null && !listenerList.isEmpty()) {
            for (ZNamingListener listener : listenerList) {
                callbackExecutor.execute(() -> {
                    try {
                        listener.onChange(serviceName, newInstances);
                        // 通知实例级别的变更
                        notifyInstanceChanges(serviceName, oldInstances, newInstances, listener);
                    } catch (Exception e) {
                        log.error("监听器回调异常", e);
                    }
                });
            }
        }
    }

    /**
     * 比较新旧实例列表，通知实例级别的增删
     */
    private void notifyInstanceChanges(String serviceName,
                                       List<ZNamingInstance> oldInstances,
                                       List<ZNamingInstance> newInstances,
                                       ZNamingListener listener) {
        if (oldInstances == null) { oldInstances = Collections.emptyList(); }

        if (newInstances == null) { newInstances = Collections.emptyList(); }


        Map<String, ZNamingInstance> oldMap = oldInstances.stream()
                .collect(Collectors.toMap(ZNamingInstance::getInstanceId, i -> i));
        Map<String, ZNamingInstance> newMap = newInstances.stream()
                .collect(Collectors.toMap(ZNamingInstance::getInstanceId, i -> i));

        // 新增的实例
        for (ZNamingInstance instance : newInstances) {
            if (!oldMap.containsKey(instance.getInstanceId())) {
                listener.onInstanceAdded(serviceName, instance);
            }
        }

        // 移除的实例
        for (ZNamingInstance instance : oldInstances) {
            if (!newMap.containsKey(instance.getInstanceId())) {
                listener.onInstanceRemoved(serviceName, instance);
            }
        }
    }

    /**
     * 启动轮询检测（备用机制）
     */
    private void startPolling() {
        pollExecutor.scheduleWithFixedDelay(() -> {
            try {
                for (String serviceName : subscribedServices) {
                    List<ZNamingInstance> currentInstances = namingClient.getAllInstances(serviceName, "DEFAULT_GROUP", namespace).getData();
                    List<ZNamingInstance> cachedInstances = instanceCache.get(serviceName);

                    // 检测变更
                    if (hasChanged(cachedInstances, currentInstances)) {
                        onServiceChanged(serviceName, currentInstances);
                    }
                }
            } catch (Exception e) {
                log.error("轮询检测服务变更异常", e);
            }
        }, 5, 5, TimeUnit.SECONDS); // 每5秒轮询一次
    }

    /**
     * 比较实例列表是否有变更
     */
    private boolean hasChanged(List<ZNamingInstance> oldList, List<ZNamingInstance> newList) {
        if (oldList == null || newList == null) { return true; }

        if (oldList.size() != newList.size()) return true;


        Set<String> oldIds = oldList.stream().map(ZNamingInstance::getInstanceId).collect(Collectors.toSet());
        Set<String> newIds = newList.stream().map(ZNamingInstance::getInstanceId).collect(Collectors.toSet());

        return !oldIds.equals(newIds);
    }

    /**
     * 启动UDP监听（用于接收服务端推送）
     */
    public void startUdpListener(int port) {
        this.udpPort = port;
        // TODO: 实现UDP监听，接收服务端推送的变更通知
        log.info("启动UDP监听端口: {}", port);
    }

    /**
     * 关闭监听管理器
     */
    public void shutdown() {
        callbackExecutor.shutdown();
        pollExecutor.shutdown();
        log.info("Naming服务监听管理器已关闭");
    }
}
