package com.zifang.z.config.client.config.listener.handler;

import com.zifang.util.json.JsonUtil;
import com.zifang.z.config.client.config.ZConfigService;
import com.zifang.z.config.common.connect.BizCommandType;
import com.zifang.z.config.common.connect.message.NormalMessage;
import com.zifang.z.config.common.connect.message.NormalResponse;
import com.zifang.z.config.common.model.PollResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClintBusinessHandler extends ChannelInboundHandlerAdapter {
    private final Logger log = LogManager.getLogger(this.getClass());

    // 是否为客户端（区分业务逻辑）
    private final boolean isClient = true;

    private ZConfigService client;

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
        } else if ("DATA_QUERY".equals(bizCommand)) {
            response = handleDataQuery(params);
        } else if (BizCommandType.LISTENER_CONFIG_RESPONSE.equals(bizCommand)) {
            try {
                PollResponse pollResponse = JsonUtil.fromJson(msg.getParams(), PollResponse.class);
                client.handleServerResponse(pollResponse);
            } catch (Exception e) {
                log.error("解析 PollResponse 失败", e);
            }
        } else {
            response = new NormalResponse(false, "未知业务指令：" + bizCommand);
        }
    }

    // 处理普通响应（客户端核心业务逻辑）
    private void handleNormalResponse(ChannelHandlerContext ctx, NormalResponse response) {
        log.info("客户端收到响应：success={}, message={}, result={}",
                response.isSuccess(), response.getMessage(), response.getResult());

        if (BizCommandType.LISTENER_CONFIG_RESPONSE.equals(response.getBizCommandType())) {
            try {
                PollResponse pollResponse = JsonUtil.fromJson(JsonUtil.toJson(response.getData()), PollResponse.class);
                client.handleServerResponse(pollResponse);
            } catch (Exception e) {
                log.error("解析 PollResponse 失败", e);
            }
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
    }


    public boolean isClient() {
        return isClient;
    }

    public ZConfigService getClient() {
        return client;
    }

    public void setClient(ZConfigService client) {
        this.client = client;
    }
}
