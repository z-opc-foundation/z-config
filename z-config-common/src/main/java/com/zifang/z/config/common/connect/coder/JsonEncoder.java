package com.zifang.z.config.common.connect.coder;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;


public class JsonEncoder extends MessageToByteEncoder<Object> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        // 将对象序列化为 JSON 字符串，再转为字节数组
        byte[] jsonBytes = objectMapper.writeValueAsBytes(msg);
        out.writeBytes(jsonBytes);
    }
}