package cn.simple.boot.module.nl2sql.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllowedTableMatcherTest {

    @Test
    void matchesExactTableName() {
        AllowedTableMatcher matcher = new AllowedTableMatcher(List.of("system_users"));
        assertTrue(matcher.matches("system_users"));
        assertTrue(matcher.matches("SYSTEM_USERS"));
        assertFalse(matcher.matches("system_role"));
    }

    @Test
    void matchesModulePrefixWildcard() {
        AllowedTableMatcher matcher = new AllowedTableMatcher(List.of("system_*"));
        assertTrue(matcher.matches("system_users"));
        assertTrue(matcher.matches("system_role"));
        assertTrue(matcher.matches("system_oauth2_access_token"));
        assertFalse(matcher.matches("infra_file"));
        assertFalse(matcher.matches("systemusers"));
        assertFalse(matcher.matches("inspect_system_users"));
    }

    @Test
    void matchesSuffixAndMiddleWildcard() {
        AllowedTableMatcher matcher = new AllowedTableMatcher(List.of("*_users", "inspect_*_issue"));
        assertTrue(matcher.matches("system_users"));
        assertTrue(matcher.matches("inspect_record_issue"));
        assertFalse(matcher.matches("inspect_record"));
    }

    @Test
    void starAllowsEveryTable() {
        AllowedTableMatcher matcher = new AllowedTableMatcher(List.of("*"));
        assertTrue(matcher.matches("system_users"));
        assertTrue(matcher.matches("inspect_facility"));
    }

    @Test
    void emptyPatternsMatchNothing() {
        AllowedTableMatcher matcher = new AllowedTableMatcher(List.of());
        assertTrue(matcher.isEmpty());
        assertFalse(matcher.matches("system_users"));
    }

}
