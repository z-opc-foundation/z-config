package com.zifang.z.config.common.connect.coder;

import com.zifang.util.json.JsonUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;


public class JsonEncoder extends MessageToByteEncoder<Object> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        // 将对象序列化为 JSON 字符串，再转为字节数组
        byte[] jsonBytes = JsonUtil.toJson(msg).getBytes(StandardCharsets.UTF_8);
        out.writeBytes(jsonBytes);
    }
}