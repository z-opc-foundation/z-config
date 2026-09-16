package com.zifang.z.config.common.model.config;

import com.zifang.util.core.meta.page.PageRequest;

public class ZConfigPageRequest extends PageRequest {

    private String nameSpace;
    private String group;
    private String dataId;

    private String search;

    private String appName;

    public static ZConfigPageRequest of(String nameSpace, String group, String dataId) {
        ZConfigPageRequest request = new ZConfigPageRequest();
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

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }
}
