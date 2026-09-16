package com.zifang.z.config.client.config;

import com.zifang.util.core.meta.Result;
import com.zifang.z.config.client.config.listener.ZConfigListener;
import com.zifang.z.config.client.config.listener.ZConfigServiceListenerManager;
import com.zifang.z.config.client.support.ConfigCallClient;
import com.zifang.z.config.common.Constance;
import com.zifang.z.config.common.model.ConfigKey;
import com.zifang.z.config.common.model.PollResponse;
import com.zifang.z.config.common.model.config.ZConfigQueryRequest;
import com.zifang.z.config.common.model.config.ZConfigSaveRequest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Config 服务客户端实现
 * <p>
 * 注：使用被 @Deprecated 标记的 HttpRequestProxy，因为 ConfigCallClient 是
 * 注解驱动的接口代理（@RestController + @RequestMapping），重构为
 * HttpExecutor 需要重写 3 个方法。后续专项迁移。
 */
@SuppressWarnings("deprecation")
public class ZConfigServiceImpl implements ZConfigService {

    private final String serviceAddr;
    private final String nameSpace;
    // 本地缓存：配置标识 -> 配置内容
    private final Map<ConfigKey, String> configCache = new ConcurrentHashMap<>();
    // 本地缓存MD5：配置标识 -> MD5
    private final Map<ConfigKey, String> configMd5Cache = new ConcurrentHashMap<>();
    // 监听器注册表：配置标识 -> 监听器列表
    private final Map<ConfigKey, ZConfigListener> listeners = new ConcurrentHashMap<>();
    private String serverHost;
    private int serverPort;
    private int serverSidePort;
    private ConfigCallClient client;
    private ZConfigServiceListenerManager ZConfigServiceListenerManager;

    public ZConfigServiceImpl(String serviceAddr, String nameSpace) {

        this.serviceAddr = serviceAddr;
        this.nameSpace = nameSpace;

        init();
    }

    private void solveParameters() {
        this.serverHost = this.serviceAddr.split(":")[0];
        this.serverPort = Integer.parseInt(serviceAddr.split(":")[1]);

        // 使用默认监听端口
        this.serverSidePort = Constance.serveBindPort;
    }

    private void init() {

        // 解析参数
        solveParameters();

        // 初始化配置捕获器
        initConfigCallClient();

        // 初始化监听服务器
        initConfigListenerServer();
    }

    private void initConfigListenerServer() {
        ZConfigServiceListenerManager = new ZConfigServiceListenerManager(
                this,
                serverHost,
                serverSidePort

        );
        ZConfigServiceListenerManager.init();
    }

    private void initConfigCallClient() {
        client = new ConfigCallClient(serverHost, serverPort);
    }

    // 获取配置
    public String getConfig(String dataId, String group) {
        ConfigKey key = ConfigKey.of(nameSpace, group, dataId);
        if (configCache.containsKey(key)) {
            return configCache.get(key);
        }
        // 从服务端拉取（简化）
        String config = fetchConfigFromServer(key);
        configCache.put(key, config);
        configMd5Cache.put(key, md5(config));
        return config;
    }

    @Override
    public void addListener(String group, String dataId, ZConfigListener listener) {
        ConfigKey key = ConfigKey.of(nameSpace, group, dataId);
        listeners.put(key, listener);
        ZConfigServiceListenerManager.startLongPolling(key, configMd5Cache.getOrDefault(key, ""));
    }


    // 处理服务端响应
    @Override
    public void handleServerResponse(PollResponse response) {
        ConfigKey key = response.getConfigKey();
        if (response.isChanged()) {
            // 更新缓存
            configCache.put(key, response.getNewConfig());
            configMd5Cache.put(key, response.getNewMd5());
            // 触发监听器
            ZConfigListener listener = listeners.get(key);
            if (listener != null) {
                listener.receiveConfigInfo(response.getNewConfig());
            }
        }
        // 继续下一次长轮询
        ZConfigServiceListenerManager.startLongPolling(key, configMd5Cache.getOrDefault(key, ""));
    }

    @Override
    public void rebuildListenerManager() {
        // 初始化监听服务器
        initConfigListenerServer();
    }

    // 从服务端拉取配置（简化）
    private String fetchConfigFromServer(ConfigKey key) {
        String config = "initialConfig: " + key;
        System.out.println("从服务端拉取配置: " + config);
        return config;
    }

    // 简化的MD5计算
    private String md5(String content) {
        return Integer.toHexString(content.hashCode());
    }


    @Override
    public Result<String> getConfig(String group, String dataId, long timeout) {
        return client.getConfig(ZConfigQueryRequest.of(nameSpace, group, dataId));
    }

    @Override
    public Result<String> saveConfig(String group, String dataId, String xcxx) {
        ZConfigSaveRequest zConfigSaveRequest = new ZConfigSaveRequest();
        zConfigSaveRequest.setNamespace(nameSpace);
        zConfigSaveRequest.setGroup(group);
        zConfigSaveRequest.setDataId(dataId);
        zConfigSaveRequest.setContent(xcxx);
        return client.saveConfig(zConfigSaveRequest);
    }

}