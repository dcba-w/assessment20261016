package cn.simple.boot.module.nl2sql.framework.web.config;

import cn.simple.boot.framework.swagger.config.SimpleSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * NL2SQL 模块的 web 组件 Configuration
 */
@Configuration(proxyBeanMethods = false)
public class Nl2sqlWebConfiguration {

    @Bean
    public GroupedOpenApi nl2sqlGroupedOpenApi() {
        return SimpleSwaggerAutoConfiguration.buildGroupedOpenApi("nl2sql");
    }

}
