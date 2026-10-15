package cn.simple.boot.server.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 阻止非本地环境携带开发期危险配置启动。
 */
@Component
public class SecurityConfigGuard implements ApplicationRunner {

    private final Environment environment;

    public SecurityConfigGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        validate(environment);
    }

    static void validate(Environment environment) {
        Set<String> profiles = new HashSet<>(Arrays.asList(environment.getActiveProfiles()));
        if (profiles.size() == 1 && profiles.contains("local")) {
            return;
        }

        requireFalse(environment, "simple.security.mock-enable",
                "非本地环境禁止开启 simple.security.mock-enable");
        requireTrue(environment, "simple.captcha.enable",
                "非本地环境必须开启 simple.captcha.enable");

        int passwordCost = environment.getProperty("simple.security.password-encoder-length", Integer.class, 4);
        require(passwordCost >= 10, "非本地环境 BCrypt 强度不得低于 10");

        int beginCode = environment.getProperty("simple.sms-code.begin-code", Integer.class, 0);
        int endCode = environment.getProperty("simple.sms-code.end-code", Integer.class, 0);
        require(beginCode >= 100000 && endCode <= 999999 && beginCode < endCode,
                "非本地环境短信验证码必须为非固定的 6 位随机数");

        if (profiles.contains("prod")) {
            requireFalse(environment, "springdoc.api-docs.enabled", "生产环境必须关闭 OpenAPI 文档");
            requireFalse(environment, "springdoc.swagger-ui.enabled", "生产环境必须关闭 Swagger UI");
            requireFalse(environment, "knife4j.enable", "生产环境必须关闭 Knife4j");
            requireFalse(environment, "spring.datasource.druid.stat-view-servlet.enabled",
                    "生产环境必须关闭 Druid 监控页面");
            String exposed = environment.getProperty("management.endpoints.web.exposure.include", "");
            require("health".equalsIgnoreCase(exposed.trim()),
                    "生产环境 Actuator 只允许暴露 health");
            String trustedPackages = environment.getProperty(
                    "spring.kafka.consumer.properties.spring.json.trusted.packages", "");
            require(trustedPackages != null && !trustedPackages.isBlank() && !"*".equals(trustedPackages.trim()),
                    "生产环境 Kafka trusted.packages 不得为 *");
        }
    }

    private static void requireTrue(Environment environment, String key, String message) {
        require(environment.getProperty(key, Boolean.class, false), message);
    }

    private static void requireFalse(Environment environment, String key, String message) {
        require(!environment.getProperty(key, Boolean.class, false), message);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
