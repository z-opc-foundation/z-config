package com.zifang.z.config.client.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.zifang.util.core.meta.Result;
import com.zifang.util.json.JsonUtil;
import com.zifang.z.config.common.model.ZNamingInstance;
import com.zifang.z.config.common.model.naming.ZNamingInstanceRegisterRequest;
import com.zifang.z.config.common.model.naming.ZNamingSubscribeRequest;
import com.zifang.z.config.common.model.naming.ZNamingUnsubscribeRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class NamingCallClientTest {

    private HttpServer server;
    private int port;
    private NamingCallClient client;

    private static String successResult(String data) {
        return "{\"success\":true,\"code\":200,\"message\":\"ok\",\"data\":" + JsonUtil.toJson(data) + "}";
    }

    private static void sendResponse(HttpExchange exchange, String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    // ===================== 注册测试 =====================

    @Before
    public void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();

        // POST /naming/registerInstance
        server.createContext("/naming/registerInstance", new MockHandler(successResult("registered")));

        // POST /naming/registerInstance/simple
        server.createContext("/naming/registerInstance/simple", new MockHandler(successResult("simple-registered")));

        // POST /naming/registerInstance/withCluster
        server.createContext("/naming/registerInstance/withCluster", new MockHandler(successResult("cluster-registered")));

        // DELETE /naming/deregisterInstance
        server.createContext("/naming/deregisterInstance/simple", new MockHandler(successResult("deregistered")));
        server.createContext("/naming/deregisterInstance/withCluster", new MockHandler(successResult("cluster-deregistered")));
        server.createContext("/naming/deregisterInstance", new MockHandler(successResult("deregistered")));

        // GET /naming/getAllInstances
        server.createContext("/naming/getAllInstances", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                List<ZNamingInstance> instances = new ArrayList<>();
                ZNamingInstance inst = new ZNamingInstance();
                inst.setServiceName("test-service");
                inst.setIp("192.168.1.10");
                inst.setPort(8080);
                inst.setHealthy(true);
                inst.setClusterName("DEFAULT");
                inst.setInstanceId("1@@192.168.1.10:8080");
                instances.add(inst);

                String json = "{\"success\":true,\"code\":200,\"message\":\"ok\",\"data\":" + JsonUtil.toJson(instances) + "}";
                sendResponse(exchange, json);
            }
        });

        // GET /naming/selectInstances/healthy
        server.createContext("/naming/selectInstances/healthy", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                List<ZNamingInstance> instances = new ArrayList<>();
                ZNamingInstance inst = new ZNamingInstance();
                inst.setServiceName("test-service");
                inst.setIp("192.168.1.20");
                inst.setPort(9090);
                inst.setHealthy(true);
                inst.setClusterName("SHANGHAI");
                inst.setInstanceId("2@@192.168.1.20:9090");
                instances.add(inst);

                String json = "{\"success\":true,\"code\":200,\"message\":\"ok\",\"data\":" + JsonUtil.toJson(instances) + "}";
                sendResponse(exchange, json);
            }
        });

        // GET /naming/selectOneHealthyInstance
        server.createContext("/naming/selectOneHealthyInstance", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                ZNamingInstance inst = new ZNamingInstance();
                inst.setServiceName("test-service");
                inst.setIp("10.0.0.1");
                inst.setPort(8080);
                inst.setHealthy(true);
                inst.setInstanceId("3@@10.0.0.1:8080");

                String json = "{\"success\":true,\"code\":200,\"message\":\"ok\",\"data\":" + JsonUtil.toJson(inst) + "}";
                sendResponse(exchange, json);
            }
        });

        // POST /naming/subscribe
        server.createContext("/naming/subscribe", new MockHandler(successResult("subscribed")));

        // POST /naming/unsubscribe
        server.createContext("/naming/unsubscribe", new MockHandler(successResult("unsubscribed")));

        server.setExecutor(null);
        server.start();

        client = new NamingCallClient("127.0.0.1", port);
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void testRegisterInstance() {
        ZNamingInstanceRegisterRequest req = new ZNamingInstanceRegisterRequest();
        req.setServiceName("order-service");
        req.setIp("192.168.1.1");
        req.setPort(8080);

        Result<String> result = client.registerInstance(req);
        assertTrue("registerInstance should succeed", result.isSuccess());
        assertEquals("registered", result.getData());
    }

    // ===================== 注销测试 =====================

    @Test
    public void testRegisterInstanceSimple() {
        Result<String> result = client.registerInstanceSimple("order-service", "192.168.1.1", 8080);
        assertTrue("registerInstanceSimple should succeed", result.isSuccess());
        assertEquals("simple-registered", result.getData());
    }

    @Test
    public void testRegisterInstanceWithCluster() {
        Result<String> result = client.registerInstanceWithCluster("order-service", "192.168.1.1", 8080, "SHANGHAI");
        assertTrue("registerInstanceWithCluster should succeed", result.isSuccess());
        assertEquals("cluster-registered", result.getData());
    }

    // ===================== 查询测试 =====================

    @Test
    public void testDeregisterInstanceSimple() {
        Result<String> result = client.deregisterInstanceSimple("order-service", "192.168.1.1", 8080);
        assertTrue("deregisterInstanceSimple should succeed", result.isSuccess());
        assertEquals("deregistered", result.getData());
    }

    @Test
    public void testDeregisterInstanceWithCluster() {
        Result<String> result = client.deregisterInstanceWithCluster("order-service", "192.168.1.1", 8080, "SHANGHAI");
        assertTrue("deregisterInstanceWithCluster should succeed", result.isSuccess());
        assertEquals("cluster-deregistered", result.getData());
    }

    @Test
    public void testGetAllInstances() {
        Result<List<ZNamingInstance>> result = client.getAllInstances("test-service", null, null);
        assertTrue("getAllInstances should succeed", result.isSuccess());
        assertNotNull("data should not be null", result.getData());
        assertEquals("should have 1 instance", 1, result.getData().size());

        ZNamingInstance inst = result.getData().get(0);
        assertEquals("test-service", inst.getServiceName());
        assertEquals("192.168.1.10", inst.getIp());
        assertEquals(Integer.valueOf(8080), inst.getPort());
        assertTrue(inst.getHealthy());
    }

    @Test
    public void testGetAllInstancesWithGroupAndNamespace() {
        Result<List<ZNamingInstance>> result = client.getAllInstances("test-service", "MY_GROUP", "dev");
        assertTrue("getAllInstances with group/ns should succeed", result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    public void testSelectInstancesHealthy() {
        Result<List<ZNamingInstance>> result = client.selectInstances("test-service", true, null);
        assertTrue("selectInstances should succeed", result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals("SHANGHAI", result.getData().get(0).getClusterName());
    }

    // ===================== 订阅/取消订阅 =====================

    @Test
    public void testSelectInstancesWithCluster() {
        Result<List<ZNamingInstance>> result = client.selectInstances("test-service", true, "SHANGHAI");
        assertTrue("selectInstances with cluster should succeed", result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    public void testSelectOneHealthyInstance() {
        Result<ZNamingInstance> result = client.selectOneHealthyInstance("test-service");
        assertTrue("selectOneHealthyInstance should succeed", result.isSuccess());
        assertNotNull("instance should not be null", result.getData());
        assertEquals("10.0.0.1", result.getData().getIp());
        assertEquals(Integer.valueOf(8080), result.getData().getPort());
    }

    // ===================== 错误场景 =====================

    @Test
    public void testSubscribe() {
        ZNamingSubscribeRequest req = new ZNamingSubscribeRequest();
        req.setSubscribeServiceName("test-service");
        req.setConsumerIp("192.168.1.100");
        req.setConsumerPort(8081);

        Result<String> result = client.subscribe(req);
        assertTrue("subscribe should succeed", result.isSuccess());
        assertEquals("subscribed", result.getData());
    }

    // ===================== helpers =====================

    @Test
    public void testUnsubscribe() {
        ZNamingUnsubscribeRequest req = new ZNamingUnsubscribeRequest();
        req.setSubscribeServiceName("test-service");
        req.setConsumerIp("192.168.1.100");
        req.setConsumerPort(8081);

        Result<String> result = client.unsubscribe(req);
        assertTrue("unsubscribe should succeed", result.isSuccess());
        assertEquals("unsubscribed", result.getData());
    }

    @Test
    public void testConnectionFailure() {
        NamingCallClient deadClient = new NamingCallClient("127.0.0.1", 1);
        Result<String> result = deadClient.registerInstanceSimple("svc", "1.2.3.4", 80);
        assertFalse("should fail when server unreachable", result.isSuccess());
    }

    static class MockHandler implements HttpHandler {
        private final String responseJson;

        MockHandler(String responseJson) {
            this.responseJson = responseJson;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                java.io.InputStream is = exchange.getRequestBody();
                java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
                int b;
                while ((b = is.read()) != -1) buf.write(b);
            } catch (Exception ignored) {
            }

            byte[] bytes = responseJson.getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }
}
