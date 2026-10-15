package cn.simple.boot.module.nl2sql.schema;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColumnLabelResolverTest {

    @Test
    void usesTableColumnCommentForAlias() {
        SchemaSnapshot schema = roleSchema();
        String sql = "SELECT r.id AS role_id, r.name AS role_name, COUNT(*) AS user_count "
                + "FROM system_role r GROUP BY r.id, r.name LIMIT 100";
        List<String> labels = ColumnLabelResolver.labels(
                List.of("role_id", "role_name", "user_count"), sql, schema);
        assertEquals(List.of("角色ID", "角色名称", "数量"), labels);
    }

    @Test
    void usesCommentWhenSelectingBareColumn() {
        SchemaSnapshot schema = roleSchema();
        List<String> labels = ColumnLabelResolver.labels(
                List.of("name", "status"),
                "SELECT name, status FROM system_role LIMIT 10",
                schema);
        assertEquals(List.of("角色名称", "角色状态"), labels);
    }

    @Test
    void keepsOriginalWhenNoComment() {
        SchemaSnapshot schema = roleSchema();
        List<String> labels = ColumnLabelResolver.labels(
                List.of("unknown_col"),
                "SELECT 1 AS unknown_col FROM system_role",
                schema);
        assertEquals(List.of("unknown_col"), labels);
    }

    private static SchemaSnapshot roleSchema() {
        Map<String, SchemaSnapshot.ColumnMeta> columns = new LinkedHashMap<>();
        columns.put("id", col("id", "角色ID"));
        columns.put("name", col("name", "角色名称"));
        columns.put("status", col("status", "角色状态（0正常 1停用）"));
        columns.put("tenant_id", col("tenant_id", "租户编号"));
        SchemaSnapshot.TableMeta table = SchemaSnapshot.TableMeta.builder()
                .name("system_role")
                .comment("角色信息表")
                .columns(columns)
                .build();
        Map<String, SchemaSnapshot.TableMeta> tables = new LinkedHashMap<>();
        tables.put("system_role", table);
        return SchemaSnapshot.builder().tables(tables).build();
    }

    private static SchemaSnapshot.ColumnMeta col(String name, String comment) {
        return SchemaSnapshot.ColumnMeta.builder().name(name).type("varchar").comment(comment).build();
    }

}
