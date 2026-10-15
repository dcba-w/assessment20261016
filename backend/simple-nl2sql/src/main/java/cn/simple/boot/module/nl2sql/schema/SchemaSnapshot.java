package cn.simple.boot.module.nl2sql.schema;

import lombok.Builder;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
public class SchemaSnapshot {

    private Map<String, TableMeta> tables;

    public TableMeta getTable(String tableName) {
        if (tableName == null || tables == null) {
            return null;
        }
        return tables.get(unwrap(tableName).toLowerCase());
    }

    public static String unwrap(String name) {
        if (name == null) {
            return "";
        }
        return name.replace("`", "").replace("\"", "");
    }

    @Data
    @Builder
    public static class TableMeta {
        private String name;
        private String comment;
        private Map<String, ColumnMeta> columns;

        public boolean hasColumn(String column) {
            return columns != null && columns.containsKey(unwrap(column).toLowerCase());
        }
    }

    @Data
    @Builder
    public static class ColumnMeta {
        private String name;
        private String type;
        private String comment;
    }

    public static SchemaSnapshot empty() {
        return SchemaSnapshot.builder().tables(new LinkedHashMap<>()).build();
    }

}
