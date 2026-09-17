package com.zifang.z.config.core.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.config.core.domain.entity.ZNamespace;
import org.apache.ibatis.annotations.Mapper;

/**
 * 命名空间表 Mapper
 */
@Mapper
public interface ZNamespaceMapper extends BaseMapper<ZNamespace> {
}
