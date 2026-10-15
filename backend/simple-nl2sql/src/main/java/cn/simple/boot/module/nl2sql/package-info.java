/**
 * NL2SQL 模块：自然语言转只读 SQL，经安全闸后再查当前数据源。
 *
 * 1. Controller URL：以 /nl2sql/ 开头，实际对外为 /admin-api/nl2sql/**
 * 2. 不落独立业务表；表白名单由 nl2sql.allowed-tables 配置，支持 * 通配
 */
package cn.simple.boot.module.nl2sql;
