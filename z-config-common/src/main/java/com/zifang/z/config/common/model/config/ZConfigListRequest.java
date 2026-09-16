package com.zifang.z.config.common.model.config;

public class ZConfigListRequest {

    private String nameSpace;
    private String group;
    private String dataId;

    public static ZConfigListRequest of(String nameSpace, String group, String dataId) {
        ZConfigListRequest request = new ZConfigListRequest();
        request.setNameSpace(nameSpace);
        request.setGroup(group);
        request.setDataId(dataId);
        return request;
    }

    public String getNameSpace() {
        return nameSpace;
    }

    public void setNameSpace(String nameSpace) {
        this.nameSpace = nameSpace;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }
}
