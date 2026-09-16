package com.zifang.z.config.core.server;

import com.zifang.z.config.common.Constance;
import com.zifang.z.config.common.connect.coder.CustomProtocolDecoder;
import com.zifang.z.config.common.connect.coder.CustomProtocolEncoder;
import com.zifang.z.config.common.connect.handler.HeartbeatHandler;
import com.zifang.z.config.core.server.handler.ServerBusinessHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class NettyServerComponent {

    // 1. 定义事件循环组（与纯Netty一致）
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);

    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    // 记录Netty服务通道，用于关闭
    private Channel serverChannel;

    // 2. Spring初始化完成后启动Netty（@PostConstruct：Bean初始化后执行）
    @PostConstruct
    public void startNettyServer() {
        // 关键：用独立线程启动Netty，避免sync()阻塞Spring主线程
        new Thread(() -> {
            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ChannelPipeline pipeline = ch.pipeline();
//                                pipeline.addLast(new IdleStateHandler(
//                                        ProtocolConstant.SERVER_READ_IDLE_SECONDS,
//                                        ProtocolConstant.SERVER_WRITE_IDLE_SECONDS,
//                                        ProtocolConstant.ALL_IDLE_SECONDS,
//                                        TimeUnit.SECONDS
//                                ));
                                // 自定义解码器（防粘包/过期丢弃）
                                pipeline.addLast(new CustomProtocolDecoder());
                                // 自定义编码器
                                pipeline.addLast(new CustomProtocolEncoder());
                                // 心跳处理器（服务端：isClient=false）
                                pipeline.addLast(new HeartbeatHandler(false));
                                // 业务逻辑处理器
                                pipeline.addLast(new ServerBusinessHandler());
                            }
                        })
                        // 可选：设置TCP参数（如连接队列大小）
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .childOption(ChannelOption.SO_KEEPALIVE, true);

                // 绑定端口（sync()阻塞当前子线程，不影响Spring主线程）
                ChannelFuture future = bootstrap.bind(Constance.serveBindPort).sync();
                System.out.println("Spring Boot集成的Netty服务已启动，监听 " + Constance.serveBindPort);

                // 阻塞子线程，直到Netty服务通道关闭（保证服务持续运行）
                serverChannel = future.channel();
                serverChannel.closeFuture().sync();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // 恢复中断状态
                e.printStackTrace();
            }
        }, "netty-server-thread").start(); // 给线程命名，便于排查问题
    }

    // 3. Spring关闭前优雅关闭Netty（@PreDestroy：Bean销毁前执行）
    @PreDestroy
    public void stopNettyServer() {
        System.out.println("开始关闭Netty服务");
        // 优雅关闭事件循环组，释放线程资源
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        // 关闭服务通道（若存在）
        if (serverChannel != null) {
            serverChannel.close();
        }
    }
}