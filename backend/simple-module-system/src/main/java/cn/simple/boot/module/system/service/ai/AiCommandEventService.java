package cn.simple.boot.module.system.service.ai;

import cn.simple.boot.module.system.controller.admin.ai.vo.AiCommandEventRespVO;
import cn.simple.boot.module.system.controller.admin.ai.vo.AiCommandEventSaveReqVO;

import java.util.List;

public interface AiCommandEventService {

    /**
     * 保存一次成功的指挥事件。同一用户的同一指令会覆盖步骤，保留最新处理顺序。
     */
    Long saveEvent(AiCommandEventSaveReqVO reqVO);

    /**
     * 当前用户最近处理过的指令，按最近使用时间倒序。
     */
    List<AiCommandEventRespVO> getMyEventList();

}
