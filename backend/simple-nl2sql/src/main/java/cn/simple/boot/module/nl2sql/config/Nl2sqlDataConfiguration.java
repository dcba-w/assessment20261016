package cn.simple.boot.module.nl2sql.config;

import cn.simple.boot.module.nl2sql.security.SqlSecurityValidator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 复用当前数据源，装配只读查询用的 JdbcTemplate 与 SQL 安全闸。
 * 不要对 DataSource 使用 {@code @ConditionalOnBean}：业务 {@code @Configuration}
 * 早于数据源自动配置解析，条件会误判为 false，导致 nl2sqlJdbcTemplate 根本不注册。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(Nl2sqlProperties.class)
public class Nl2sqlDataConfiguration {

    @Bean(name = "nl2sqlJdbcTemplate")
    public JdbcTemplate nl2sqlJdbcTemplate(DataSource dataSource, Nl2sqlProperties properties) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setQueryTimeout(Math.max(properties.getQueryTimeoutSeconds(), 1));
        return jdbcTemplate;
    }

    @Bean
    public SqlSecurityValidator sqlSecurityValidator(Nl2sqlProperties properties) {
        return new SqlSecurityValidator(
                properties.getAllowedTables(),
                properties.getMaxRows(),
                properties.getDefaultTenantId());
    }

}
