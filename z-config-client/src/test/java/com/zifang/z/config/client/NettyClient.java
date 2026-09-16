package com.zifang.z.config.client;

import com.zifang.z.config.client.config.listener.handler.ClintBusinessHandler;
import com.zifang.z.config.common.connect.ProtocolConstant;
import com.zifang.z.config.common.connect.coder.CustomProtocolDecoder;
import com.zifang.z.config.common.connect.coder.CustomProtocolEncoder;
import com.zifang.z.config.common.connect.handler.HeartbeatHandler;
import com.zifang.z.config.common.connect.message.NormalMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

public class NettyClient {
    private static final Logger log = LogManager.getLogger(NettyClient.class);
    private final String host;
    private final int port;
    private EventLoopGroup group;
    private Channel channel;

    public NettyClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static void main(String[] args) {
        NettyClient client = new NettyClient(ProtocolConstant.SERVER_HOST, ProtocolConstant.SERVER_PORT);
        client.start();

        // 注册JVM关闭钩子，优雅关闭客户端
        Runtime.getRuntime().addShutdownHook(new Thread(client::shutdown));
    }

    // 启动客户端（包含重连逻辑）
    public void start() {
        group = new NioEventLoopGroup();
        doConnect();
    }

    // 连接服务端（失败自动重连）
    private void doConnect() {
        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000) // 连接超时3秒
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                // 空闲检测（读空闲10秒，写空闲10秒）
                                .addLast(new IdleStateHandler(
                                        ProtocolConstant.CLIENT_READ_IDLE_SECONDS,
                                        ProtocolConstant.CLIENT_WRITE_IDLE_SECONDS,
                                        ProtocolConstant.ALL_IDLE_SECONDS,
                                        TimeUnit.SECONDS
                                ))

                                // 自定义解码器
                                .addLast(new CustomProtocolDecoder())
                                // 自定义编码器
                                .addLast(new CustomProtocolEncoder())
                                // 心跳处理器（客户端：isClient=true）
                                .addLast(new HeartbeatHandler(true))
                                // 业务逻辑处理器
                                .addLast(new ClintBusinessHandler());
                    }
                });

        // 发起连接
        bootstrap.connect(host, port).addListener(future -> {
            if (future.isSuccess()) {
                channel = ((ChannelFuture) future).channel();
                log.info("客户端连接成功：{}:{}", host, port);
                // 连接成功后，发送测试指令
                sendTestCommand();
            } else {
                log.error("客户端连接失败，5秒后重试：{}:{}", host, port);
                // 5秒后自动重连
                group.schedule(this::doConnect, 5, TimeUnit.SECONDS);
            }
        });
    }

    // 发送测试业务指令
    private void sendTestCommand() {
        // 1. 发送登录指令
        NormalMessage loginMsg = new NormalMessage("USER_LOGIN", "username=admin&password=123456");
        channel.writeAndFlush(loginMsg);

        // 2. 2秒后发送数据查询指令
        group.schedule(() -> {
            NormalMessage queryMsg = new NormalMessage("DATA_QUERY", "id=1");
            channel.writeAndFlush(queryMsg);
        }, 2, TimeUnit.SECONDS);
    }

    // 关闭客户端
    public void shutdown() {
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
        log.info("客户端关闭");
    }
}