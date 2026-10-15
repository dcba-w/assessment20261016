package cn.simple.boot.module.system.controller.admin.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - AI 指挥事件 Response VO")
@Data
public class AiCommandEventRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "用户指令", example = "打开用户管理")
    private String task;

    @Schema(description = "发生页面", example = "/system/user")
    private String pagePath;

    @Schema(description = "步骤数量", example = "3")
    private Integer stepCount;

    @Schema(description = "处理步骤，按执行顺序")
    private List<AiCommandEventStepVO> steps;

    @Schema(description = "最近一次成功处理时间")
    private LocalDateTime updateTime;

}
