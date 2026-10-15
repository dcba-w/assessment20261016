package cn.simple.boot.module.nl2sql.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - NL2SQL 查询 Request VO")
@Data
public class Nl2sqlQueryReqVO {

    @Schema(description = "自然语言问题", requiredMode = Schema.RequiredMode.REQUIRED, example = "有多少座桥梁？")
    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题长度不能超过 500")
    private String question;

}
