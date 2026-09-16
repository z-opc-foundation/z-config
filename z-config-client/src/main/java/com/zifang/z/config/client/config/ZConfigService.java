package com.zifang.z.config.client.config;

import com.zifang.util.core.meta.Result;
import com.zifang.z.config.client.config.listener.ZConfigListener;
import com.zifang.z.config.common.model.PollResponse;

public interface ZConfigService {

    void handleServerResponse(PollResponse response);

    void rebuildListenerManager();

    void addListener(String dataId, String group, ZConfigListener listener);

    Result<String> getConfig(String group, String dataId, long timeout);

    /**
     * 保存配置信息
     *
     * @param group   组
     * @param dataId  数据id
     * @param content 数据体
     */
    Result<String> saveConfig(String group, String dataId, String content);
}
