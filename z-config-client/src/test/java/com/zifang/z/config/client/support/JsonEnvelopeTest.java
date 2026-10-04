package com.zifang.z.config.client.support;

import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.define.TypeReference;
import com.zifang.util.json.model.JsonObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * 响应信封 {@code data} 取值逻辑。
 *
 * <p>钉的是两个真实缺陷（旧实现用 {@code json.getString("data")} 取值）：</p>
 * <ul>
 *   <li>{@code data} 为对象/数组/数字时 {@code getString} 返回 {@code null} ⇒ 客户端返回
 *       {@code Result.success()} 但 {@code data == null}，<b>静默丢数据</b>；</li>
 *   <li>{@code data} 为裸字符串时该字符串被再喂一次 {@code fromJson}，而 {@code "saved"}
 *       这类裸文本不是合法 JSON，抛 {@code Rule 'json' failed to match}，
 *       <b>所有字符串型响应一律解析失败</b>。</li>
 * </ul>
 *
 * <p>断言用字面量，不用被测代码的输出推导期望值。</p>
 */
public class JsonEnvelopeTest {

    /** 表单：data 的四种形态 + 缺失，全部是线上真实存在的响应形状。 */
    @Test
    public void testDataAsPlainStringIsTerminalValue() {
        JsonObject json = JsonUtil.parseObject("{\"success\":true,\"data\":\"saved\"}");
        assertEquals("saved", JsonEnvelope.readData(json, String.class));
    }

    @Test
    public void testDataAsStringWithPunctuation() {
        JsonObject json = JsonUtil.parseObject("{\"success\":true,\"data\":\"server.port=8080\\napp.name=test\"}");
        assertEquals("server.port=8080\napp.name=test", JsonEnvelope.readData(json, String.class));
    }

    /** 旧实现在这里返回 null：调用方拿到 success=true 却没有数据。 */
    @Test
    public void testDataAsObjectIsNotSilentlyDropped() {
        JsonObject json = JsonUtil.parseObject("{\"success\":true,\"data\":{\"ip\":\"10.0.0.1\",\"port\":8080}}");
        Bean bean = JsonEnvelope.readData(json, Bean.class);
        assertEquals("10.0.0.1", bean.ip);
        assertEquals(Integer.valueOf(8080), bean.port);
    }

    @Test
    public void testDataAsArrayKeepsGenericElementType() {
        JsonObject json = JsonUtil.parseObject("{\"success\":true,\"data\":[{\"ip\":\"1.2.3.4\",\"port\":80}]}");
        List<Bean> list = JsonEnvelope.readData(json, new TypeReference<List<Bean>>() {});
        assertEquals(1, list.size());
        assertEquals("1.2.3.4", list.get(0).ip);
        assertEquals(Integer.valueOf(80), list.get(0).port);
    }

    @Test
    public void testDataAsNumber() {
        JsonObject json = JsonUtil.parseObject("{\"success\":true,\"data\":42}");
        assertEquals(Integer.valueOf(42), JsonEnvelope.readData(json, Integer.class));
    }

    @Test
    public void testMissingDataYieldsNull() {
        JsonObject json = JsonUtil.parseObject("{\"success\":true,\"message\":\"ok\"}");
        assertNull(JsonEnvelope.readData(json, String.class));
        assertNull(JsonEnvelope.readData(json, Bean.class));
    }

    /** data 是裸文本却要对象：必须显式失败，不能静默返回 null。 */
    @Test(expected = RuntimeException.class)
    public void testBareStringForObjectTargetFailsLoudly() {
        JsonObject json = JsonUtil.parseObject("{\"success\":true,\"data\":\"not-json\"}");
        JsonEnvelope.readData(json, new TypeReference<Bean>() {});
    }

    /**
     * 泛型目标为 String 时，data 的裸文本就是终值。
     * 这条钉的是 TypeReference 分支里的 String 短路——没有它时该分支无测试覆盖，
     * 变异验证会显示全绿（误以为有牙齿）。
     */
    @Test
    public void testGenericStringTargetIsTerminalValue() {
        JsonObject json = JsonUtil.parseObject("{\"success\":true,\"data\":\"saved\"}");
        assertEquals("saved", JsonEnvelope.readData(json, new TypeReference<String>() {}));
    }

    public static class Bean {
        private String ip;
        private Integer port;

        public String getIp() { return ip; }
        public Integer getPort() { return port; }
    }
}
