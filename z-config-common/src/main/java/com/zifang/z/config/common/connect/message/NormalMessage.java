package com.zifang.z.config.common.connect.message;

import com.zifang.z.config.common.connect.CommandType;


public class NormalMessage extends Message {

    // 业务指令（字符串类型，如"USER_LOGIN"、"DATA_QUERY"）
    private String bizCommand;

    // 业务参数（JSON格式字符串或POJO）
    private String params;

    public NormalMessage(String bizCommand, String params) {
        this.bizCommand = bizCommand;
        this.params = params;
        super.setCommandType(CommandType.NORMAL_MESSAGE.getCode());
        super.setTimestamp(System.currentTimeMillis());
    }

    @Override
    public CommandType getCommand() {
        return CommandType.NORMAL_MESSAGE;
    }

    public String getBizCommand() {
        return bizCommand;
    }

    public void setBizCommand(String bizCommand) {
        this.bizCommand = bizCommand;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }
}