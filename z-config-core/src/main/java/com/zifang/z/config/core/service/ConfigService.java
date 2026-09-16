package com.zifang.z.config.core.service;

import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.page.Pageable;
import com.zifang.z.config.common.model.ZConfigDTO;
import com.zifang.z.config.common.model.config.ZConfigListRequest;
import com.zifang.z.config.common.model.config.ZConfigPageRequest;
import com.zifang.z.config.common.model.config.ZConfigQueryRequest;
import com.zifang.z.config.common.model.config.ZConfigSaveRequest;

import java.util.List;

public interface ConfigService {

    Result<String> saveConfig(ZConfigSaveRequest zConfigSaveRequest);

    Result<String> getConfig(ZConfigQueryRequest zConfigRequest);

    Result<Pageable<ZConfigDTO>> pageConfig(ZConfigPageRequest configRequest);

    Result<List<ZConfigDTO>> listConfig(ZConfigListRequest request);

    Result<String> deleteConfig(ZConfigQueryRequest request);
}
