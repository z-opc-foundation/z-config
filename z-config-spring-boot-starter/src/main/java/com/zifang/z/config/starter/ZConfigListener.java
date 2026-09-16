package com.zifang.z.config.starter;

import java.lang.annotation.*;

/**
 * z-config 配置变更监听注解。
 *
 * <p>标注在 {@link org.springframework.boot.context.properties.ConfigurationProperties} 类上，
 * 当 z-config 服务端推送对应 dataId 的配置变更时，自动调用回调方法。
 *
 * <p>配合 {@link com.zifang.z.team.core.config.support.AbstractZConfigProperties} 使用时，
 * 只需声明注解，回调方法由基类 {@code onConfigChanged(String)} 兜底实现。
 *
 * <p>使用示例：
 * <pre>{@code
 * @ConfigurationProperties(prefix = "z-team.llm")
 * @ZConfigListener(dataId = "z-team.llm", group = "DEFAULT_GROUP", fireOnInit = false)
 * public class TeamLlmProperties extends AbstractZConfigProperties {
 *     // ...
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ZConfigListener {

    /**
     * 监听的配置 dataId（对应 z-config 服务端的 dataId）。
     */
    String dataId();

    /**
     * 监听的配置 group（默认 DEFAULT_GROUP）。
     */
    String group() default "DEFAULT_GROUP";

    /**
     * 是否在初始化时触发回调（默认 false，即只在配置变更时触发）。
     */
    boolean fireOnInit() default false;

    /**
     * 回调方法名（默认 "onConfigChanged"，与 AbstractZConfigProperties 兜底方法一致）。
     */
    String callbackMethod() default "onConfigChanged";
}
