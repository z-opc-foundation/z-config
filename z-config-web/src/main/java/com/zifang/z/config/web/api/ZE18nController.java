package com.zifang.z.config.web.api;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.util.core.meta.Result;
import com.zifang.z.config.core.domain.entity.ZE18nMessage;
import com.zifang.z.config.core.domain.mapper.ZE18nMessageMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 国际化消息控制器（对齐 Nacos 的多语言支持）
 */
@RestController
@RequestMapping("/api/i18n")
@Tag(name = "018_国际化消息管理")
public class ZE18nController {

    @Resource
    private ZE18nMessageMapper messageMapper;

    /**
     * 获取指定语言的所有消息
     */
    @GetMapping("/messages")
    @Operation(summary = "001_获取国际化消息")
    public Result<Map<String, String>> getMessages(
            @RequestParam(defaultValue = "zh_CN") String language,
            @RequestParam(required = false) String namespace) {

        QueryWrapper<ZE18nMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("language", language);
        if (namespace != null && !namespace.isEmpty()) {
            queryWrapper.eq("namespace", namespace);
        }

        List<ZE18nMessage> messages = messageMapper.selectList(queryWrapper);
        Map<String, String> result = new HashMap<>();
        for (ZE18nMessage msg : messages) {
            result.put(msg.getMessageKey(), msg.getMessageValue());
        }
        return Result.success(result);
    }

    /**
     * 获取单条消息
     */
    @GetMapping("/message")
    @Operation(summary = "002_获取单条消息")
    public Result<String> getMessage(
            @RequestParam String key,
            @RequestParam(defaultValue = "zh_CN") String language,
            @RequestParam(required = false) String namespace) {

        ZE18nMessage message = messageMapper.selectOne(
                new QueryWrapper<ZE18nMessage>()
                        .eq("message_key", key)
                        .eq("language", language)
                        .eq("namespace", namespace != null ? namespace : ""));

        return message != null ? Result.success(message.getMessageValue()) : Result.fail("消息不存在");
    }

    /**
     * 保存或更新消息
     */
    @PostMapping("/save")
    @Operation(summary = "003_保存消息")
    public Result<String> save(@RequestBody ZE18nMessage message) {
        if (message.getMessageKey() == null || message.getLanguage() == null) {
            return Result.fail("消息键和语言不能为空");
        }

        // 尝试更新
        ZE18nMessage existing = messageMapper.selectOne(
                new QueryWrapper<ZE18nMessage>()
                        .eq("message_key", message.getMessageKey())
                        .eq("language", message.getLanguage())
                        .eq("namespace", message.getNamespace() != null ? message.getNamespace() : ""));

        if (existing != null) {
            existing.setMessageValue(message.getMessageValue());
            existing.setGmtModified(LocalDateTime.now());
            messageMapper.updateById(existing);
        } else {
            message.setGmtCreate(LocalDateTime.now());
            message.setGmtModified(LocalDateTime.now());
            messageMapper.insert(message);
        }
        return Result.success();
    }

    /**
     * 删除消息
     */
    @PostMapping("/delete")
    @Operation(summary = "004_删除消息")
    public Result<String> delete(@RequestParam Long id) {
        messageMapper.deleteById(id);
        return Result.success();
    }
}
