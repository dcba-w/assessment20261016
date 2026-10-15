package cn.simple.boot.module.nl2sql.schema;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 用 SQL 选列对应的库表 COMMENT 作为展示名；对不上时保留结果列名。
 */
public final class ColumnLabelResolver {

    private ColumnLabelResolver() {
    }

    public static List<String> labels(List<String> columns, String sql, SchemaSnapshot snapshot) {
        Map<String, String> fromSql = parseSelectLabels(sql, snapshot);
        List<String> result = new ArrayList<>();
        for (String column : columns == null ? List.<String>of() : columns) {
            if (!StringUtils.hasText(column)) {
                result.add("");
                continue;
            }
            String key = column.trim();
            String label = fromSql.get(key.toLowerCase(Locale.ROOT));
            if (!StringUtils.hasText(label)) {
                label = commentByColumnName(snapshot, key);
            }
            result.add(StringUtils.hasText(label) ? shortenComment(label) : key);
        }
        return result;
    }

    private static Map<String, String> parseSelectLabels(String sql, SchemaSnapshot snapshot) {
        Map<String, String> labels = new LinkedHashMap<>();
        if (!StringUtils.hasText(sql) || snapshot == null) {
            return labels;
        }
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select select) || !(select instanceof PlainSelect plainSelect)) {
                return labels;
            }
            Map<String, String> aliasToTable = tableAliases(plainSelect);
            if (plainSelect.getSelectItems() == null) {
                return labels;
            }
            for (SelectItem<?> item : plainSelect.getSelectItems()) {
                if (item == null) {
                    continue;
                }
                Expression expression = item.getExpression();
                if (expression instanceof AllColumns || expression instanceof AllTableColumns) {
                    continue;
                }
                String resultName = resultName(item, expression);
                if (!StringUtils.hasText(resultName)) {
                    continue;
                }
                String comment = commentOfExpression(expression, aliasToTable, snapshot);
                if (StringUtils.hasText(comment)) {
                    labels.put(resultName.toLowerCase(Locale.ROOT), comment);
                }
            }
        } catch (Exception ignored) {
            return labels;
        }
        return labels;
    }

    private static String resultName(SelectItem<?> item, Expression expression) {
        Alias alias = item.getAlias();
        if (alias != null && StringUtils.hasText(alias.getName())) {
            return SchemaSnapshot.unwrap(alias.getName());
        }
        if (expression instanceof Column column) {
            return SchemaSnapshot.unwrap(column.getColumnName());
        }
        return null;
    }

    private static String commentOfExpression(Expression expression,
                                              Map<String, String> aliasToTable,
                                              SchemaSnapshot snapshot) {
        if (expression instanceof Column column) {
            return commentOfColumn(column, aliasToTable, snapshot);
        }
        if (expression instanceof Function function) {
            return aggregateLabel(function);
        }
        return "";
    }

    private static String commentOfColumn(Column column,
                                          Map<String, String> aliasToTable,
                                          SchemaSnapshot snapshot) {
        String columnName = SchemaSnapshot.unwrap(column.getColumnName());
        String tableRef = column.getTable() == null ? "" : SchemaSnapshot.unwrap(column.getTable().getName());
        if (StringUtils.hasText(tableRef)) {
            String physical = aliasToTable.getOrDefault(tableRef.toLowerCase(Locale.ROOT), tableRef);
            return comment(snapshot, physical, columnName);
        }
        if (aliasToTable.size() == 1) {
            return comment(snapshot, aliasToTable.values().iterator().next(), columnName);
        }
        return commentByColumnName(snapshot, columnName);
    }

    private static String aggregateLabel(Function function) {
        String name = function.getName() == null ? "" : function.getName().toLowerCase(Locale.ROOT);
        if ("count".equals(name)) {
            return "数量";
        }
        if ("sum".equals(name)) {
            return "合计";
        }
        if ("avg".equals(name)) {
            return "平均值";
        }
        return "";
    }

    private static Map<String, String> tableAliases(PlainSelect plainSelect) {
        Map<String, String> aliasToTable = new LinkedHashMap<>();
        collectTableAlias(plainSelect.getFromItem(), aliasToTable);
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                collectTableAlias(join.getFromItem(), aliasToTable);
            }
        }
        return aliasToTable;
    }

    private static void collectTableAlias(FromItem fromItem, Map<String, String> aliasToTable) {
        if (fromItem instanceof Table table) {
            String physical = SchemaSnapshot.unwrap(table.getName());
            if (!StringUtils.hasText(physical)) {
                return;
            }
            aliasToTable.put(physical.toLowerCase(Locale.ROOT), physical);
            if (table.getAlias() != null && StringUtils.hasText(table.getAlias().getName())) {
                aliasToTable.put(table.getAlias().getName().toLowerCase(Locale.ROOT), physical);
            }
            return;
        }
        if (fromItem instanceof ParenthesedSelect parenthesedSelect
                && parenthesedSelect.getSelect() instanceof PlainSelect inner) {
            collectTableAlias(inner.getFromItem(), aliasToTable);
        }
    }

    private static String comment(SchemaSnapshot snapshot, String table, String column) {
        if (snapshot == null || !StringUtils.hasText(table) || !StringUtils.hasText(column)) {
            return "";
        }
        SchemaSnapshot.TableMeta meta = snapshot.getTable(table);
        if (meta == null || meta.getColumns() == null) {
            return "";
        }
        SchemaSnapshot.ColumnMeta columnMeta = meta.getColumns().get(SchemaSnapshot.unwrap(column).toLowerCase(Locale.ROOT));
        return columnMeta == null ? "" : nullToEmpty(columnMeta.getComment());
    }

    private static String commentByColumnName(SchemaSnapshot snapshot, String column) {
        if (snapshot == null || snapshot.getTables() == null || !StringUtils.hasText(column)) {
            return "";
        }
        String key = SchemaSnapshot.unwrap(column).toLowerCase(Locale.ROOT);
        String found = null;
        for (SchemaSnapshot.TableMeta table : snapshot.getTables().values()) {
            if (table.getColumns() == null) {
                continue;
            }
            SchemaSnapshot.ColumnMeta columnMeta = table.getColumns().get(key);
            if (columnMeta == null || !StringUtils.hasText(columnMeta.getComment())) {
                continue;
            }
            String comment = columnMeta.getComment();
            if (found != null && !found.equals(comment)) {
                return "";
            }
            found = comment;
        }
        return found == null ? "" : found;
    }

    static String shortenComment(String comment) {
        if (!StringUtils.hasText(comment)) {
            return "";
        }
        String text = comment.trim();
        int paren = indexOfEnumHint(text);
        return paren > 0 ? text.substring(0, paren).trim() : text;
    }

    private static int indexOfEnumHint(String text) {
        int full = text.indexOf('（');
        int half = text.indexOf('(');
        if (full < 0) {
            return half;
        }
        if (half < 0) {
            return full;
        }
        return Math.min(full, half);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

}
