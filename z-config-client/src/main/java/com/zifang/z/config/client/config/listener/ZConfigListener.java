package com.zifang.z.config.client.config.listener;

public interface ZConfigListener {

    /**
     * 监听回调, 只有的确存在更新数据才会调用此方法
     *
     * @param newConfig 捕获的新数据
     */
    void receiveConfigInfo(String newConfig);
}
