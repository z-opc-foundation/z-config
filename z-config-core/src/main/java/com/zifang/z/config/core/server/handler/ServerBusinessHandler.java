package com.zifang.z.config.core.server.handler;

import com.zifang.util.json.JsonUtil;
import com.zifang.z.config.common.connect.BizCommandType;
import com.zifang.z.config.common.connect.message.NormalMessage;
import com.zifang.z.config.common.connect.message.NormalResponse;
import com.zifang.z.config.common.model.ConfigKey;
import com.zifang.z.config.common.model.PollResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.zifang.z.config.common.connect.CommandType.NORMAL_RESPONSE;

/**
 * 服务端业务处理器
 * <p>
 * 核心职责：
 * 1. 按 ConfigKey 精确管理长轮询客户端
 * 2. 配置变更时精确通知对应客户端（对齐 Nacos 精确推送机制）
 * 3. 记录推送通知历史（对齐 Nacos 推送轨迹）
 */
public class ServerBusinessHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(ServerBusinessHandler.class);

    /**
     * 按 ConfigKey 分组的等待客户端 Map
     * outerKey = ConfigKey.toKey() (namespace + group + dataId)
     * innerKey = clientKey (remote address)
     * 对齐 Nacos 的精确通知机制：配置变更时只通知监听了对应配置的客户端
     */
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, ChannelHandlerContext>> waitingClients = new ConcurrentHashMap<>();

    // 是否为客户端（区分业务逻辑）
    private final boolean isClient = false;

    // 长轮询超时时间（秒），对齐 Nacos 的 30 秒
    private static final long LONG_POLL_TIMEOUT_SECONDS = 30;

    // 推送历史记录器（可选注入）
    private static PushHistoryRecorder pushHistoryRecorder;

    /**
     * 设置推送历史记录器（Spring Bean 注入后调用）
     */
    public static void setPushHistoryRecorder(PushHistoryRecorder recorder) {
        pushHistoryRecorder = recorder;
    }

    /**
     * 推送历史记录器接口
     */
    public interface PushHistoryRecorder {
        void record(String dataId, String group, String namespace, String clientIp,
                    String pushType, String pushResult, String newMd5);
    }

    /**
     * 当配置变更时，只通知监听了对应 ConfigKey 的客户端（而非全部客户端）
     *
     * @param configKey 变更的配置标识
     * @param data      新的配置内容
     * @param md5       新内容的 MD5
     */
    public static void notifyClients(ConfigKey configKey, String data, String md5) {
        if (configKey == null) {
            log.warn("notifyClients: configKey 为空，跳过通知");
            return;
        }

        String outerKey = configKey.toKey();
        ConcurrentHashMap<String, ChannelHandlerContext> clients = waitingClients.remove(outerKey);
        if (clients == null || clients.isEmpty()) {
            log.debug("notifyClients: 没有等待的客户端监听 {}", configKey);
            return;
        }

        int notified = 0;
        for (Map.Entry<String, ChannelHandlerContext> entry : clients.entrySet()) {
            ChannelHandlerContext ctx = entry.getValue();
            if (ctx.channel().isActive()) {
                PollResponse pollResponse = new PollResponse();
                pollResponse.setChanged(true);
                pollResponse.setConfigKey(configKey);
                pollResponse.setNewConfig(data);
                pollResponse.setNewMd5(md5);

                NormalResponse normalMessage = new NormalResponse();
                normalMessage.setCommandType(NORMAL_RESPONSE.getCode());
                normalMessage.setBizCommandType(BizCommandType.LISTENER_CONFIG_RESPONSE);
                normalMessage.setData(pollResponse);

                ctx.writeAndFlush(normalMessage);
                notified++;

                // 记录推送历史
                if (pushHistoryRecorder != null) {
                    String clientIp = entry.getKey();
                    pushHistoryRecorder.record(
                            configKey.getDataId(), configKey.getGroup(), configKey.getNameSpace(),
                            clientIp, "CONFIG_CHANGE", "SUCCESS", md5);
                }
            }
        }
        log.info("notifyClients: 已通知 {} 个客户端（配置: {}）", notified, configKey);
    }

    /**
     * 兼容旧接口：向后兼容，内部委托给 notifyClients
     *
     * @deprecated 使用 {@link #notifyClients(ConfigKey, String, String)} 替代
     */
    @Deprecated
    public static void notifyAllClients(ConfigKey configKey, String data, String md5) {
        notifyClients(configKey, data, md5);
    }

    /**
     * 获取指定 ConfigKey 的监听者列表（对齐 Nacos 的配置监听者查询功能）
     *
     * @param configKey 配置标识
     * @return 监听者客户端地址列表
     */
    public static List<String> getListenerClients(ConfigKey configKey) {
        if (configKey == null) {
            return Collections.emptyList();
        }
        String outerKey = configKey.toKey();
        ConcurrentHashMap<String, ChannelHandlerContext> clients = waitingClients.get(outerKey);
        if (clients == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(clients.keySet());
    }

    /**
     * 获取所有活跃的监听者数量
     */
    public static int getActiveListenerCount() {
        int count = 0;
        for (ConcurrentHashMap<String, ChannelHandlerContext> clients : waitingClients.values()) {
            count += clients.size();
        }
        return count;
    }

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

    // 处理普通消息（服务端核心业务逻辑）
    private void handleNormalMessage(ChannelHandlerContext ctx, NormalMessage msg) {
        String bizCommand = msg.getBizCommand();
        String params = msg.getParams();
        log.info("{}收到业务指令：command={}, params={}", isClient ? "客户端" : "服务端", bizCommand, params);

        // 业务逻辑路由
        NormalResponse response = null;

        if ("USER_LOGIN".equals(bizCommand)) {
            response = handleLogin(params);
            response.setCommandType(NORMAL_RESPONSE.getCode());
            ctx.writeAndFlush(response);
        } else if ("DATA_QUERY".equals(bizCommand)) {
            response = handleDataQuery(params);
            response.setCommandType(NORMAL_RESPONSE.getCode());
            ctx.writeAndFlush(response);
        } else if (BizCommandType.LISTENER_CONFIG_REQUEST.equals(bizCommand)) {

            // 解析客户端监听的 ConfigKey
            ConfigKey configKey = JsonUtil.fromJson(params, ConfigKey.class);
            if (configKey == null) {
                log.warn("收到监听请求但解析 ConfigKey 失败: {}", params);
                return;
            }

            // 保存客户端连接到对应 ConfigKey 的分组中
            String clientKey = ctx.channel().remoteAddress().toString();
            String outerKey = configKey.toKey();
            waitingClients.computeIfAbsent(outerKey, k -> new ConcurrentHashMap<>())
                    .put(clientKey, ctx);

            log.info("注册长轮询客户端: client={}, configKey={}", clientKey, configKey);

            // 注册超时任务：若 LONG_POLL_TIMEOUT_SECONDS 秒内无数据变更，回复超时（changed=false）
            ctx.executor().schedule(() -> {
                ConcurrentHashMap<String, ChannelHandlerContext> clients = waitingClients.get(outerKey);
                if (clients != null && clients.remove(clientKey) != null) {
                    PollResponse pollResponse = new PollResponse();
                    pollResponse.setChanged(false);
                    pollResponse.setConfigKey(configKey);
                    pollResponse.setNewConfig(null);
                    pollResponse.setNewMd5(null);

                    NormalResponse normalMessage = new NormalResponse();
                    normalMessage.setCommandType(NORMAL_RESPONSE.getCode());
                    normalMessage.setBizCommandType(BizCommandType.LISTENER_CONFIG_RESPONSE);
                    normalMessage.setData(pollResponse);

                    ctx.writeAndFlush(normalMessage);
                    log.debug("长轮询超时，回复客户端: client={}, configKey={}", clientKey, configKey);
                }
            }, LONG_POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } else {
            response = new NormalResponse(false, "未知业务指令：" + bizCommand);
        }
    }

    // 处理普通响应（客户端核心业务逻辑）
    private void handleNormalResponse(ChannelHandlerContext ctx, NormalResponse response) {
        log.info("客户端收到响应：success={}, message={}, result={}",
                response.isSuccess(), response.getMessage(), response.getResult());
    }

    /**
     * 处理空闲状态事件：清理死连接
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                log.warn("服务端读空闲超时，断开连接: {}", ctx.channel().remoteAddress());
                ctx.close();
                // 清理所有 waitingClients 中对此 ctx 的引用
                cleanUpWaitingClients(ctx);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 从 waitingClients 中移除已断开的客户端连接
     */
    private void cleanUpWaitingClients(ChannelHandlerContext ctx) {
        String clientKey = ctx.channel().remoteAddress().toString();
        for (Map.Entry<String, ConcurrentHashMap<String, ChannelHandlerContext>> entry : waitingClients.entrySet()) {
            entry.getValue().remove(clientKey);
        }
    }

    // 示例1：处理登录指令
    private NormalResponse handleLogin(String params) {
        // 模拟校验（实际对接数据库/缓存）
        if (params.contains("username=admin") && params.contains("password=123456")) {
            return new NormalResponse(true, "登录成功", "{\"token\":\"xxx123xxx\"}");
        } else {
            return new NormalResponse(false, "用户名或密码错误");
        }
    }

    // 示例2：处理数据查询指令
    private NormalResponse handleDataQuery(String params) {
        return new NormalResponse(true, "查询成功", "{\"data\":[{\"id\":1,\"name\":\"测试数据\"}]}");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("业务处理异常：", cause);
        ctx.close();
        cleanUpWaitingClients(ctx);
    }
}
