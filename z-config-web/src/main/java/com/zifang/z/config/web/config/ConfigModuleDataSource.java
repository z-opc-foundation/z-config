package com.zifang.z.config.web.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zifang.z.boot.datasource.starter.ModuleDataSourceTemplate;
import org.apache.ibatis.plugin.Interceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = "com.zifang.z.config.core.domain.mapper", sqlSessionFactoryRef = "zConfigSqlSessionFactory")
public class ConfigModuleDataSource extends ModuleDataSourceTemplate {

    @Bean("zConfigDataSource")
    public DataSource dataSource(org.springframework.core.env.Environment env) {
        return buildDataSource(env, "config");
    }

    /**
     * MyBatis-Plus 分页拦截器.
     * <p>
     * 没有 PaginationInnerInterceptor 时, {@code BaseMapper.selectPage(IPage, Wrapper)}
     * 不会自动拼接 {@code LIMIT ...} 子句, 导致 pageConfig 返回 total=0 但有 records.
     */
    @Bean("mybatisPlusInterceptorConfig")
    public MybatisPlusInterceptor mybatisPlusInterceptorConfig() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    @Bean("zConfigSqlSessionFactory")
    public MybatisSqlSessionFactoryBean sqlSessionFactory(
            DataSource zConfigDataSource,
            MybatisPlusInterceptor mybatisPlusInterceptorConfig) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = buildSqlSessionFactory(zConfigDataSource);
        factoryBean.setPlugins(new Interceptor[]{mybatisPlusInterceptorConfig});
        return factoryBean;
    }
}
