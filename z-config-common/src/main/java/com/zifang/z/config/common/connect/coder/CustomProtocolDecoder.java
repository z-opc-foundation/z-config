package com.zifang.z.config.common.connect.coder;

import com.zifang.z.config.common.connect.CommandType;
import com.zifang.z.config.common.connect.ProtocolConstant;
import com.zifang.z.config.common.connect.message.HeartbeatMessage;
import com.zifang.z.config.common.connect.message.Message;
import com.zifang.z.config.common.connect.message.NormalMessage;
import com.zifang.z.config.common.connect.message.NormalResponse;
import com.zifang.z.config.common.connect.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.zip.CRC32;

import static com.zifang.z.config.common.connect.CommandType.*;

public class CustomProtocolDecoder extends ByteToMessageDecoder {

    private final Logger log = LogManager.getLogger(getClass());


    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 1. 不够协议头长度，直接返回（防粘包第一步：积累数据）
        if (in.readableBytes() < ProtocolConstant.HEADER_LENGTH) {
            return;
        }

        // 2. 标记当前读指针位置（方便后续重置）
        in.markReaderIndex();

        // 3. 解析协议头
        int magicNumber = in.readInt();          // 魔数
        byte version = in.readByte();            // 版本
        byte serializerType = in.readByte();     // 序列化方式
        short commandType = in.readShort();      // 指令类型
        long timestamp = in.readLong();          // 时间戳
        int dataLength = in.readInt();           // 数据长度
        int checksum = in.readInt();             // 校验和

        // 4. 校验魔数和版本（非法数据包直接丢弃）
        if (magicNumber != ProtocolConstant.MAGIC_NUMBER) {
            log.warn("非法数据包：魔数不匹配，channel={}", ctx.channel());
            ctx.close();
            return;
        }
        if (version != ProtocolConstant.PROTOCOL_VERSION) {
            log.warn("协议版本不兼容：当前版本={}, 服务端版本={}", version, ProtocolConstant.PROTOCOL_VERSION);
            ctx.close();
            return;
        }

        // 5. 校验数据包是否过期
        long currentTime = System.currentTimeMillis();
        if (currentTime - timestamp > ProtocolConstant.PACKET_EXPIRE_TIME) {
            log.warn("数据包过期：时间戳={}, 当前时间={}, 已过期{}ms",
                    timestamp, currentTime, currentTime - timestamp);
            // 跳过当前数据包（需要读取完数据，避免影响后续包）
            if (in.readableBytes() >= dataLength) {
                in.skipBytes(dataLength);
            } else {
                // 数据不完整，重置读指针（下次继续解析）
                in.resetReaderIndex();
            }
            return;
        }

        // 6. 不够数据长度，重置读指针（防粘包第二步：等待完整数据）
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return;
        }

        // 7. 读取数据内容
        byte[] data = new byte[dataLength];
        in.readBytes(data);

        // 8. 校验和验证（防止数据篡改）
        int calculatedChecksum = calculateChecksum(magicNumber, version, serializerType, commandType, timestamp, dataLength, data);
        if (calculatedChecksum != checksum) {
            log.warn("数据校验失败：实际校验和={}, 计算校验和={}", checksum, calculatedChecksum);
            return;
        }

        // 9. 反序列化数据，构建消息对象
        Serializer serializer = Serializer.Factory.getSerializer(serializerType);
        CommandType command = CommandType.getByCode(commandType);
        Message message = buildMessage(command, serializer, data);
        if (message != null) {
            message.setCommandType(commandType);
            message.setTimestamp(timestamp);
            out.add(message);
        }
    }

    // 构建具体消息对象（根据指令类型路由）
    private Message buildMessage(CommandType command, Serializer serializer, byte[] data) throws Exception {
        if (HEARTBEAT_REQUEST == command || HEARTBEAT_RESPONSE == command) {
            return serializer.deserialize(data, HeartbeatMessage.class);
        } else if (NORMAL_MESSAGE == command) {
            return serializer.deserialize(data, NormalMessage.class);
        } else if (NORMAL_RESPONSE == command) {
            return serializer.deserialize(data, NormalResponse.class);
        }

        return null;
    }

    // 计算校验和（基于协议头+数据内容）
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