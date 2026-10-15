package cn.simple.boot.module.nl2sql.enums;

import cn.simple.boot.framework.common.exception.ErrorCode;

/**
 * NL2SQL 错误码枚举类
 *
 * nl2sql 模块，使用 1-024-000-000 段
 */
public interface ErrorCodeConstants {

    ErrorCode NL2SQL_API_KEY_MISSING = new ErrorCode(1_024_000_000,
            "未配置云端模型 API Key，请设置 nl2sql.api-key 或环境变量 NL2SQL_API_KEY");
    ErrorCode NL2SQL_SQL_GENERATE_FAIL = new ErrorCode(1_024_000_001, "模型未能生成可用 SQL：{}");
    ErrorCode NL2SQL_SQL_INVALID = new ErrorCode(1_024_000_002, "生成的 SQL 未通过安全校验：{}");
    ErrorCode NL2SQL_TABLE_DENIED = new ErrorCode(1_024_000_003, "SQL 引用了未授权的表：{}");
    ErrorCode NL2SQL_QUERY_FAIL = new ErrorCode(1_024_000_004, "查询执行失败：{}");
    ErrorCode NL2SQL_SCHEMA_EMPTY = new ErrorCode(1_024_000_005,
            "表白名单为空或数据源中找不到这些表，请检查 nl2sql.allowed-tables");

}
