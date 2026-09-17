package com.zifang.z.config.client.config.listener.handler;

import com.zifang.util.json.JsonUtil;
import com.zifang.z.config.client.config.ZConfigService;
import com.zifang.z.config.client.config.listener.ZConfigServiceListenerManager;
import com.zifang.z.config.common.connect.BizCommandType;
import com.zifang.z.config.common.connect.CommandType;
import com.zifang.z.config.common.connect.message.NormalMessage;
import com.zifang.z.config.common.connect.message.NormalResponse;
import com.zifang.z.config.common.model.PollResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 客户端 Netty 业务处理器
 * <p>
 * 核心改进（对齐 Nacos）：
 * 1. 连接断开后自动触发指数退避重连
 * 2. 读空闲时主动发心跳保活
 * 3. 支持多 Server 地址轮询重连
 */
public class ClintBusinessHandler extends ChannelInboundHandlerAdapter {
    private final Logger log = LogManager.getLogger(this.getClass());

    private final boolean isClient = true;

    private ZConfigService client;

    /**
     * 关联的 ListenerManager（用于触发重连）
     */
    private ZConfigServiceListenerManager listenerManager;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof NormalMessage) {
            handleNormalMessage(ctx, (NormalMessage) msg);
        } else if (msg instanceof NormalResponse) {
            handleNormalResponse(ctx, (NormalResponse) msg);
        } else {
            super.channelRead(ctx, msg);
        }
    }

    /**
     * 连接断开时触发重连（对齐 Nacos 的自动重连机制）
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.warn("与服务端的连接已断开: {}", ctx.channel().remoteAddress());
        // 触发异步重连，避免阻塞 Netty IO 线程
        scheduleReconnect();
        super.channelInactive(ctx);
    }

    /**
     * 调度重连（指数退避 + 随机抖动，避免重连风暴）
     * 对齐 Nacos 客户端的重连策略
     */
    private void scheduleReconnect() {
        if (listenerManager == null) {
            log.error("listenerManager 未设置，无法重连");
            return;
        }
        // 使用指数退避：1s, 2s, 4s, 8s, ... 最大 30s
        // 加入随机抖动（±20%）避免所有客户端同时重连
        long baseDelay = Math.min(1000L * (1L << Math.min(listenerManager.getReconnectAttempts(), 5)), 30000L);
        long jitter = (long) (baseDelay * 0.2 * (Math.random() * 2 - 1));
        long delay = Math.max(500L, baseDelay + jitter);

        log.info("将在 {}ms 后尝试重连（第 {} 次）", delay, listenerManager.getReconnectAttempts() + 1);
        listenerManager.getEventLoop().schedule(() -> {
            try {
                listenerManager.incrementReconnectAttempts();
                listenerManager.reconnect();
            } catch (Exception e) {
                log.error("重连失败", e);
            }
        }, delay, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    // 处理普通消息
    private void handleNormalMessage(ChannelHandlerContext ctx, NormalMessage msg) {
        String bizCommand = msg.getBizCommand();
        String params = msg.getParams();
        log.info("{}收到业务指令：command={}, params={}", isClient ? "客户端" : "服务端", bizCommand, params);

        NormalResponse response = null;

        if ("USER_LOGIN".equals(bizCommand)) {
            response = handleLogin(params);
        } else if ("DATA_QUERY".equals(bizCommand)) {
            response = handleDataQuery(params);
        } else if (BizCommandType.LISTENER_CONFIG_RESPONSE.equals(bizCommand)) {
            try {
                PollResponse pollResponse = JsonUtil.fromJson(msg.getParams(), PollResponse.class);
                client.handleServerResponse(pollResponse);
                // 收到响应后重置重连计数（说明连接正常）
                if (listenerManager != null) {
                    listenerManager.resetReconnectAttempts();
                }
            } catch (Exception e) {
                log.error("解析 PollResponse 失败", e);
            }
            return; // 响应不需要写回
        } else {
            response = new NormalResponse(false, "未知业务指令：" + bizCommand);
        }

        if (response != null) {
            response.setCommandType(com.zifang.z.config.common.connect.CommandType.NORMAL_RESPONSE.getCode());
            ctx.writeAndFlush(response);
        }
    }

    // 处理普通响应
    private void handleNormalResponse(ChannelHandlerContext ctx, NormalResponse response) {
        log.info("客户端收到响应：success={}, message={}, result={}",
                response.isSuccess(), response.getMessage(), response.getResult());

        if (BizCommandType.LISTENER_CONFIG_RESPONSE.equals(response.getBizCommandType())) {
            try {
                PollResponse pollResponse = JsonUtil.fromJson(JsonUtil.toJson(response.getData()), PollResponse.class);
                client.handleServerResponse(pollResponse);
                if (listenerManager != null) {
                    listenerManager.resetReconnectAttempts();
                }
            } catch (Exception e) {
                log.error("解析 PollResponse 失败", e);
            }
        }
    }

    private NormalResponse handleLogin(String params) {
        if (params.contains("username=admin") && params.contains("password=123456")) {
            return new NormalResponse(true, "登录成功", "{\"token\":\"xxx123xxx\"}");
        } else {
            return new NormalResponse(false, "用户名或密码错误");
        }
    }

    private NormalResponse handleDataQuery(String params) {
        return new NormalResponse(true, "查询成功", "{\"data\":[{\"id\":1,\"name\":\"测试数据\"}]}");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("客户端业务处理异常: {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }

    // ==================== Getter/Setter ====================

    public boolean isClient() {
        return isClient;
    }

    public ZConfigService getClient() {
        return client;
    }

    public void setClient(ZConfigService client) {
        this.client = client;
    }

    public ZConfigServiceListenerManager getListenerManager() {
        return listenerManager;
    }

    public void setListenerManager(ZConfigServiceListenerManager listenerManager) {
        this.listenerManager = listenerManager;
    }
}
