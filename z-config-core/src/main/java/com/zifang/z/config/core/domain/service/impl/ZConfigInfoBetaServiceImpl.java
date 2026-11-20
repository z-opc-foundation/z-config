package com.zifang.z.config.core.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zifang.z.config.core.domain.entity.ZConfigInfoBeta;
import com.zifang.z.config.core.domain.mapper.ZConfigInfoBetaMapper;
import com.zifang.z.config.core.domain.service.IZConfigInfoBetaService;
import com.zifang.z.meta.api.annotation.BizMethod;
import org.springframework.stereotype.Service;

/**
 * Beta环境配置表 服务实现类
 *
 * @author auto-generated
 */
@Service
public class ZConfigInfoBetaServiceImpl extends ServiceImpl<ZConfigInfoBetaMapper, ZConfigInfoBeta> implements IZConfigInfoBetaService {

    @Override
    @BizMethod(name = "queryBetaConfig", module = "z-config", description = "", operation = BizMethod.Operation.QUERY)
    public ZConfigInfoBeta queryBetaConfig(String namespace, String group, String dataId) {
        return getOne(new QueryWrapper<ZConfigInfoBeta>()
                .eq("data_id", dataId)
                .eq("`group`", group)
                .eq("namespace", namespace != null ? namespace : ""));
    }
}



