package com.zifang.z.config.common.connect.serializer;


import com.zifang.z.config.common.connect.ProtocolConstant;

import java.io.IOException;

public interface Serializer {
    // 序列化
    <T> byte[] serialize(T obj) throws IOException;

    // 反序列化
    <T> T deserialize(byte[] data, Class<T> clazz) throws IOException;

    // 获取序列化类型（对应 ProtocolConstant 中的序列化方式）
    byte getSerializerType();

    // 序列化工厂（根据类型获取实例）
    class Factory {
        private static final Serializer JSON_SERIALIZER = new JsonSerializer();

        public static Serializer getSerializer(byte type) {
            if (type == ProtocolConstant.SERIALIZER_TYPE_JSON) {
                return JSON_SERIALIZER;
            }
            throw new IllegalArgumentException("未知序列化类型：" + type);
        }
    }
}