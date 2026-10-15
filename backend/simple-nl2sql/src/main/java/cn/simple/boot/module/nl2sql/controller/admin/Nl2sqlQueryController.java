package cn.simple.boot.module.nl2sql.controller.admin;

import cn.simple.boot.framework.common.pojo.CommonResult;
import cn.simple.boot.module.nl2sql.controller.admin.vo.Nl2sqlQueryReqVO;
import cn.simple.boot.module.nl2sql.controller.admin.vo.Nl2sqlQueryRespVO;
import cn.simple.boot.module.nl2sql.service.Nl2sqlQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.simple.boot.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - NL2SQL")
@RestController
@RequestMapping("/nl2sql")
@Validated
public class Nl2sqlQueryController {

    @Resource
    private Nl2sqlQueryService nl2sqlQueryService;

    @PostMapping("/query")
    @Operation(summary = "自然语言查询")
    public CommonResult<Nl2sqlQueryRespVO> query(@Valid @RequestBody Nl2sqlQueryReqVO request) {
        return success(nl2sqlQueryService.query(request));
    }

}
