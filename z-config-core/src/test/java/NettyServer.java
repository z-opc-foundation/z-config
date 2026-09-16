import com.zifang.z.config.common.connect.ProtocolConstant;
import com.zifang.z.config.common.connect.coder.CustomProtocolDecoder;
import com.zifang.z.config.common.connect.coder.CustomProtocolEncoder;
import com.zifang.z.config.common.connect.handler.HeartbeatHandler;
import com.zifang.z.config.core.server.handler.ServerBusinessHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

public class NettyServer {
    private static final Logger log = LogManager.getLogger(NettyServer.class);
    private final int port;

    public NettyServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        new NettyServer(ProtocolConstant.SERVER_PORT).start();
    }

    public void start() {
        // 1. 配置EventLoopGroup（bossGroup处理连接，workerGroup处理IO）
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            // 2. 配置服务端启动器
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128) // 连接队列大小
                    .childOption(ChannelOption.SO_KEEPALIVE, true) // 开启TCP保活
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // 处理器链（顺序：解码->心跳->编码->业务）
                            ch.pipeline()
                                    // 空闲检测（读空闲10秒）
                                    .addLast(new IdleStateHandler(
                                            ProtocolConstant.SERVER_READ_IDLE_SECONDS,
                                            ProtocolConstant.SERVER_WRITE_IDLE_SECONDS,
                                            ProtocolConstant.ALL_IDLE_SECONDS,
                                            TimeUnit.SECONDS
                                    ))

                                    // 自定义解码器（防粘包/过期丢弃）
                                    .addLast(new CustomProtocolDecoder())
                                    // 自定义编码器
                                    .addLast(new CustomProtocolEncoder())
                                    // 心跳处理器（服务端：isClient=false）
                                    .addLast(new HeartbeatHandler(false))
                                    // 业务逻辑处理器
                                    .addLast(new ServerBusinessHandler());
                        }
                    });

            // 3. 绑定端口，同步等待启动
            ChannelFuture future = bootstrap.bind(port).sync();
            log.info("服务端启动成功，端口：{}", port);

            // 4. 等待服务端关闭（阻塞）
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("服务端启动异常：", e);
            Thread.currentThread().interrupt();
        } finally {
            // 5. 优雅关闭EventLoopGroup
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            log.info("服务端关闭");
        }
    }
}