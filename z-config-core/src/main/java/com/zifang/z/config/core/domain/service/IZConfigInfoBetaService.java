package com.zifang.z.config.core.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zifang.z.config.core.domain.entity.ZConfigInfoBeta;

/**
 * Beta环境配置表 服务接口
 *
 * @author auto-generated
 */
public interface IZConfigInfoBetaService extends IService<ZConfigInfoBeta> {

    /**
     * 根据配置三元组查询 Beta 配置
     *
     * @param namespace 命名空间
     * @param group     分组
     * @param dataId    配置ID
     * @return Beta 配置信息，不存在时返回 null
     */
    ZConfigInfoBeta queryBetaConfig(String namespace, String group, String dataId);
}
