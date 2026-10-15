package cn.simple.boot.module.system.service.ai;

import cn.hutool.core.util.StrUtil;
import cn.simple.boot.framework.common.util.json.JsonUtils;
import cn.simple.boot.module.system.controller.admin.ai.vo.AiCommandEventRespVO;
import cn.simple.boot.module.system.controller.admin.ai.vo.AiCommandEventSaveReqVO;
import cn.simple.boot.module.system.controller.admin.ai.vo.AiCommandEventStepVO;
import cn.simple.boot.module.system.dal.dataobject.ai.AiCommandEventDO;
import cn.simple.boot.module.system.dal.mysql.ai.AiCommandEventMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;

import static cn.simple.boot.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.simple.boot.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.simple.boot.module.system.enums.ErrorCodeConstants.AI_COMMAND_EVENT_NOT_LOGIN;

@Service
@Validated
public class AiCommandEventServiceImpl implements AiCommandEventService {

    private static final int LIST_LIMIT = 30;

    @Resource
    private AiCommandEventMapper aiCommandEventMapper;

    @Override
    public Long saveEvent(AiCommandEventSaveReqVO reqVO) {
        Long userId = getLoginUserId();
        if (userId == null) {
            throw exception(AI_COMMAND_EVENT_NOT_LOGIN);
        }
        String task = normalizeTask(reqVO.getTask());
        AiCommandEventDO existing = aiCommandEventMapper.selectByUserAndTask(userId, task);
        AiCommandEventDO entity = existing != null ? existing : new AiCommandEventDO();
        entity.setUserId(userId);
        entity.setTask(task);
        entity.setPagePath(StrUtil.maxLength(StrUtil.blankToDefault(reqVO.getPagePath(), ""), 255));
        entity.setSteps(JsonUtils.toJsonString(reqVO.getSteps()));
        entity.setStepCount(reqVO.getSteps().size());
        if (existing == null) {
            aiCommandEventMapper.insert(entity);
        } else {
            aiCommandEventMapper.updateById(entity);
        }
        return entity.getId();
    }

    @Override
    public List<AiCommandEventRespVO> getMyEventList() {
        Long userId = getLoginUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        return aiCommandEventMapper.selectListByUserId(userId, LIST_LIMIT).stream()
                .map(this::toResp)
                .toList();
    }

    private AiCommandEventRespVO toResp(AiCommandEventDO entity) {
        AiCommandEventRespVO resp = new AiCommandEventRespVO();
        resp.setId(entity.getId());
        resp.setTask(entity.getTask());
        resp.setPagePath(entity.getPagePath());
        resp.setStepCount(entity.getStepCount());
        resp.setUpdateTime(entity.getUpdateTime());
        List<AiCommandEventStepVO> steps = JsonUtils.parseObjectQuietly(entity.getSteps(),
                new TypeReference<List<AiCommandEventStepVO>>() {});
        resp.setSteps(steps != null ? steps : Collections.emptyList());
        return resp;
    }

    private static String normalizeTask(String task) {
        return StrUtil.trim(task).replaceAll("\\s+", " ");
    }

}
