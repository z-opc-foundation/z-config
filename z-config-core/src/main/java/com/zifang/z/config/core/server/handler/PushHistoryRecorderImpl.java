package com.zifang.z.config.core.server.handler;

import com.zifang.z.config.core.domain.entity.ZConfigPushHistory;
import com.zifang.z.config.core.domain.mapper.ZConfigPushHistoryMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;

/**
 * 推送历史记录器实现
 * 将推送通知记录到数据库，供查询和审计使用
 */
@Component
public class PushHistoryRecorderImpl implements ServerBusinessHandler.PushHistoryRecorder {

    private static final Logger log = LogManager.getLogger(PushHistoryRecorderImpl.class);

    @Autowired
    private ZConfigPushHistoryMapper pushHistoryMapper;

    @PostConstruct
    public void init() {
        // 注册到 ServerBusinessHandler
        ServerBusinessHandler.setPushHistoryRecorder(this);
        log.info("推送历史记录器已初始化");
    }

    @Override
    public void record(String dataId, String group, String namespace, String clientIp,
                       String pushType, String pushResult, String newMd5) {
        try {
            ZConfigPushHistory history = new ZConfigPushHistory();
            history.setDataId(dataId);
            history.setGroupName(group);
            history.setNamespace(namespace);
            history.setClientIp(clientIp);
            history.setPushType(pushType);
            history.setPushResult(pushResult);
            history.setNewMd5(newMd5);
            history.setPushTime(LocalDateTime.now());
            pushHistoryMapper.insert(history);
        } catch (Exception e) {
            log.warn("推送历史记录失败: dataId={}, clientIp={}", dataId, clientIp, e);
        }
    }
}
