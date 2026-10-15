package cn.simple.boot.server.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityConfigGuardTest {

    @Test
    void localProfileAllowsDevelopmentSettings() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        environment.setProperty("simple.security.mock-enable", "true");
        environment.setProperty("simple.captcha.enable", "false");

        assertDoesNotThrow(() -> SecurityConfigGuard.validate(environment));
    }

    @Test
    void prodProfileAcceptsSecureSettings() {
        MockEnvironment environment = secureProdEnvironment();

        assertDoesNotThrow(() -> SecurityConfigGuard.validate(environment));
    }

    @Test
    void prodYamlIsValidAndSecure() throws Exception {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(
                "application-prod", new ClassPathResource("application-prod.yaml"));
        PropertySource<?> source = sources.get(0);

        assertEquals(false, source.getProperty("simple.security.mock-enable"));
        assertEquals(true, source.getProperty("simple.captcha.enable"));
        assertEquals(10, source.getProperty("simple.security.password-encoder-length"));
        assertEquals(100000, source.getProperty("simple.sms-code.begin-code"));
        assertEquals(999999, source.getProperty("simple.sms-code.end-code"));
        assertEquals("health", source.getProperty("management.endpoints.web.exposure.include"));
        assertEquals(false, source.getProperty("springdoc.swagger-ui.enabled"));
        assertEquals("cn.simple.boot",
                source.getProperty("spring.kafka.consumer.properties.spring.json.trusted.packages"));
    }

    @Test
    void rejectsWildcardKafkaTrustedPackagesInProd() {
        MockEnvironment environment = secureProdEnvironment();
        environment.setProperty("spring.kafka.consumer.properties.spring.json.trusted.packages", "*");

        assertThrows(IllegalStateException.class, () -> SecurityConfigGuard.validate(environment));
    }

    @Test
    void rejectsMockLoginOutsideLocal() {
        MockEnvironment environment = secureProdEnvironment();
        environment.setProperty("simple.security.mock-enable", "true");

        assertThrows(IllegalStateException.class, () -> SecurityConfigGuard.validate(environment));
    }

    @Test
    void rejectsDisabledCaptchaOutsideLocal() {
        MockEnvironment environment = secureProdEnvironment();
        environment.setProperty("simple.captcha.enable", "false");

        assertThrows(IllegalStateException.class, () -> SecurityConfigGuard.validate(environment));
    }

    @Test
    void rejectsWeakPasswordCost() {
        MockEnvironment environment = secureProdEnvironment();
        environment.setProperty("simple.security.password-encoder-length", "4");

        assertThrows(IllegalStateException.class, () -> SecurityConfigGuard.validate(environment));
    }

    @Test
    void rejectsFixedSmsCode() {
        MockEnvironment environment = secureProdEnvironment();
        environment.setProperty("simple.sms-code.begin-code", "999999");
        environment.setProperty("simple.sms-code.end-code", "999999");

        assertThrows(IllegalStateException.class, () -> SecurityConfigGuard.validate(environment));
    }

    @Test
    void rejectsExcessiveActuatorExposureInProd() {
        MockEnvironment environment = secureProdEnvironment();
        environment.setProperty("management.endpoints.web.exposure.include", "*");

        assertThrows(IllegalStateException.class, () -> SecurityConfigGuard.validate(environment));
    }

    private static MockEnvironment secureProdEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.setProperty("simple.security.mock-enable", "false");
        environment.setProperty("simple.captcha.enable", "true");
        environment.setProperty("simple.security.password-encoder-length", "10");
        environment.setProperty("simple.sms-code.begin-code", "100000");
        environment.setProperty("simple.sms-code.end-code", "999999");
        environment.setProperty("springdoc.api-docs.enabled", "false");
        environment.setProperty("springdoc.swagger-ui.enabled", "false");
        environment.setProperty("knife4j.enable", "false");
        environment.setProperty("spring.datasource.druid.stat-view-servlet.enabled", "false");
        environment.setProperty("management.endpoints.web.exposure.include", "health");
        environment.setProperty("spring.kafka.consumer.properties.spring.json.trusted.packages", "cn.simple.boot");
        return environment;
    }
}
