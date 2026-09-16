package com.zifang.z.config.client.support;

import com.zifang.util.core.meta.Result;
import com.zifang.util.http.client.HttpExecutionResult;
import com.zifang.util.http.client.HttpExecutor;
import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.define.TypeReference;
import com.zifang.util.json.model.JsonObject;
import com.zifang.z.config.common.model.ZNamingInstance;
import com.zifang.z.config.common.model.naming.ZNamingInstanceDeregisterRequest;
import com.zifang.z.config.common.model.naming.ZNamingInstanceRegisterRequest;
import com.zifang.z.config.common.model.naming.ZNamingSubscribeRequest;
import com.zifang.z.config.common.model.naming.ZNamingUnsubscribeRequest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NamingCallClient {

    private static final Map<String, String> JSON_HEADERS = Collections.singletonMap("Content-Type", "application/json");
    private final String serverHost;
    private final int serverPort;
    private final HttpExecutor httpExecutor = HttpExecutor.getDefault();

    public NamingCallClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    private static String enc(String s) {
        if (s == null) { return ""; }

        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    private String base() {
        return "http://" + serverHost + ":" + serverPort + "/naming";
    }

    // ===================== 服务注册接口 =====================
    public Result<String> registerInstance(ZNamingInstanceRegisterRequest request) {
        return doPost("/registerInstance", request, String.class);
    }

    public Result<String> registerInstanceSimple(String serviceName, String ip, Integer port) {
        String url = base() + "/registerInstance/simple"
                + "?serviceName=" + enc(serviceName) + "&ip=" + enc(ip) + "&port=" + port;
        return doPostRaw(url, String.class);
    }

    public Result<String> registerInstanceWithCluster(String serviceName, String ip, Integer port, String clusterName) {
        String url = base() + "/registerInstance/withCluster"
                + "?serviceName=" + enc(serviceName) + "&ip=" + enc(ip)
                + "&port=" + port + "&clusterName=" + enc(clusterName);
        return doPostRaw(url, String.class);
    }

    // ===================== 服务注销接口 =====================
    public Result<String> deregisterInstance(ZNamingInstanceDeregisterRequest request) {
        return doDelete("/deregisterInstance", request, String.class);
    }

    public Result<String> deregisterInstanceSimple(String serviceName, String ip, Integer port) {
        String url = base() + "/deregisterInstance/simple"
                + "?serviceName=" + enc(serviceName) + "&ip=" + enc(ip) + "&port=" + port;
        return doDeleteRaw(url, String.class);
    }

    public Result<String> deregisterInstanceWithCluster(String serviceName, String ip, Integer port, String clusterName) {
        String url = base() + "/deregisterInstance/withCluster"
                + "?serviceName=" + enc(serviceName) + "&ip=" + enc(ip)
                + "&port=" + port + "&clusterName=" + enc(clusterName);
        return doDeleteRaw(url, String.class);
    }

    // ===================== 服务查询接口 =====================
    public Result<List<ZNamingInstance>> getAllInstances(String serviceName, String group, String namespace) {
        String url = base() + "/getAllInstances"
                + "?serviceName=" + enc(serviceName)
                + "&group=" + enc(group == null ? "DEFAULT_GROUP" : group)
                + "&namespace=" + enc(namespace == null ? "" : namespace);
        return doGet(url, new TypeReference<Result<List<ZNamingInstance>>>() {
        });
    }

    public Result<List<ZNamingInstance>> selectInstances(String serviceName, boolean healthy, String clusterName) {
        String url = base() + "/selectInstances/healthy"
                + "?serviceName=" + enc(serviceName) + "&healthy=" + healthy;
        if (clusterName != null) { url += "&clusterName=" + enc(clusterName); }

        return doGet(url, new TypeReference<Result<List<ZNamingInstance>>>() {
        });
    }

    public Result<ZNamingInstance> selectOneHealthyInstance(String serviceName) {
        String url = base() + "/selectOneHealthyInstance?serviceName=" + enc(serviceName);
        return doGet(url, new TypeReference<Result<ZNamingInstance>>() {
        });
    }

    // ===================== 订阅/取消订阅 =====================
    public Result<String> subscribe(ZNamingSubscribeRequest request) {
        return doPost("/subscribe", request, String.class);
    }

    public Result<String> unsubscribe(ZNamingUnsubscribeRequest request) {
        return doPost("/unsubscribe", request, String.class);
    }

    // ===================== 内部 HTTP 助手 =====================

    private <T> Result<T> doPost(String path, Object body, Class<T> dataClass) {
        return doPostRaw(base() + path, body, dataClass);
    }

    private <T> Result<T> doPostRaw(String url, Class<T> dataClass) {
        return doPostRaw(url, null, dataClass);
    }

    private <T> Result<T> doPostRaw(String url, Object body, Class<T> dataClass) {
        String jsonBody = body != null ? JsonUtil.toJson(body) : "";
        HttpExecutionResult r = httpExecutor.executeByMethodUrl(
                "POST", url, JSON_HEADERS, jsonBody);
        return parse(r, dataClass);
    }

    private <T> Result<T> doDelete(String path, Object body, Class<T> dataClass) {
        String jsonBody = body != null ? JsonUtil.toJson(body) : "";
        HttpExecutionResult r = httpExecutor.executeByMethodUrl(
                "DELETE", base() + path, JSON_HEADERS, jsonBody);
        return parse(r, dataClass);
    }

    private <T> Result<T> doDeleteRaw(String url, Class<T> dataClass) {
        HttpExecutionResult r = httpExecutor.executeByMethodUrl("DELETE", url, null, null);
        return parse(r, dataClass);
    }

    private <T> Result<T> doGet(String url, TypeReference<Result<T>> ref) {
        HttpExecutionResult r = httpExecutor.executeByMethodUrl("GET", url, null, null);
        if (!r.isSuccess()) { return fail(r.getError() == null ? "http error" : r.getError()); }
        return parseJsonResult(r.getBody(), ref);
    }

    private <T> Result<T> parse(HttpExecutionResult r, Class<T> dataClass) {
        if (!r.isSuccess()) { return fail(r.getError() == null ? "http error" : r.getError()); }
        return parseJsonResult(r.getBody(), dataClass);
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
