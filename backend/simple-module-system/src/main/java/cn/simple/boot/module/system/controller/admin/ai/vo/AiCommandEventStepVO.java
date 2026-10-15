package cn.simple.boot.module.system.controller.admin.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - AI 指挥事件步骤")
@Data
public class AiCommandEventStepVO {

    @Schema(description = "工具名", requiredMode = Schema.RequiredMode.REQUIRED, example = "click")
    @NotBlank(message = "工具名不能为空")
    private String tool;

    @Schema(description = "工具入参")
    private Object input;

    @Schema(description = "当时点中的元素文本，用于下次重新定位")
    private String elementText;

}
