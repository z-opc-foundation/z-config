package com.zifang.z.config.client.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.page.Pageable;
import com.zifang.util.json.JsonUtil;
import com.zifang.z.config.common.model.ZConfigDTO;
import com.zifang.z.config.common.model.config.ZConfigPageRequest;
import com.zifang.z.config.common.model.config.ZConfigQueryRequest;
import com.zifang.z.config.common.model.config.ZConfigSaveRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ConfigCallClientTest {

    private HttpServer server;
    private int port;
    private ConfigCallClient client;

    private static String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int b;
        while ((b = is.read()) != -1) buf.write(b);
        return buf.toString("UTF-8");
    }

    private static void sendResponse(HttpExchange exchange, String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    // ===================== getConfig =====================

    @Before
    public void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();

        // --- POST /config/getConfig ---
        server.createContext("/config/getConfig", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                readBody(exchange);
                String json = "{\"success\":true,\"code\":200,\"message\":\"ok\",\"data\":\"server.port=8080\\napp.name=test\"}";
                sendResponse(exchange, json);
            }
        });

        // --- POST /config/saveConfig ---
        server.createContext("/config/saveConfig", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                readBody(exchange);
                String json = "{\"success\":true,\"code\":200,\"message\":\"ok\",\"data\":\"saved\"}";
                sendResponse(exchange, json);
            }
        });

        // --- POST /config/pageConfig ---
        server.createContext("/config/pageConfig", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                readBody(exchange);

                List<ZConfigDTO> records = new ArrayList<>();
                ZConfigDTO dto1 = new ZConfigDTO();
                dto1.setId(1L);
                dto1.setDataId("user-service-dev.yaml");
                dto1.setGroup("DEFAULT_GROUP");
                dto1.setContent("server.port=8080");
                dto1.setAppName("user-service");
                dto1.setNamespace("dev");
                records.add(dto1);

                ZConfigDTO dto2 = new ZConfigDTO();
                dto2.setId(2L);
                dto2.setDataId("order-service-dev.yaml");
                dto2.setGroup("DEFAULT_GROUP");
                dto2.setContent("server.port=8081");
                dto2.setAppName("order-service");
                dto2.setNamespace("dev");
                records.add(dto2);

                String recordsJson = JsonUtil.toJson(records);
                String pageableJson = "{\"records\":" + recordsJson
                        + ",\"total\":10,\"size\":20,\"current\":1}";

                String json = "{\"success\":true,\"code\":200,\"message\":\"ok\","
                        + "\"data\":" + pageableJson + "}";
                sendResponse(exchange, json);
            }
        });

        server.setExecutor(null);
        server.start();

        client = new ConfigCallClient("127.0.0.1", port);
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    // ===================== saveConfig =====================

    @Test
    public void testGetConfig() {
        ZConfigQueryRequest req = ZConfigQueryRequest.of("dev", "DEFAULT_GROUP", "user-service-dev.yaml");

        Result<String> result = client.getConfig(req);
        assertTrue("getConfig should succeed", result.isSuccess());
        assertNotNull("data should not be null", result.getData());
        assertTrue("config content should contain server.port", result.getData().contains("server.port=8080"));
    }

    @Test
    public void testGetConfigWithNulls() {
        ZConfigQueryRequest req = new ZConfigQueryRequest();
        req.setDataId("test.yaml");

        Result<String> result = client.getConfig(req);
        assertTrue("getConfig with partial fields should succeed", result.isSuccess());
    }

    // ===================== pageConfig =====================

    @Test
    public void testSaveConfig() {
        ZConfigSaveRequest req = new ZConfigSaveRequest();
        req.setDataId("user-service-dev.yaml");
        req.setGroup("DEFAULT_GROUP");
        req.setContent("server.port=9090");
        req.setNamespace("dev");
        req.setAppName("user-service");
        req.setConfigDesc("test config");

        Result<String> result = client.saveConfig(req);
        assertTrue("saveConfig should succeed", result.isSuccess());
        assertEquals("saved", result.getData());
    }

    @Test
    public void testSaveConfigMinimal() {
        ZConfigSaveRequest req = new ZConfigSaveRequest();
        req.setDataId("minimal.yaml");
        req.setContent("key=value");

        Result<String> result = client.saveConfig(req);
        assertTrue("saveConfig minimal should succeed", result.isSuccess());
    }

    // ===================== 错误场景 =====================

    @Test
    public void testPageConfig() {
        ZConfigPageRequest req = ZConfigPageRequest.of("dev", "DEFAULT_GROUP", null);

        Result<Pageable<ZConfigDTO>> result = client.pageConfig(req);
        assertTrue("pageConfig should succeed", result.isSuccess());
        assertNotNull("pageable data should not be null", result.getData());
        assertEquals("total should be 10", 10L, result.getData().getTotal());
        assertEquals("size should be 20", 20L, result.getData().getSize());
        assertEquals("current should be 1", 1L, result.getData().getCurrent());

        List<ZConfigDTO> records = result.getData().getRecords();
        assertNotNull("records should not be null", records);
        assertEquals("should have 2 records", 2, records.size());

        ZConfigDTO first = records.get(0);
        assertEquals(Long.valueOf(1L), first.getId());
        assertEquals("user-service-dev.yaml", first.getDataId());
        assertEquals("DEFAULT_GROUP", first.getGroup());
        assertEquals("user-service", first.getAppName());
    }

    @Test
    public void testPageConfigWithSearch() {
        ZConfigPageRequest req = ZConfigPageRequest.of("dev", null, null);
        req.setSearch("user");

        Result<Pageable<ZConfigDTO>> result = client.pageConfig(req);
        assertTrue("pageConfig with search should succeed", result.isSuccess());
    }

    // ===================== helpers =====================

    @Test
    public void testConnectionFailure() {
        ConfigCallClient deadClient = new ConfigCallClient("127.0.0.1", 1);
        Result<String> result = deadClient.getConfig(ZConfigQueryRequest.of("dev", "G", "test.yaml"));
        assertFalse("should fail when server unreachable", result.isSuccess());
    }

    @Test
    public void testMalformedResponse() throws IOException {
        HttpServer badServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int badPort = badServer.getAddress().getPort();
        badServer.createContext("/config/getConfig", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                sendResponse(exchange, "this is not json");
            }
        });
        badServer.start();

        try {
            ConfigCallClient badClient = new ConfigCallClient("127.0.0.1", badPort);
            Result<String> result = badClient.getConfig(ZConfigQueryRequest.of("dev", "G", "x.yaml"));
            assertFalse("should fail on malformed response", result.isSuccess());
            assertTrue("message should mention parse", result.getMessage().contains("parse"));
        } finally {
            badServer.stop(0);
        }
    }
}
