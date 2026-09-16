package com.zifang.z.config.common.connect;


public enum CommandType {
    // 心跳请求（1）、心跳响应（2）、普通消息（3）、普通响应（4）
    UN_KNOWN((short) -1),

    HEARTBEAT_REQUEST((short) 1),
    HEARTBEAT_RESPONSE((short) 2),

    NORMAL_MESSAGE((short) 3),
    NORMAL_RESPONSE((short) 4);


    private final short code;

    CommandType(short code) {
        this.code = code;
    }

    // 根据指令码获取枚举
    public static CommandType getByCode(short code) {
        for (CommandType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return UN_KNOWN;
    }

    public short getCode() {
        return code;
    }
}