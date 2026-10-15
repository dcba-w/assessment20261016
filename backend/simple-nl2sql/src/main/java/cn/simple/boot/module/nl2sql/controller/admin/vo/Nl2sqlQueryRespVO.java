package cn.simple.boot.module.nl2sql.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - NL2SQL 查询 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Nl2sqlQueryRespVO {

    @Schema(description = "面向用户的口语化中文回答")
    private String answer;
    @Builder.Default
    @Schema(description = "结果列名")
    private List<String> columns = new ArrayList<>();
    @Builder.Default
    @Schema(description = "结果列展示名，与 columns 按下标对齐，取自库表 COMMENT")
    private List<String> columnLabels = new ArrayList<>();
    @Builder.Default
    @Schema(description = "结果行")
    private List<Map<String, Object>> rows = new ArrayList<>();

}
