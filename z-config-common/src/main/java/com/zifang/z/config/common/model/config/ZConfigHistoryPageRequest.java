package com.zifang.z.config.common.model.config;

import com.zifang.util.core.meta.page.PageRequest;

/**
 * 配置历史分页请求。
 * 继承 z-util-core PageRequest (current/size)，复用排序能力。
 */
public class ZConfigHistoryPageRequest extends PageRequest {

    private String search;
    private Long namespace;
    private String group;

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public Long getNamespace() {
        return namespace;
    }

    public void setNamespace(Long namespace) {
        this.namespace = namespace;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}
