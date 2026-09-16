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
import java.util.concurrent.locks.ReentrantLock;

public class ZConfigServiceListenerManager {
    private final Logger log = LogManager.getLogger(this.getClass());
    private final ReentrantLock connectLock = new ReentrantLock();
    ZConfigService zConfigService;
    String serverHost;
    int serverPort;
    private Channel serverChannel; // 与服务端的连接通道

    public ZConfigServiceListenerManager(ZConfigService zConfigService, String serverHost, int serverPort) {
        this.zConfigService = zConfigService;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public void init() {
        ClintBusinessHandler clintBusinessHandler = new ClintBusinessHandler();
        clintBusinessHandler.setClient(zConfigService);

        try {
            connectLock.lock();
            if (serverChannel != null && serverChannel.isActive()) {
                return;
            }
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
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
            // 连接服务端
            ChannelFuture future = bootstrap.connect(serverHost, serverPort).sync();
            serverChannel = future.channel();
            log.info("客户端已连接到服务端: " + serverHost + ":" + serverPort);
            System.out.println("客户端已连接到服务端: " + serverHost + ":" + serverPort);
        } catch (InterruptedException e) {
            log.error("连接服务端失败", e);
            throw new RuntimeException("连接服务端失败", e);
        } finally {
            connectLock.unlock();
        }
    }

    public void startLongPolling(ConfigKey key, String clientMd5) {
        if (serverChannel == null || !serverChannel.isActive()) {
            init(); // 重连
        }

        // 普通请求 - 监听配置项变更
        String keyJson;
        try {
            keyJson = JsonUtil.toJson(key);
        } catch (Exception e) {
            throw new RuntimeException("serialize ConfigKey failed", e);
        }
        NormalMessage loginMsg = new NormalMessage(BizCommandType.LISTENER_CONFIG_REQUEST, keyJson);

        serverChannel.writeAndFlush(loginMsg).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                System.err.println("长轮询发送失败，重试...");
                serverChannel.eventLoop().schedule(() -> startLongPolling(key, clientMd5), 1, java.util.concurrent.TimeUnit.SECONDS);
            }
        });
    }
}
