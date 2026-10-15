package cn.simple.boot.framework.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenAI 兼容协议客户端配置。Key 通过环境变量注入，禁止写入仓库。
 */
@ConfigurationProperties(prefix = "simple.ai.openai")
@Data
public class OpenAiCompatibleProperties {

    /**
     * 未配置 Key 时保持关闭
     */
    private boolean enabled = false;

    /**
     * 例如 https://api.openai.com/v1 或兼容网关
     */
    private String baseUrl = "https://api.openai.com/v1";

    /**
     * 从环境变量 OPENAI_API_KEY 读取
     */
    private String apiKey = "";

    private String model = "gpt-4o-mini";

    /**
     * 向量化模型，例如 text-embedding-3-small / bge-m3；留空则不支持知识库检索
     */
    private String embeddingModel = "";

    private int timeoutMs = 60_000;

    private int maxRetries = 2;

}
