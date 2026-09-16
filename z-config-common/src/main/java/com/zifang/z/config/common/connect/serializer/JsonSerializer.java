package com.zifang.z.config.common.connect.serializer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.z.config.common.connect.ProtocolConstant;

import java.io.IOException;

public class JsonSerializer implements Serializer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // 关键配置


    @Override
    public <T> byte[] serialize(T obj) throws IOException {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(obj);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) throws IOException {
        return OBJECT_MAPPER.readValue(data, clazz);
    }

    @Override
    public byte getSerializerType() {
        return ProtocolConstant.SERIALIZER_TYPE_JSON;
    }
}