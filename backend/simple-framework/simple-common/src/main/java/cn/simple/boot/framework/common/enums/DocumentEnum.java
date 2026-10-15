package cn.simple.boot.framework.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文档地址
 *
 * @author Simple Boot
 */
@Getter
@AllArgsConstructor
public enum DocumentEnum {

    REDIS_INSTALL("https://example.com", "Redis 安装文档"),
    TENANT("https://example.com", "SaaS 多租户文档");

    private final String url;
    private final String memo;

}
