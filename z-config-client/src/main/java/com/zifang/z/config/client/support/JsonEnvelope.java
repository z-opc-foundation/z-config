package com.zifang.z.config.client.support;

import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.define.TypeReference;
import com.zifang.util.json.model.JsonArray;
import com.zifang.util.json.model.JsonObject;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 统一响应信封 {@code {"success":..,"code":..,"message":..,"data":..}} 的 {@code data} 取值逻辑。
 *
 * <p><b>为什么不能用 {@code json.getString("data")}</b>：它只在 {@code data} 恰好是 JSON 字符串时
 * 才有值，{@code data} 为对象/数组/数字时一律返回 {@code null}。两个客户端原本都这么写，后果有两类，
 * 均已实测：</p>
 * <ul>
 *   <li>{@code data} 是对象或数组 → 客户端返回 {@code Result.success()} 但 {@code data == null}，
 *       <b>静默丢数据</b>：调用方拿到"成功"却什么也没有。</li>
 *   <li>{@code data} 是裸字符串 → 该字符串被再喂一次 {@code fromJson}，而 {@code "saved"} 这种
 *       裸文本不是合法 JSON，抛 {@code Rule 'json' failed to match}，
 *       <b>所有字符串型响应一律解析失败</b>。</li>
 * </ul>
 *
 * <p>正确做法是按 JSON 原生语义取值：{@code data} 是什么就按什么来，字符串不再二次解析。</p>
 *
 * @author zifang
 */
final class JsonEnvelope {

    private JsonEnvelope() {
    }

    /**
     * 从信封里取 {@code data} 并转成指定类型。
     *
     * @param json     已解析的信封
     * @param dataClass {@code data} 的目标类型
     * @return 转换后的 {@code data}；{@code data} 缺失或为 null 时返回 null
     */
    static <T> T readData(JsonObject json, Class<T> dataClass) {
        Object raw = json.get("data");
        if (raw == null) {
            return null;
        }
        if (raw instanceof String) {
            // 裸文本已是终值，不再二次解析（这正是旧实现对所有字符串型响应失败的原因）
            if (dataClass == String.class) {
                return cast(raw);
            }
            return JsonUtil.fromJson((String) raw, dataClass);
        }
        if (raw instanceof JsonObject || raw instanceof JsonArray) {
            // JsonObject/JsonArray 的 toString() 产出的就是合法 JSON，回灌即可保住泛型信息
            if (dataClass == String.class) {
                return cast(raw.toString());
            }
            return JsonUtil.fromJson(raw.toString(), dataClass);
        }
        // 数字、布尔等标量
        if (dataClass == String.class) {
            return cast(String.valueOf(raw));
        }
        return JsonUtil.fromJson(JsonUtil.toJson(raw), dataClass);
    }

    /**
     * 从信封里取 {@code data} 并转成带泛型的指定类型。
     * <p>注意泛型参数描述的是 <b>{@code data} 本身</b>（如 {@code TypeReference&lt;Pageable&lt;ZConfigDTO&gt;&gt;}），
     * 不是整个信封——旧实现传的是 {@code Result<T>} 却拿去解析 {@code data}，属类型混淆。</p>
     *
     * @param json 已解析的信封
     * @param ref  {@code data} 的目标泛型类型
     * @return 转换后的 {@code data}；{@code data} 缺失或为 null 时返回 null
     */
    static <T> T readData(JsonObject json, TypeReference<T> ref) {
        Object raw = json.get("data");
        if (raw == null) {
            return null;
        }
        if (raw instanceof String) {
            if (String.class.equals(rawTypeOf(ref))) {
                return cast(raw);
            }
            // 目标不是 String 却收到裸文本：让 fromJson 抛出来，由调用方转成显式失败，
            // 不静默返回 null——那正是本次要修掉的行为。
            return JsonUtil.fromJson((String) raw, ref);
        }
        return JsonUtil.fromJson(raw.toString(), ref);
    }

    /** 取泛型引用的原始类型；取不到时返回 null。 */
    private static Class<?> rawTypeOf(TypeReference<?> ref) {
        Type type = ref.getType();
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            Type raw = ((ParameterizedType) type).getRawType();
            if (raw instanceof Class) {
                return (Class<?>) raw;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) {
        return (T) value;
    }
}
