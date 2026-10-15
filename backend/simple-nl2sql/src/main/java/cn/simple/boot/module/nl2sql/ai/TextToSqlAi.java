package cn.simple.boot.module.nl2sql.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface TextToSqlAi {

    @SystemMessage("""
            你是资深数据工程师。根据用户自然语言和提供的数据库 Schema 生成 MySQL SELECT。
            严格规则：
            1. 仅使用提供的表和字段，禁止编造
            2. 仅允许一条 SELECT，严禁 INSERT/UPDATE/DELETE/DDL/多语句
            3. 方言为 MySQL，不要用 PostgreSQL 语法
            4. 枚举字段使用 Schema 中的数字取值，不要写中文当条件值
            5. 不要手写 tenant_id 条件
            6. 只输出 JSON：{"sql":"...","explanation":"..."}，不要 Markdown，不要其它文字
            """)
    @UserMessage("""
            Schema:
            {{schema}}

            示例:
            {{fewShotExamples}}

            用户问题:
            {{question}}
            """)
    String generate(@V("schema") String schema,
                    @V("fewShotExamples") String fewShotExamples,
                    @V("question") String question);

    @UserMessage("""
            下列 MySQL SELECT 执行失败，请修正。只输出 JSON：{"sql":"...","explanation":"..."}。
            仍只能使用给定 Schema 中的表字段，且必须是一条 SELECT。
            Schema:
            {{schema}}
            原 SQL:
            {{sql}}
            错误:
            {{error}}
            """)
    String repair(@V("schema") String schema,
                  @V("sql") String sql,
                  @V("error") String error);

    @UserMessage("""
            用户用中文问：{{question}}
            下面是查询结果（JSON，最多 20 行）：
            {{resultJson}}
            请像同事口头汇报一样，用一两句通顺的中文直接回答。
            要求：先给结论，再补关键数字或名称；口语、准确；
            不要提 SQL、表名、字段名、租户、模型；不要输出 Markdown；
            没有数据就说没有查到符合条件的记录。只输出回答正文。
            """)
    String answer(@V("question") String question, @V("resultJson") String resultJson);

}
