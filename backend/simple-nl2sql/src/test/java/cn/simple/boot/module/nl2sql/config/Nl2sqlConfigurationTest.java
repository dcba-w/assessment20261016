package cn.simple.boot.module.nl2sql.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class Nl2sqlConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(Nl2sqlConfiguration.class)
            .withPropertyValues(
                    "nl2sql.base-url=http://127.0.0.1:8095/v1",
                    "nl2sql.api-key=test-key",
                    "nl2sql.model=test-model");

    @Test
    void registersNl2sqlPropertiesBean() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(Nl2sqlProperties.class);
            Nl2sqlProperties properties = context.getBean(Nl2sqlProperties.class);
            assertThat(properties.getModel()).isEqualTo("test-model");
            assertThat(properties.getApiKey()).isEqualTo("test-key");
        });
    }

}
