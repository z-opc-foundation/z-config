package com.zifang.z.config.common.connect;

public class BizCommandType {

    public static final String LISTENER_CONFIG_REQUEST = "LISTENER_CONFIG_REQUEST";
    public static final String LISTENER_CONFIG_RESPONSE = "LISTENER_CONFIG_RESPONSE";

    /**
     * 心跳请求（客户端 -> 服务端）
     */
    public static final String HEARTBEAT_REQUEST = "HEARTBEAT_REQUEST";

    /**
     * 心跳响应（服务端 -> 客户端）
     */
    public static final String HEARTBEAT_RESPONSE = "HEARTBEAT_RESPONSE";

}
