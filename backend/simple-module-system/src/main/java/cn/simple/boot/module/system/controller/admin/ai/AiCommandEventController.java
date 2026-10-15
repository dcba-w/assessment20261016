package cn.simple.boot.module.system.controller.admin.ai;

import cn.simple.boot.framework.common.pojo.CommonResult;
import cn.simple.boot.module.system.controller.admin.ai.vo.AiCommandEventRespVO;
import cn.simple.boot.module.system.controller.admin.ai.vo.AiCommandEventSaveReqVO;
import cn.simple.boot.module.system.service.ai.AiCommandEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.simple.boot.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI 指挥事件")
@RestController
@RequestMapping("/system/ai-command-event")
@Validated
public class AiCommandEventController {

    @Resource
    private AiCommandEventService aiCommandEventService;

    @PostMapping("/save")
    @Operation(summary = "保存用户指挥 AI 的事件及处理顺序")
    public CommonResult<Long> saveEvent(@Valid @RequestBody AiCommandEventSaveReqVO reqVO) {
        return success(aiCommandEventService.saveEvent(reqVO));
    }

    @GetMapping("/list")
    @Operation(summary = "查询当前用户过往指挥记录，供吉祥物重新执行")
    public CommonResult<List<AiCommandEventRespVO>> getMyEventList() {
        return success(aiCommandEventService.getMyEventList());
    }

}
