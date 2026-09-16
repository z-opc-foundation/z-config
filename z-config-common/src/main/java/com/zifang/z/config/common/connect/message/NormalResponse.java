package com.zifang.z.config.common.connect.message;

import com.zifang.z.config.common.connect.CommandType;


public class NormalResponse extends Message {

    private boolean success;

    private String message;

    private String result;

    private String bizCommandType;

    public NormalResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        super.setCommandType(CommandType.NORMAL_RESPONSE.getCode());
        super.setTimestamp(System.currentTimeMillis());
    }

    public NormalResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        super.setCommandType(CommandType.NORMAL_RESPONSE.getCode());
        super.setTimestamp(System.currentTimeMillis());
    }

    public NormalResponse() {
    }

    @Override
    public CommandType getCommand() {
        return CommandType.getByCode(super.getCommandType());
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getBizCommandType() {
        return bizCommandType;
    }

    public void setBizCommandType(String bizCommandType) {
        this.bizCommandType = bizCommandType;
    }
}