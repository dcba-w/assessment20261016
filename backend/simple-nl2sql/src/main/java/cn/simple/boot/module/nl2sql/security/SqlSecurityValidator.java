package cn.simple.boot.module.nl2sql.security;

import cn.simple.boot.framework.tenant.core.context.TenantContextHolder;
import cn.simple.boot.module.nl2sql.schema.SchemaSnapshot;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

import static cn.simple.boot.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.simple.boot.module.nl2sql.enums.ErrorCodeConstants.NL2SQL_SQL_INVALID;
import static cn.simple.boot.module.nl2sql.enums.ErrorCodeConstants.NL2SQL_TABLE_DENIED;

/**
 * AST 校验与改写：只允许 SELECT，表白名单，强制 LIMIT，补 tenant_id / deleted。
 */
public class SqlSecurityValidator {

    private final AllowedTableMatcher tableMatcher;
    private final int maxRows;
    private final long defaultTenantId;

    public SqlSecurityValidator(List<String> allowedTables, int maxRows, long defaultTenantId) {
        this.tableMatcher = new AllowedTableMatcher(allowedTables);
        this.maxRows = maxRows > 0 ? maxRows : 100;
        this.defaultTenantId = defaultTenantId;
    }

    public String validateAndRewrite(String rawSql, SchemaSnapshot schema) {
        String sql = rawSql == null ? "" : rawSql.trim();
        if (!StringUtils.hasText(sql)) {
            throw exception(NL2SQL_SQL_INVALID, "SQL 为空");
        }
        sql = stripTrailingSemicolons(sql);
        Statements statements;
        try {
            statements = CCJSqlParserUtil.parseStatements(sql);
        } catch (Exception ex) {
            throw exception(NL2SQL_SQL_INVALID, "无法解析：" + ex.getMessage());
        }
        if (statements == null || CollectionUtils.isEmpty(statements.getStatements())
                || statements.getStatements().size() != 1) {
            throw exception(NL2SQL_SQL_INVALID, "只允许单条语句");
        }
        Statement statement = statements.getStatements().get(0);
        if (!(statement instanceof Select) || !(statement instanceof PlainSelect)) {
            throw exception(NL2SQL_SQL_INVALID, "只允许单条 SELECT（不含 UNION/DDL）");
        }
        Select select = (Select) statement;
        PlainSelect plainSelect = (PlainSelect) statement;
        if (plainSelect.getIntoTables() != null && !plainSelect.getIntoTables().isEmpty()) {
            throw exception(NL2SQL_SQL_INVALID, "禁止 SELECT INTO");
        }
        if (plainSelect.toString().toLowerCase(Locale.ROOT).contains(" for update")) {
            throw exception(NL2SQL_SQL_INVALID, "禁止 FOR UPDATE");
        }
        validateTables(select);
        rewritePlainSelect(plainSelect, schema);
        return plainSelect.toString();
    }

    private void validateTables(Select select) {
        Set<String> used;
        try {
            used = new LinkedHashSet<String>(new TablesNamesFinder().getTableList((Statement) select));
        } catch (Exception ex) {
            throw exception(NL2SQL_SQL_INVALID, "无法解析表名");
        }
        if (CollectionUtils.isEmpty(used)) {
            throw exception(NL2SQL_SQL_INVALID, "SQL 未引用任何表");
        }
        List<String> denied = new ArrayList<>();
        for (String table : used) {
            String name = unwrap(table).toLowerCase(Locale.ROOT);
            if (!tableMatcher.matches(name)) {
                denied.add(name);
            }
        }
        if (!denied.isEmpty()) {
            throw exception(NL2SQL_TABLE_DENIED, String.join(", ", denied));
        }
    }

    private void rewritePlainSelect(PlainSelect plainSelect, SchemaSnapshot schema) {
        capLimit(plainSelect);
        List<Table> tables = new ArrayList<>();
        collectTables(plainSelect.getFromItem(), tables, schema);
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                collectTables(join.getFromItem(), tables, schema);
            }
        }
        Expression extra = buildTenantAndDeleted(tables, schema);
        if (extra != null) {
            Expression where = plainSelect.getWhere();
            plainSelect.setWhere(where == null ? extra : new AndExpression(where, extra));
        }
    }

    private void collectTables(FromItem fromItem, List<Table> tables, SchemaSnapshot schema) {
        if (fromItem == null) {
            return;
        }
        if (fromItem instanceof Table) {
            tables.add((Table) fromItem);
            return;
        }
        if (fromItem instanceof ParenthesedSelect) {
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) fromItem;
            if (parenthesedSelect.getSelect() instanceof PlainSelect) {
                PlainSelect inner = (PlainSelect) parenthesedSelect.getSelect();
                collectTables(inner.getFromItem(), tables, schema);
                if (inner.getJoins() != null) {
                    for (Join join : inner.getJoins()) {
                        collectTables(join.getFromItem(), tables, schema);
                    }
                }
                rewritePlainSelect(inner, schema);
            }
        }
    }

    private Expression buildTenantAndDeleted(List<Table> tables, SchemaSnapshot schema) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            tenantId = defaultTenantId;
        }
        Expression all = null;
        for (Table table : tables) {
            String tableName = unwrap(table.getName());
            SchemaSnapshot.TableMeta meta = schema == null ? null : schema.getTable(tableName);
            String alias = table.getAlias() != null ? table.getAlias().getName() : tableName;
            if (meta != null && meta.hasColumn("tenant_id") && tenantId != null) {
                all = and(all, eq(alias, "tenant_id", new LongValue(tenantId)));
            }
            if (meta != null && meta.hasColumn("deleted")) {
                all = and(all, eq(alias, "deleted", new LongValue(0)));
            }
        }
        return all;
    }

    private void capLimit(PlainSelect plainSelect) {
        Limit limit = plainSelect.getLimit();
        if (limit == null || limit.getRowCount() == null) {
            Limit created = new Limit();
            created.setRowCount(new LongValue(maxRows));
            plainSelect.setLimit(created);
            return;
        }
        if (limit.getRowCount() instanceof LongValue) {
            LongValue longValue = (LongValue) limit.getRowCount();
            if (longValue.getValue() > maxRows) {
                limit.setRowCount(new LongValue(maxRows));
            }
        }
    }

    private static Expression and(Expression left, Expression right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return new AndExpression(left, right);
    }

    private static EqualsTo eq(String alias, String column, Expression value) {
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(new Table(alias), column));
        equalsTo.setRightExpression(value);
        return equalsTo;
    }

    private static String unwrap(String name) {
        return SchemaSnapshot.unwrap(name);
    }

    private static String stripTrailingSemicolons(String sql) {
        String result = sql.trim();
        while (result.endsWith(";")) {
            result = result.substring(0, result.length() - 1).trim();
        }
        return result;
    }

}
