package cn.simple.boot.module.nl2sql.security;

import cn.simple.boot.framework.common.exception.ServiceException;
import cn.simple.boot.framework.tenant.core.context.TenantContextHolder;
import cn.simple.boot.module.nl2sql.schema.SchemaSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlSecurityValidatorTest {

    private SqlSecurityValidator validator;
    private SchemaSnapshot schema;

    @BeforeEach
    void setUp() {
        validator = new SqlSecurityValidator(List.of("inspect_facility", "inspect_record"), 20, 1L);
        Map<String, SchemaSnapshot.ColumnMeta> columns = new LinkedHashMap<>();
        columns.put("id", SchemaSnapshot.ColumnMeta.builder().name("id").type("bigint").comment("").build());
        columns.put("name", SchemaSnapshot.ColumnMeta.builder().name("name").type("varchar").comment("").build());
        columns.put("tenant_id", SchemaSnapshot.ColumnMeta.builder().name("tenant_id").type("bigint").comment("").build());
        columns.put("deleted", SchemaSnapshot.ColumnMeta.builder().name("deleted").type("bit(1)").comment("").build());
        SchemaSnapshot.TableMeta table = SchemaSnapshot.TableMeta.builder()
                .name("inspect_facility")
                .comment("设施")
                .columns(columns)
                .build();
        Map<String, SchemaSnapshot.TableMeta> tables = new LinkedHashMap<>();
        tables.put("inspect_facility", table);
        schema = SchemaSnapshot.builder().tables(tables).build();
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void allowsSelectAndRewrites() {
        String sql = validator.validateAndRewrite("SELECT name FROM inspect_facility", schema);
        String lower = sql.toLowerCase();
        assertTrue(lower.contains("select"));
        assertTrue(lower.contains("inspect_facility"));
        assertTrue(lower.contains("limit"));
        assertTrue(lower.contains("tenant_id"));
        assertTrue(lower.contains("deleted"));
    }

    @Test
    void rejectsWriteSql() {
        assertThrows(ServiceException.class,
                () -> validator.validateAndRewrite("DELETE FROM inspect_facility", schema));
    }

    @Test
    void rejectsUnknownTable() {
        assertThrows(ServiceException.class,
                () -> validator.validateAndRewrite("SELECT * FROM system_users", schema));
    }

    @Test
    void allowsTableMatchingWildcard() {
        validator = new SqlSecurityValidator(List.of("inspect_*"), 20, 1L);
        String sql = validator.validateAndRewrite("SELECT name FROM inspect_facility", schema);
        assertTrue(sql.toLowerCase().contains("inspect_facility"));
    }

    @Test
    void rejectsTableOutsideWildcard() {
        validator = new SqlSecurityValidator(List.of("system_*"), 20, 1L);
        assertThrows(ServiceException.class,
                () -> validator.validateAndRewrite("SELECT name FROM inspect_facility", schema));
    }

    @Test
    void rejectsMultiStatement() {
        assertThrows(ServiceException.class,
                () -> validator.validateAndRewrite("SELECT 1 FROM inspect_facility; DROP TABLE inspect_facility", schema));
    }

}
