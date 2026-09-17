package com.zifang.z.config.core.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zifang.z.config.core.domain.entity.ZConfigAudit;

/**
 * 配置审计日志服务接口
 */
public interface IZConfigAuditService extends IService<ZConfigAudit> {

    /**
     * 记录配置变更审计日志
     *
     * @param dataId    配置ID
     * @param group     分组
     * @param namespace 命名空间
     * @param action    操作类型（CREATE/UPDATE/DELETE/ROLLBACK等）
     * @param oldContent 变更前内容
     * @param newContent 变更后内容
     * @param srcUser   操作人
     * @param srcIp     操作IP
     * @param result    操作结果
     * @param errorMsg  错误信息（失败时）
     */
    void audit(String dataId, String group, String namespace, String action,
               String oldContent, String newContent,
               String srcUser, String srcIp, String result, String errorMsg);
}
