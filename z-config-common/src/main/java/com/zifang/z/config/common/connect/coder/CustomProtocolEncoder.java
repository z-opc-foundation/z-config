package com.zifang.z.config.common.connect.coder;

import com.zifang.z.config.common.connect.CommandType;
import com.zifang.z.config.common.connect.ProtocolConstant;
import com.zifang.z.config.common.connect.message.Message;
import com.zifang.z.config.common.connect.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.zip.CRC32;

public class CustomProtocolEncoder extends MessageToByteEncoder<Message> {
    private final Logger log = LogManager.getLogger(getClass());

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
        log.debug("编码消息：指令={}, 数据长度={}, 校验和={}", msg);

        // 1. 获取序列化器（默认JSON）
        Serializer serializer = Serializer.Factory.getSerializer(ProtocolConstant.SERIALIZER_TYPE_JSON);
        // 2. 序列化业务数据
        byte[] data = serializer.serialize(msg);
        // 3. 构建协议头并写入
        int magicNumber = ProtocolConstant.MAGIC_NUMBER;
        byte version = ProtocolConstant.PROTOCOL_VERSION;
        byte serializerType = serializer.getSerializerType();
        short commandType = msg.getCommandType();
        long timestamp = msg.getTimestamp();
        int dataLength = data.length;
        // 4. 计算校验和
        int checksum = calculateChecksum(magicNumber, version, serializerType, commandType, timestamp, dataLength, data);

        // 5. 写入ByteBuf（大端序，网络字节序）
        out.writeInt(magicNumber);
        out.writeByte(version);
        out.writeByte(serializerType);
        out.writeShort(commandType);
        out.writeLong(timestamp);
        out.writeInt(dataLength);
        out.writeInt(checksum);
        // 6. 写入业务数据
        out.writeBytes(data);

        log.debug("编码消息：指令={}, 数据长度={}, 校验和={}",
                CommandType.getByCode(commandType), dataLength, checksum);
    }

    // 与解码器校验和算法一致
    private int calculateChecksum(int magicNumber, byte version, byte serializerType,
                                  short commandType, long timestamp, int dataLength, byte[] data) {
        CRC32 crc32C = new CRC32();
        crc32C.update(intToBytes(magicNumber));
        crc32C.update(version);
        crc32C.update(serializerType);
        crc32C.update(shortToBytes(commandType));
        crc32C.update(longToBytes(timestamp));
        crc32C.update(intToBytes(dataLength));
        crc32C.update(data);
        return (int) crc32C.getValue();
    }

    // 工具方法：int转byte数组（大端序）
    private byte[] intToBytes(int value) {
        return new byte[]{
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value
        };
    }

    // 工具方法：short转byte数组（大端序）
    private byte[] shortToBytes(short value) {
        return new byte[]{
                (byte) (value >> 8),
                (byte) value
        };
    }

    // 工具方法：long转byte数组（大端序）
    private byte[] longToBytes(long value) {
        return new byte[]{
                (byte) (value >> 56),
                (byte) (value >> 48),
                (byte) (value >> 40),
                (byte) (value >> 32),
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value
        };
    }
}