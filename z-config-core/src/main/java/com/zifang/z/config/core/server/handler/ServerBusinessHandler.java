package com.zifang.z.config.core.server.handler;

import com.zifang.util.json.JsonUtil;
import com.zifang.z.config.common.connect.BizCommandType;
import com.zifang.z.config.common.connect.message.NormalMessage;
import com.zifang.z.config.common.connect.message.NormalResponse;
import com.zifang.z.config.common.model.ConfigKey;
import com.zifang.z.config.common.model.PollResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.zifang.z.config.common.connect.CommandType.NORMAL_RESPONSE;

public class ServerBusinessHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(ServerBusinessHandler.class);
    private static final ConcurrentHashMap<String, ChannelHandlerContext> waitingClients = new ConcurrentHashMap<>();
    // 是否为客户端（区分业务逻辑）
    private final boolean isClient = false;

    // 当有新数据时，触发所有等待的长轮询客户端响应
    public static void notifyAllClients(ConfigKey configKey, String data, String md5) {
        // 遍历所有等待的客户端，发送新数据并移除
        for (Map.Entry<String, ChannelHandlerContext> entry : waitingClients.entrySet()) {
            ChannelHandlerContext ctx = entry.getValue();
            if (ctx.channel().isActive()) { // 确保连接有效

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
            }
            waitingClients.remove(entry.getKey());
        }
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

        // 业务逻辑路由（可扩展为 Spring Bean 注入）
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

            System.out.println("收到客户端消息：" + msg);

            // 保存客户端连接（用远程地址作为key，避免重复）
            String clientKey = ctx.channel().remoteAddress().toString();
            waitingClients.put(clientKey, ctx);

            // 注册超时任务：若30秒内无数据，回复超时
            ctx.executor().schedule(() -> {
                if (waitingClients.containsKey(clientKey)) {
                    // 超时后移除并回复
                    waitingClients.remove(clientKey);


                    ConfigKey configKey = JsonUtil.fromJson(params, ConfigKey.class);

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
                }
            }, 5, TimeUnit.SECONDS);
        } else {
            response = new NormalResponse(false, "未知业务指令：" + bizCommand);
        }
    }

    // 处理普通响应（客户端核心业务逻辑）
    private void handleNormalResponse(ChannelHandlerContext ctx, NormalResponse response) {
        log.info("客户端收到响应：success={}, message={}, result={}",
                response.isSuccess(), response.getMessage(), response.getResult());
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
    }
}