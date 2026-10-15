package cn.simple.boot.module.nl2sql.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlGenerationResult {

    private String sql;
    private String explanation;

}
