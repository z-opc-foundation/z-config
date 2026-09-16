package com.zifang.z.config.common.connect.handler;

import com.zifang.z.config.common.connect.CommandType;
import com.zifang.z.config.common.connect.message.HeartbeatMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HeartbeatHandler extends ChannelInboundHandlerAdapter {

    private final Logger log = LogManager.getLogger(getClass());

    // 是否为客户端（区分心跳发送逻辑）
    private final boolean isClient;

    public HeartbeatHandler(boolean isClient) {
        this.isClient = isClient;
    }

    // 空闲事件触发（由 IdleStateHandler 触发）
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE) {
                // 读空闲：未收到对方消息，断开连接（防止死连接）
                log.warn("{}读空闲超时，断开连接：{}", isClient ? "客户端" : "服务端", ctx.channel());
                ctx.close();
            } else if (state == IdleState.WRITER_IDLE) {
                // 写空闲：发送心跳请求
                HeartbeatMessage heartbeat = new HeartbeatMessage(CommandType.HEARTBEAT_REQUEST);
                ctx.writeAndFlush(heartbeat);
                log.debug("{}发送心跳请求：{}", isClient ? "客户端" : "服务端", ctx.channel());
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    // 接收心跳请求，回复心跳响应
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HeartbeatMessage) {
            CommandType command = ((HeartbeatMessage) msg).getCommand();
            if (command == CommandType.HEARTBEAT_REQUEST) {
                // 收到心跳请求，回复响应
                HeartbeatMessage response = new HeartbeatMessage(CommandType.HEARTBEAT_RESPONSE);
                ctx.writeAndFlush(response);
                log.debug("{}回复心跳响应：{}", isClient ? "客户端" : "服务端", ctx.channel());
            } else if (command == CommandType.HEARTBEAT_RESPONSE) {
                // 收到心跳响应，更新空闲状态（无需额外处理，IdleStateHandler 会重置计数器）
                log.debug("{}收到心跳响应：{}", isClient ? "客户端" : "服务端", ctx.channel());
            }
        } else {
            // 非心跳消息，透传给下一个处理器
            super.channelRead(ctx, msg);
        }
    }
}