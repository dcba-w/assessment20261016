package cn.simple.boot.module.system.controller.admin.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - AI 指挥事件保存 Request VO")
@Data
public class AiCommandEventSaveReqVO {

    @Schema(description = "用户指令", requiredMode = Schema.RequiredMode.REQUIRED, example = "打开用户管理")
    @NotBlank(message = "指令不能为空")
    @Size(max = 1000, message = "指令长度不能超过 1000")
    private String task;

    @Schema(description = "发生页面", example = "/system/user")
    @Size(max = 255, message = "页面路径长度不能超过 255")
    private String pagePath;

    @Schema(description = "处理步骤，顺序即下次重放顺序", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "处理步骤不能为空")
    @Size(max = 80, message = "处理步骤不能超过 80 步")
    @Valid
    private List<AiCommandEventStepVO> steps;

}
