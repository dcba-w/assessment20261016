package cn.simple.boot.module.nl2sql.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "nl2sql")
public class Nl2sqlProperties {

    private String baseUrl = "https://api.openai.com/v1";
    private String apiKey = "";
    private String model = "gpt-4o-mini";
    private int timeoutSeconds = 60;
    private int maxRows = 100;
    private int maxRetries = 1;
    private int queryTimeoutSeconds = 10;
    /**
     * 当前请求未带租户上下文时的兜底租户
     */
    private long defaultTenantId = 1L;
    /**
     * 表白名单。支持 * 通配：system_* 匹配系统模块表，inspect_* 匹配巡检表；单独一个 * 表示当前库全部表。
     */
    private List<String> allowedTables = new ArrayList<>(List.of(
            "inspect_facility",
            "inspect_record",
            "inspect_record_issue",
            "inspect_record_audit"));

}
