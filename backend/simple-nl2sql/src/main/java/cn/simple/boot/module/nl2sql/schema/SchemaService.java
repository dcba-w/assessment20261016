package cn.simple.boot.module.nl2sql.schema;

import cn.simple.boot.module.nl2sql.config.Nl2sqlProperties;
import cn.simple.boot.module.nl2sql.security.AllowedTableMatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static cn.simple.boot.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.simple.boot.module.nl2sql.enums.ErrorCodeConstants.NL2SQL_SCHEMA_EMPTY;

@Service
@Slf4j
public class SchemaService {

    private static final String DICT_HINTS = """
            枚举取值（条件里用数字）：
            inspect_facility.facility_type：1 桥梁，2 隧道，3 路段
            inspect_facility.status：1 正常使用，2 维修中，3 停用
            inspect_record.status：1 草稿，2 已提交，3 已审核，4 已退回
            inspect_record.conclusion：1 正常，2 异常
            inspect_record_issue.issue_type：1 结构，2 外观，3 其它
            inspect_record_issue.severity：1 一般，2 严重，3 紧急
            inspect_record_audit.audit_action：1 提交，2 通过，3 退回
            关联：inspect_record.facility_id = inspect_facility.id；inspect_record_issue.record_id = inspect_record.id
            """;

    private static final String DEFAULT_FEW_SHOT = """
            Q: 有多少座桥梁？
            A: SELECT COUNT(*) AS cnt FROM inspect_facility WHERE facility_type = 1
            Q: 列出已审核的异常巡检记录
            A: SELECT r.record_no, f.name AS facility_name, r.inspect_time FROM inspect_record r JOIN inspect_facility f ON r.facility_id = f.id WHERE r.status = 3 AND r.conclusion = 2
            Q: 最近 10 条巡检记录
            A: SELECT record_no, facility_id, inspect_time, status FROM inspect_record ORDER BY inspect_time DESC LIMIT 10
            """;

    private final JdbcTemplate jdbcTemplate;
    private final Nl2sqlProperties properties;
    private final AtomicReference<Cache> cacheRef = new AtomicReference<>();

    public SchemaService(@Qualifier("nl2sqlJdbcTemplate") JdbcTemplate jdbcTemplate, Nl2sqlProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    public SchemaSnapshot getSnapshot() {
        return load().snapshot;
    }

    public String getPromptSchema() {
        return load().prompt;
    }

    public String getFewShotExamples() {
        return DEFAULT_FEW_SHOT;
    }

    private Cache load() {
        Cache cached = cacheRef.get();
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.loadedAt < 10 * 60 * 1000L) {
            return cached;
        }
        SchemaSnapshot snapshot = loadSnapshot();
        if (snapshot.getTables().isEmpty()) {
            throw exception(NL2SQL_SCHEMA_EMPTY);
        }
        Cache fresh = new Cache(snapshot, buildPrompt(snapshot), now);
        cacheRef.set(fresh);
        return fresh;
    }

    private SchemaSnapshot loadSnapshot() {
        AllowedTableMatcher matcher = new AllowedTableMatcher(properties.getAllowedTables());
        if (matcher.isEmpty()) {
            throw exception(NL2SQL_SCHEMA_EMPTY);
        }
        List<Map<String, Object>> tableRows;
        List<Map<String, Object>> columnRows;
        try {
            tableRows = jdbcTemplate.queryForList(
                    "SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()");
        } catch (Exception ex) {
            log.warn("[nl2sql] 读取 information_schema 失败", ex);
            throw exception(NL2SQL_SCHEMA_EMPTY);
        }
        tableRows = tableRows.stream()
                .filter(row -> matcher.matches(String.valueOf(row.get("TABLE_NAME"))))
                .collect(Collectors.toList());
        if (tableRows.isEmpty()) {
            throw exception(NL2SQL_SCHEMA_EMPTY);
        }
        List<String> matchedLower = tableRows.stream()
                .map(row -> String.valueOf(row.get("TABLE_NAME")).toLowerCase())
                .toList();
        String placeholders = matchedLower.stream().map(t -> "?").collect(Collectors.joining(","));
        try {
            columnRows = jdbcTemplate.queryForList(
                    "SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) IN ("
                            + placeholders + ") ORDER BY TABLE_NAME, ORDINAL_POSITION",
                    matchedLower.toArray());
        } catch (Exception ex) {
            log.warn("[nl2sql] 读取 information_schema 失败", ex);
            throw exception(NL2SQL_SCHEMA_EMPTY);
        }

        Map<String, SchemaSnapshot.TableMeta> tables = new LinkedHashMap<>();
        for (Map<String, Object> row : tableRows) {
            String name = String.valueOf(row.get("TABLE_NAME"));
            SchemaSnapshot.TableMeta meta = SchemaSnapshot.TableMeta.builder()
                    .name(name)
                    .comment(nullToEmpty((String) row.get("TABLE_COMMENT")))
                    .columns(new LinkedHashMap<>())
                    .build();
            tables.put(name.toLowerCase(), meta);
        }
        for (Map<String, Object> row : columnRows) {
            String tableName = String.valueOf(row.get("TABLE_NAME"));
            SchemaSnapshot.TableMeta table = tables.get(tableName.toLowerCase());
            if (table == null) {
                continue;
            }
            String columnName = String.valueOf(row.get("COLUMN_NAME"));
            table.getColumns().put(columnName.toLowerCase(), SchemaSnapshot.ColumnMeta.builder()
                    .name(columnName)
                    .type(String.valueOf(row.get("COLUMN_TYPE")))
                    .comment(nullToEmpty((String) row.get("COLUMN_COMMENT")))
                    .build());
        }
        return SchemaSnapshot.builder().tables(tables).build();
    }

    private String buildPrompt(SchemaSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        for (SchemaSnapshot.TableMeta table : snapshot.getTables().values()) {
            sb.append("表 ").append(table.getName());
            if (StringUtils.hasText(table.getComment())) {
                sb.append("（").append(table.getComment()).append("）");
            }
            sb.append('\n');
            List<String> cols = new ArrayList<>();
            for (SchemaSnapshot.ColumnMeta col : table.getColumns().values()) {
                String piece = col.getName() + " " + col.getType();
                if (StringUtils.hasText(col.getComment())) {
                    piece += " -- " + col.getComment();
                }
                cols.add(piece);
            }
            sb.append("  列：").append(String.join("; ", cols)).append('\n');
        }
        sb.append('\n').append(DICT_HINTS);
        return sb.toString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record Cache(SchemaSnapshot snapshot, String prompt, long loadedAt) {
    }

}
