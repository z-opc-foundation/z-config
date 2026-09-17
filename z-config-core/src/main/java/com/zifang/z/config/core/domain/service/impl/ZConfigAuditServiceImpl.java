package com.zifang.z.config.core.domain.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zifang.util.core.encrypt.MD5Utils;
import com.zifang.z.config.core.domain.entity.ZConfigAudit;
import com.zifang.z.config.core.domain.mapper.ZConfigAuditMapper;
import com.zifang.z.config.core.domain.service.IZConfigAuditService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
public class ZConfigAuditServiceImpl extends ServiceImpl<ZConfigAuditMapper, ZConfigAudit> implements IZConfigAuditService {

    private static final Logger log = LogManager.getLogger(ZConfigAuditServiceImpl.class);

    @Override
    public void audit(String dataId, String group, String namespace, String action,
                      String oldContent, String newContent,
                      String srcUser, String srcIp, String result, String errorMsg) {
        try {
            ZConfigAudit audit = new ZConfigAudit();
            audit.setDataId(dataId);
            audit.setGroupName(group);
            audit.setNamespace(namespace);
            audit.setAction(action);
            audit.setOldContent(oldContent);
            audit.setNewContent(newContent);
            if (oldContent != null) {
                audit.setOldMd5(MD5Utils.encrypt(oldContent.getBytes(StandardCharsets.UTF_8)));
            }
            if (newContent != null) {
                audit.setNewMd5(MD5Utils.encrypt(newContent.getBytes(StandardCharsets.UTF_8)));
            }
            audit.setSrcUser(srcUser);
            audit.setSrcIp(srcIp);
            audit.setResult(result);
            audit.setErrorMsg(errorMsg);
            audit.setGmtCreate(LocalDateTime.now());
            save(audit);
        } catch (Exception e) {
            log.error("审计日志记录失败: dataId={}, action={}", dataId, action, e);
        }
    }
}
