package com.zifang.z.config.common.connect;

public class ProtocolConstant {
    // 魔数（用于校验数据包合法性）
    public static final int MAGIC_NUMBER = 0xCAFEBABE;
    // 协议版本
    public static final byte PROTOCOL_VERSION = 1;
    // 序列化方式（0=JSON，1=Hessian，预留扩展）
    public static final byte SERIALIZER_TYPE_JSON = 0;
    // 数据包过期时间（5秒）
    public static final long PACKET_EXPIRE_TIME = Long.MAX_VALUE;
    // 协议头长度（魔数4 + 版本1 + 序列化1 + 指令2 + 时间戳8 + 数据长度4 + 校验和4）
    public static final int HEADER_LENGTH = 4 + 1 + 1 + 2 + 8 + 4 + 4;

    // 心跳空闲时间（服务端：读空闲10秒，写空闲0，总空闲0；客户端同理）
    public static final int READ_IDLE_SECONDS = 15;
    public static final int WRITE_IDLE_SECONDS = 5;
    public static final int ALL_IDLE_SECONDS = 0;

    // ========== 客户端配置 ==========
    // 写空闲：5秒（每5秒主动发心跳请求）
    public static final int CLIENT_WRITE_IDLE_SECONDS = 5;
    // 读空闲：15秒（15秒没收到服务端响应才断开，给服务端足够回复时间）
    public static final int CLIENT_READ_IDLE_SECONDS = 15;

    // ========== 服务端配置 ==========
    // 读空闲：20秒（20秒没收到客户端消息才断开，比客户端读空闲更宽松）
    public static final int SERVER_READ_IDLE_SECONDS = 20;
    // 写空闲：0（禁用，服务端不主动发心跳）
    public static final int SERVER_WRITE_IDLE_SECONDS = 0;


    // 服务端端口
    public static final int SERVER_PORT = 8888;
    // 服务端地址
    public static final String SERVER_HOST = "127.0.0.1";
}