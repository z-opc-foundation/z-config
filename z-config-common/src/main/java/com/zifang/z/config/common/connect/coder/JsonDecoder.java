package com.zifang.z.config.common.connect.coder;

import com.zifang.util.json.JsonUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;


public class JsonDecoder extends ByteToMessageDecoder {
    private final Class<?> targetClass;

    // 构造函数指定要解码的目标类（客户端解码响应，服务端解码请求）
    public JsonDecoder(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 读取所有字节（实际应处理粘包/拆包，这里简化）
        byte[] bytes = new byte[in.readableBytes()];
        in.readBytes(bytes);
        // 将 JSON 字节数组反序列化为目标对象
        Object obj = JsonUtil.fromJson(new String(bytes, StandardCharsets.UTF_8), targetClass);
        out.add(obj);
    }
}