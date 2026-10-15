package cn.simple.boot.framework.ai.core;

import cn.simple.boot.framework.ai.config.OpenAiCompatibleProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 最小 OpenAI Chat Completions 封装：服务端调用、超时、有限重试、失败降级。
 */
@Slf4j
public class OpenAiCompatibleClient {

    private final OpenAiCompatibleProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleClient(OpenAiCompatibleProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getTimeoutMs());
        factory.setReadTimeout(properties.getTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
    }

    public boolean isEnabled() {
        return properties.isEnabled() && properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    /**
     * @return 模型文本；未启用或调用失败时返回空字符串（降级）
     */
    public String chat(String prompt) {
        if (!isEnabled()) {
            log.warn("[ai] OpenAI compatible client disabled (set simple.ai.openai.enabled=true and OPENAI_API_KEY)");
            return "";
        }
        RestClientException last = null;
        int attempts = Math.max(1, properties.getMaxRetries() + 1);
        for (int i = 0; i < attempts; i++) {
            try {
                return doChat(prompt);
            } catch (RestClientException ex) {
                last = ex;
                log.warn("[ai] chat attempt {} failed: {}", i + 1, ex.getMessage());
            } catch (Exception ex) {
                log.warn("[ai] chat parse failed: {}", ex.getMessage());
                return "";
            }
        }
        if (last != null) {
            log.error("[ai] chat degraded after retries", last);
        }
        return "";
    }

    private String doChat(String prompt) throws Exception {
        String url = trimSlash(properties.getBaseUrl()) + "/chat/completions";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());
        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );
        String payload = objectMapper.writeValueAsString(body);
        String resp = restTemplate.postForObject(url, new HttpEntity<>(payload, headers), String.class);
        if (resp == null || resp.isBlank()) {
            return "";
        }
        JsonNode root = objectMapper.readTree(resp);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        return content.isMissingNode() || content.isNull() ? "" : content.asText();
    }

    /**
     * 文本向量化。未启用或未配置 embeddingModel 时返回 null（降级）。
     */
    public float[] embed(String text) {
        if (!isEnabled() || properties.getEmbeddingModel() == null || properties.getEmbeddingModel().isBlank()) {
            return null;
        }
        try {
            String url = trimSlash(properties.getBaseUrl()) + "/embeddings";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.getApiKey());
            Map<String, Object> body = Map.of(
                    "model", properties.getEmbeddingModel(),
                    "input", text
            );
            String payload = objectMapper.writeValueAsString(body);
            String resp = restTemplate.postForObject(url, new HttpEntity<>(payload, headers), String.class);
            if (resp == null || resp.isBlank()) {
                return null;
            }
            JsonNode root = objectMapper.readTree(resp);
            JsonNode embedding = root.path("data").path(0).path("embedding");
            if (!embedding.isArray()) {
                return null;
            }
            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = (float) embedding.path(i).asDouble();
            }
            return vector;
        } catch (Exception ex) {
            log.warn("[ai] embed failed: {}", ex.getMessage());
            return null;
        }
    }

    private static String trimSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.openai.com/v1";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

}
