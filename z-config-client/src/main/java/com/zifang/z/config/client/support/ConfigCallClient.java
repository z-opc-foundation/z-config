package com.zifang.z.config.client.support;

import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.page.Pageable;
import com.zifang.util.http.client.HttpExecutionResult;
import com.zifang.util.http.client.HttpExecutor;
import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.define.TypeReference;
import com.zifang.util.json.model.JsonObject;
import com.zifang.z.config.common.model.ZConfigDTO;
import com.zifang.z.config.common.model.config.ZConfigPageRequest;
import com.zifang.z.config.common.model.config.ZConfigQueryRequest;
import com.zifang.z.config.common.model.config.ZConfigSaveRequest;

import java.util.Collections;
import java.util.Map;

public class ConfigCallClient {

    private static final Map<String, String> JSON_HEADERS = Collections.singletonMap("Content-Type", "application/json");
    private final String serverHost;
    private final int serverPort;
    private final HttpExecutor httpExecutor = HttpExecutor.getDefault();

    public ConfigCallClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    private String base() {
        return "http://" + serverHost + ":" + serverPort + "/config";
    }

    public Result<String> getConfig(ZConfigQueryRequest request) {
        return doPost("/getConfig", request, String.class);
    }

    public Result<String> saveConfig(ZConfigSaveRequest request) {
        return doPost("/saveConfig", request, String.class);
    }

    public Result<Pageable<ZConfigDTO>> pageConfig(ZConfigPageRequest request) {
        return doPost("/pageConfig", request,
                new TypeReference<Result<Pageable<ZConfigDTO>>>() {
                });
    }

    private <T> Result<T> doPost(String path, Object body, Class<T> dataClass) {
        String jsonBody = JsonUtil.toJson(body);
        HttpExecutionResult r = httpExecutor.executeByMethodUrl(
                "POST", base() + path, JSON_HEADERS, jsonBody);
        if (!r.isSuccess()) { return fail(r.getError() == null ? "http error" : r.getError()); }
        return parseJsonResult(r.getBody(), dataClass);
    }

    private <T> Result<T> doPost(String path, Object body, TypeReference<Result<T>> ref) {
        String jsonBody = JsonUtil.toJson(body);
        HttpExecutionResult r = httpExecutor.executeByMethodUrl(
                "POST", base() + path, JSON_HEADERS, jsonBody);
        if (!r.isSuccess()) { return fail(r.getError() == null ? "http error" : r.getError()); }
        return parseJsonResult(r.getBody(), ref);
    }

    private <T> Result<T> parseJsonResult(String body, Class<T> dataClass) {
        if (body == null || body.isEmpty()) { return fail("empty response"); }
        try {
            JsonObject json = JsonUtil.parseObject(body);
            Boolean success = json.getBoolean("success");
            if (success == null || !success) {
                String msg = json.getString("message");
                return Result.fail(msg != null ? msg : "request failed");
            }
            String dataStr = json.getString("data");
            T data = dataStr != null ? JsonUtil.fromJson(dataStr, dataClass) : null;
            return Result.<T>success().data(data);
        } catch (Exception e) {
            return fail("parse failed: " + e.getMessage());
        }
    }

    private <T> Result<T> parseJsonResult(String body, TypeReference<Result<T>> ref) {
        if (body == null || body.isEmpty()) { return fail("empty response"); }
        try {
            JsonObject json = JsonUtil.parseObject(body);
            Boolean success = json.getBoolean("success");
            if (success == null || !success) {
                String msg = json.getString("message");
                return Result.fail(msg != null ? msg : "request failed");
            }
            String dataStr = json.getString("data");
            T data;
            if (dataStr != null) {
                data = JsonUtil.fromJson(dataStr, ref).getData();
            } else {
                data = null;
            }
            return Result.<T>success().data(data);
        } catch (Exception e) {
            return fail("parse failed: " + e.getMessage());
        }
    }

    private <T> Result<T> fail(String msg) {
        return Result.fail(msg);
    }
}
