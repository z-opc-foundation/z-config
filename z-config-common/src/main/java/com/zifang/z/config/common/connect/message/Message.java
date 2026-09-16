package com.zifang.z.config.common.connect.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zifang.z.config.common.connect.CommandType;


public abstract class Message {

    // 指令类型（对应 CommandType）
    private short commandType;

    // 消息时间戳（毫秒级）
    private long timestamp;

    // 业务数据（子类扩展）
    private Object data;

    @JsonIgnore
    public abstract CommandType getCommand();

    public short getCommandType() {
        return commandType;
    }

    public void setCommandType(short commandType) {
        this.commandType = commandType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}