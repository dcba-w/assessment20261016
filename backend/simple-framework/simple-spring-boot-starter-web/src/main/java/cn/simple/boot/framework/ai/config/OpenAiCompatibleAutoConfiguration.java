package cn.simple.boot.framework.ai.config;

import cn.simple.boot.framework.ai.core.OpenAiCompatibleClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(OpenAiCompatibleProperties.class)
public class OpenAiCompatibleAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OpenAiCompatibleClient openAiCompatibleClient(OpenAiCompatibleProperties properties,
                                                         ObjectMapper objectMapper) {
        return new OpenAiCompatibleClient(properties, objectMapper);
    }

}
