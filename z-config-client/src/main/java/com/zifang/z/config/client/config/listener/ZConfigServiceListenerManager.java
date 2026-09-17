package com.zifang.z.config.client.config.listener;

import com.zifang.util.json.JsonUtil;
import com.zifang.z.config.client.config.ZConfigService;
import com.zifang.z.config.client.config.listener.handler.ClintBusinessHandler;
import com.zifang.z.config.common.connect.BizCommandType;
import com.zifang.z.config.common.connect.ProtocolConstant;
import com.zifang.z.config.common.connect.coder.CustomProtocolDecoder;
import com.zifang.z.config.common.connect.coder.CustomProtocolEncoder;
import com.zifang.z.config.common.connect.handler.HeartbeatHandler;
import com.zifang.z.config.common.connect.message.NormalMessage;
import com.zifang.z.config.common.model.ConfigKey;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 客户端长连接管理器
 * <p>
 * 核心改进（对齐 Nacos）：
 * 1. 自动重连：连接断开后指数退避重连
 * 2. EventLoopGroup 生命周期管理：避免内存泄漏
 * 3. 重连计数器：成功后重置，失败后递增
 * 4. 支持多 Server 地址轮询（由外部 ZConfigServiceImpl 管理地址切换）
 */
public class ZConfigServiceListenerManager {
    private final Logger log = LogManager.getLogger(this.getClass());

    ZConfigService zConfigService;
    String serverHost;
    int serverPort;

    private volatile Channel serverChannel;
    private volatile EventLoopGroup workerGroup;

    /**
     * 重连计数器（成功通信后重置）
     */
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);

    /**
     * 最大重连次数（超过后需要外部切换 Server）
     */
    private static final int MAX_RECONNECT_ATTEMPTS = 10;

    public ZConfigServiceListenerManager(ZConfigService zConfigService, String serverHost, int serverPort) {
        this.zConfigService = zConfigService;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    /**
     * 初始化连接
     */
    public void init() {
        try {
            if (serverChannel != null && serverChannel.isActive()) {
                return;
            }
            connect();
        } catch (Exception e) {
            log.error("初始化连接失败: {}:{}", serverHost, serverPort, e);
        }
    }

    /**
     * 建立到服务端的 Netty 连接
     */
    private void connect() throws InterruptedException {
        // 关闭旧的 EventLoopGroup 避免泄漏
        closeExistingConnection();

        // 创建新的 EventLoopGroup
        workerGroup = new NioEventLoopGroup();

        ClintBusinessHandler clintBusinessHandler = new ClintBusinessHandler();
        clintBusinessHandler.setClient(zConfigService);
        clintBusinessHandler.setListenerManager(this);

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new IdleStateHandler(
                                ProtocolConstant.CLIENT_READ_IDLE_SECONDS,
                                ProtocolConstant.CLIENT_WRITE_IDLE_SECONDS,
                                ProtocolConstant.ALL_IDLE_SECONDS,
                                TimeUnit.SECONDS
                        ));
                        pipeline.addLast(new CustomProtocolDecoder());
                        pipeline.addLast(new CustomProtocolEncoder());
                        pipeline.addLast(new HeartbeatHandler(true));
                        pipeline.addLast(clintBusinessHandler);
                    }
                });

        // 连接服务端（同步等待）
        ChannelFuture future = bootstrap.connect(serverHost, serverPort).sync();
        serverChannel = future.channel();
        log.info("客户端已连接到服务端: {}:{}", serverHost, serverPort);
    }

    /**
     * 重连（供 ClintBusinessHandler 调用）
     */
    public void reconnect() {
        if (reconnectAttempts.get() > MAX_RECONNECT_ATTEMPTS) {
            log.error("重连次数超过上限 ({}), 停止重连. 请检查服务端状态或切换 Server 地址",
                    MAX_RECONNECT_ATTEMPTS);
            return;
        }
        try {
            log.info("尝试重连到 {}:{}", serverHost, serverPort);
            connect();
            log.info("重连成功: {}:{}", serverHost, serverPort);
        } catch (Exception e) {
            log.error("重连失败: {}:{}, 将继续重试", serverHost, serverPort, e);
            // 重连失败，触发下一次重连
            reconnectAttempts.incrementAndGet();
        }
    }

    /**
     * 关闭现有连接和 EventLoopGroup
     */
    private void closeExistingConnection() {
        if (serverChannel != null && serverChannel.isActive()) {
            serverChannel.close();
            serverChannel = null;
        }
        if (workerGroup != null && !workerGroup.isShutdown()) {
            workerGroup.shutdownGracefully(0, 1, TimeUnit.SECONDS);
            workerGroup = null;
        }
    }

    /**
     * 启动长轮询
     */
    public void startLongPolling(ConfigKey key, String clientMd5) {
        if (serverChannel == null || !serverChannel.isActive()) {
            log.warn("连接未就绪，尝试重连...");
            try {
                connect();
            } catch (Exception e) {
                log.error("长轮询前重连失败", e);
                return;
            }
        }

        String keyJson;
        try {
            keyJson = JsonUtil.toJson(key);
        } catch (Exception e) {
            throw new RuntimeException("serialize ConfigKey failed", e);
        }
        NormalMessage msg = new NormalMessage(BizCommandType.LISTENER_CONFIG_REQUEST, keyJson);

        serverChannel.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                log.warn("长轮询发送失败: {}", key);
                // 延迟 1 秒重试
                serverChannel.eventLoop().schedule(
                        () -> startLongPolling(key, clientMd5),
                        1, TimeUnit.SECONDS);
            }
        });
    }

    /**
     * 获取 EventLoop（用于调度重连任务）
     */
    public EventLoop getEventLoop() {
        return serverChannel != null ? serverChannel.eventLoop() : null;
    }

    /**
     * 获取当前重连次数
     */
    public int getReconnectAttempts() {
        return reconnectAttempts.get();
    }

    /**
     * 增加重连计数
     */
    public void incrementReconnectAttempts() {
        reconnectAttempts.incrementAndGet();
    }

    /**
     * 重置重连计数（连接恢复正常时调用）
     */
    public void resetReconnectAttempts() {
        reconnectAttempts.set(0);
    }

    /**
     * 关闭连接管理器
     */
    public void shutdown() {
        closeExistingConnection();
        log.info("客户端连接管理器已关闭: {}:{}", serverHost, serverPort);
    }
}
