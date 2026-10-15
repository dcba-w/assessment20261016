package cn.simple.boot.module.nl2sql.config;

import cn.simple.boot.module.nl2sql.security.SqlSecurityValidator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class Nl2sqlDataConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(Nl2sqlDataConfiguration.class)
            .withConfiguration(AutoConfigurations.of(LateDataSourceAutoConfiguration.class))
            .withPropertyValues(
                    "nl2sql.query-timeout-seconds=15",
                    "nl2sql.max-rows=50",
                    "nl2sql.default-tenant-id=2",
                    "nl2sql.allowed-tables=inspect_facility,system_users");

    @Test
    void registersNamedJdbcTemplateWhenDataSourceIsAutoConfiguredLater() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("nl2sqlJdbcTemplate");
            JdbcTemplate jdbcTemplate = context.getBean("nl2sqlJdbcTemplate", JdbcTemplate.class);
            assertThat(jdbcTemplate.getQueryTimeout()).isEqualTo(15);
        });
    }

    @Test
    void registersSqlSecurityValidator() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SqlSecurityValidator.class);
        });
    }

    /**
     * 模拟生产环境：DataSource 由自动配置提供，晚于业务 @Configuration。
     */
    @AutoConfiguration
    static class LateDataSourceAutoConfiguration {
        @Bean
        DataSource dataSource() {
            return mock(DataSource.class);
        }
    }

}
