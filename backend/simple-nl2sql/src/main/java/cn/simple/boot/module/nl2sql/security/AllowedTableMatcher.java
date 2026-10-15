package cn.simple.boot.module.nl2sql.security;

import cn.simple.boot.module.nl2sql.schema.SchemaSnapshot;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 表白名单匹配：精确表名，或 * 通配（如 system_*）。
 */
public final class AllowedTableMatcher {

    private static final String REGEX_META = ".[]{}()+^$|\\?";

    private final List<Pattern> patterns = new ArrayList<>();

    public AllowedTableMatcher(List<String> allowedTables) {
        if (allowedTables == null) {
            return;
        }
        for (String table : allowedTables) {
            if (!StringUtils.hasText(table)) {
                continue;
            }
            String glob = table.trim().toLowerCase(Locale.ROOT);
            patterns.add(Pattern.compile(globToRegex(glob)));
        }
    }

    public boolean isEmpty() {
        return patterns.isEmpty();
    }

    public boolean matches(String tableName) {
        if (!StringUtils.hasText(tableName) || patterns.isEmpty()) {
            return false;
        }
        String name = SchemaSnapshot.unwrap(tableName).toLowerCase(Locale.ROOT);
        for (Pattern pattern : patterns) {
            if (pattern.matcher(name).matches()) {
                return true;
            }
        }
        return false;
    }

    static String globToRegex(String glob) {
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if (c == '*') {
                regex.append(".*");
            } else if (REGEX_META.indexOf(c) >= 0) {
                regex.append('\\').append(c);
            } else {
                regex.append(c);
            }
        }
        regex.append('$');
        return regex.toString();
    }

}
