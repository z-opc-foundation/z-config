package com.zifang.z.config.common.connect.message;

import com.zifang.z.config.common.connect.CommandType;

public class HeartbeatMessage extends Message {


    // 心跳消息无额外数据，可扩展添加设备ID等
    public HeartbeatMessage(CommandType commandType) {
        super.setCommandType(commandType.getCode());
        super.setTimestamp(System.currentTimeMillis());
    }

    @Override
    public CommandType getCommand() {
        return CommandType.getByCode(super.getCommandType());
    }
}