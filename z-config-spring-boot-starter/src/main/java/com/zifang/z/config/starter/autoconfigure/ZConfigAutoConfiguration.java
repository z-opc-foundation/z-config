package com.zifang.z.config.starter.autoconfigure;

import com.zifang.z.config.client.config.ZConfigFactory;
import com.zifang.z.config.client.config.ZConfigService;
import com.zifang.z.config.client.naming.ZNamingService;
import com.zifang.z.config.starter.properties.ZConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * Z-Config Spring Boot 自动配置。
 *
 * <p>当 {@code z.config.enabled=true} 且 classpath 含 {@link ZConfigService} 时激活,
 * 自动创建 {@link ZConfigService} + {@link ZNamingService} 两个 bean.</p>
 *
 * <p>典型用法:</p>
 * <pre>
 * {@literal @}Autowired private ZConfigService zconfig;
 * {@literal @}Autowired private ZNamingService znaming;
 * </pre>
 */
@Configuration
@ConditionalOnClass({ZConfigService.class, ZNamingService.class})
@ConditionalOnProperty(prefix = "z.config", name = "enabled", havingValue = "true")
@AutoConfigureOrder(Integer.MIN_VALUE + 100)
@EnableConfigurationProperties(ZConfigProperties.class)
public class ZConfigAutoConfiguration {

    @ConditionalOnMissingBean
    @Bean(destroyMethod = "shutDown")
    public ZConfigService zConfigService(ZConfigProperties props) {
        Properties p = toProperties(props);
        return ZConfigFactory.createConfigService(p);
    }

    @ConditionalOnMissingBean
    @Bean(destroyMethod = "shutDown")
    public ZNamingService zNamingService(ZConfigProperties props) {
        Properties p = toProperties(props);
        return ZConfigFactory.createNamingService(p);
    }

    private static Properties toProperties(ZConfigProperties props) {
        Properties p = new Properties();
        if (props.getServerAddr() != null) p.setProperty("serverAddr", props.getServerAddr());
        p.setProperty("namespace", props.getNamespace());
        if (props.getUsername() != null) p.setProperty("username", props.getUsername());
        if (props.getPassword() != null) p.setProperty("password", props.getPassword());
        return p;
    }
}
