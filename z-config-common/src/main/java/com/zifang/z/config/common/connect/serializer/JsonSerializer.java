package com.zifang.z.config.common.connect.serializer;

import com.zifang.util.json.JsonUtil;
import com.zifang.z.config.common.connect.ProtocolConstant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JsonSerializer implements Serializer {

    @Override
    public <T> byte[] serialize(T obj) throws IOException {
        try {
            return JsonUtil.toJson(obj).getBytes(StandardCharsets.UTF_8);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) throws IOException {
        return JsonUtil.fromJson(new String(data, StandardCharsets.UTF_8), clazz);
    }

    @Override
    public byte getSerializerType() {
        return ProtocolConstant.SERIALIZER_TYPE_JSON;
    }
}