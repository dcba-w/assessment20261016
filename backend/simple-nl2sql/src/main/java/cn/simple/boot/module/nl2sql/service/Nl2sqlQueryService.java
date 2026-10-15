package cn.simple.boot.module.nl2sql.service;

import cn.simple.boot.framework.common.exception.ServiceException;
import cn.simple.boot.module.nl2sql.ai.SqlGenerationResult;
import cn.simple.boot.module.nl2sql.ai.TextToSqlAi;
import cn.simple.boot.module.nl2sql.config.Nl2sqlProperties;
import cn.simple.boot.module.nl2sql.controller.admin.vo.Nl2sqlQueryReqVO;
import cn.simple.boot.module.nl2sql.controller.admin.vo.Nl2sqlQueryRespVO;
import cn.simple.boot.module.nl2sql.schema.ColumnLabelResolver;
import cn.simple.boot.module.nl2sql.schema.SchemaService;
import cn.simple.boot.module.nl2sql.schema.SchemaSnapshot;
import cn.simple.boot.module.nl2sql.security.SqlSecurityValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.simple.boot.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.simple.boot.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.simple.boot.module.nl2sql.enums.ErrorCodeConstants.NL2SQL_API_KEY_MISSING;
import static cn.simple.boot.module.nl2sql.enums.ErrorCodeConstants.NL2SQL_QUERY_FAIL;
import static cn.simple.boot.module.nl2sql.enums.ErrorCodeConstants.NL2SQL_SQL_GENERATE_FAIL;

@Service
@Slf4j
public class Nl2sqlQueryService {

    private static final Pattern CODE_BLOCK = Pattern.compile("```(?:json|sql)?\\s*(.*?)\\s*```",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private final TextToSqlAi textToSqlAi;
    private final SchemaService schemaService;
    private final SqlSecurityValidator sqlSecurityValidator;
    private final Nl2sqlProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public Nl2sqlQueryService(TextToSqlAi textToSqlAi,
                              SchemaService schemaService,
                              SqlSecurityValidator sqlSecurityValidator,
                              Nl2sqlProperties properties,
                              @Qualifier("nl2sqlJdbcTemplate") JdbcTemplate jdbcTemplate,
                              ObjectMapper objectMapper) {
        this.textToSqlAi = textToSqlAi;
        this.schemaService = schemaService;
        this.sqlSecurityValidator = sqlSecurityValidator;
        this.properties = properties;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public Nl2sqlQueryRespVO query(Nl2sqlQueryReqVO request) {
        if (request == null || !StringUtils.hasText(request.getQuestion())) {
            throw invalidParamException("问题不能为空");
        }
        checkApiKey();
        String schemaPrompt = schemaService.getPromptSchema();
        SchemaSnapshot snapshot = schemaService.getSnapshot();
        String question = request.getQuestion().trim();
        SqlGenerationResult generated = parseAiOutput(textToSqlAi.generate(
                schemaPrompt, schemaService.getFewShotExamples(), question));
        return executeWithSelfCorrection(question, generated, schemaPrompt, snapshot, 0);
    }

    private Nl2sqlQueryRespVO executeWithSelfCorrection(String question,
                                                        SqlGenerationResult generated,
                                                        String schemaPrompt,
                                                        SchemaSnapshot snapshot,
                                                        int attempt) {
        try {
            String safeSql = sqlSecurityValidator.validateAndRewrite(generated.getSql(), snapshot);
            List<Map<String, Object>> rows = convertRows(jdbcTemplate.queryForList(safeSql));
            List<String> columns = rows.isEmpty() ? new ArrayList<>() : new ArrayList<>(rows.get(0).keySet());
            return Nl2sqlQueryRespVO.builder()
                    .answer(buildAnswer(question, columns, rows))
                    .columns(columns)
                    .columnLabels(ColumnLabelResolver.labels(columns, safeSql, snapshot))
                    .rows(rows)
                    .build();
        } catch (ServiceException ex) {
            return retryOrThrow(question, generated, schemaPrompt, snapshot, attempt, ex.getMessage(), ex);
        } catch (DataAccessException ex) {
            String message = rootMessage(ex);
            return retryOrThrow(question, generated, schemaPrompt, snapshot, attempt, message,
                    exception(NL2SQL_QUERY_FAIL, message));
        }
    }

    private Nl2sqlQueryRespVO retryOrThrow(String question,
                                           SqlGenerationResult generated,
                                           String schemaPrompt,
                                           SchemaSnapshot snapshot,
                                           int attempt,
                                           String error,
                                           ServiceException giveUp) {
        if (attempt >= Math.max(properties.getMaxRetries(), 0)) {
            throw giveUp;
        }
        log.info("[nl2sql] SQL 失败，回喂模型修正 attempt={} error={}", attempt + 1, error);
        SqlGenerationResult repaired = parseAiOutput(textToSqlAi.repair(schemaPrompt, generated.getSql(), error));
        return executeWithSelfCorrection(question, repaired, schemaPrompt, snapshot, attempt + 1);
    }

    private String buildAnswer(String question, List<String> columns, List<Map<String, Object>> rows) {
        try {
            int preview = Math.min(rows.size(), 20);
            String resultJson = objectMapper.writeValueAsString(Map.of(
                    "rowCount", rows.size(),
                    "columns", columns,
                    "rows", rows.subList(0, preview)));
            String answer = textToSqlAi.answer(question, resultJson);
            if (StringUtils.hasText(answer)) {
                return cleanAnswer(answer);
            }
        } catch (Exception ex) {
            log.warn("[nl2sql] 口语化回答失败，使用兜底文案", ex);
        }
        if (rows.isEmpty()) {
            return "没有查到符合条件的记录。";
        }
        return "一共查到 " + rows.size() + " 条记录。";
    }

    private void checkApiKey() {
        String apiKey = properties.getApiKey();
        if (!StringUtils.hasText(apiKey) || "sk-xxxx".equalsIgnoreCase(apiKey.trim())) {
            throw exception(NL2SQL_API_KEY_MISSING);
        }
    }

    private SqlGenerationResult parseAiOutput(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw exception(NL2SQL_SQL_GENERATE_FAIL, "模型返回空内容");
        }
        String text = raw.trim();
        Matcher matcher = CODE_BLOCK.matcher(text);
        if (matcher.find()) {
            text = matcher.group(1).trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            try {
                JsonNode obj = objectMapper.readTree(text.substring(start, end + 1));
                String sql = textOf(obj, "sql");
                if (!StringUtils.hasText(sql)) {
                    sql = textOf(obj, "SQL");
                }
                if (StringUtils.hasText(sql)) {
                    return new SqlGenerationResult(sql.trim(), textOf(obj, "explanation"));
                }
            } catch (Exception ignored) {
                // 按纯 SQL 处理
            }
        }
        if (text.regionMatches(true, 0, "select", 0, 6)) {
            return new SqlGenerationResult(text, "");
        }
        log.warn("[nl2sql] 模型输出无法解析为 SQL，preview={}",
                text.length() > 200 ? text.substring(0, 200) : text);
        throw exception(NL2SQL_SQL_GENERATE_FAIL, "模型返回内容无法解析");
    }

    private static String cleanAnswer(String answer) {
        if (!StringUtils.hasText(answer)) {
            return "";
        }
        return answer.replaceAll("(?s)```.*?```", "")
                .replaceAll("^[\"“]+|[\"”]+$", "")
                .trim();
    }

    private static String textOf(JsonNode obj, String field) {
        JsonNode node = obj.get(field);
        return node == null || node.isNull() ? "" : node.asText();
    }

    private List<Map<String, Object>> convertRows(List<Map<String, Object>> rawRows) {
        List<Map<String, Object>> result = new ArrayList<>(rawRows.size());
        for (Map<String, Object> raw : rawRows) {
            Map<String, Object> row = new LinkedHashMap<>();
            raw.forEach((key, value) -> row.put(key, convertValue(value)));
            result.add(row);
        }
        return result;
    }

    private Object convertValue(Object value) {
        if (value instanceof byte[]) {
            byte[] bytes = (byte[]) value;
            if (bytes.length == 1) {
                return bytes[0] != 0;
            }
            return bytes;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime().toString();
        }
        if (value instanceof Date && !(value instanceof java.sql.Date)) {
            return LocalDateTime.ofInstant(((Date) value).toInstant(), ZoneId.systemDefault()).toString();
        }
        return value;
    }

    private static String rootMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return StringUtils.hasText(current.getMessage()) ? current.getMessage() : ex.getClass().getSimpleName();
    }

}
