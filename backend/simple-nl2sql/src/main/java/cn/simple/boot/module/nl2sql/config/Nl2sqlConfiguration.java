package cn.simple.boot.module.nl2sql.config;

import cn.simple.boot.module.nl2sql.ai.TextToSqlAi;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 只装配模型。JdbcTemplate / SqlSecurityValidator 在 {@link Nl2sqlDataConfiguration}。
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(Nl2sqlProperties.class)
@ConditionalOnClass({ChatModel.class, OpenAiChatModel.class, AiServices.class})
public class Nl2sqlConfiguration {

    @Bean
    public TextToSqlAi textToSqlAi(Nl2sqlProperties properties) {
        try {
            String apiKey = StringUtils.hasText(properties.getApiKey()) ? properties.getApiKey() : "sk-xxxx";
            ChatModel chatModel = OpenAiChatModel.builder()
                    .baseUrl(properties.getBaseUrl())
                    .apiKey(apiKey)
                    .modelName(properties.getModel())
                    .temperature(0.0)
                    .timeout(Duration.ofSeconds(Math.max(properties.getTimeoutSeconds(), 10)))
                    .build();
            return AiServices.builder(TextToSqlAi.class)
                    .chatModel(chatModel)
                    .build();
        } catch (Throwable ex) {
            log.warn("[nl2sql] 模型初始化失败，已跳过查询接口：{}", ex.toString());
            return null;
        }
    }

}
