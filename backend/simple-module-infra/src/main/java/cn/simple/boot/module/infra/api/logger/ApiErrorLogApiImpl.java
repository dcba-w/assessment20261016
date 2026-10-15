package cn.simple.boot.module.infra.api.logger;

import cn.simple.boot.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import cn.simple.boot.framework.common.biz.infra.logger.dto.ApiErrorLogCreateReqDTO;
import cn.simple.boot.module.infra.service.logger.ApiErrorLogService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;

/**
 * API 访问日志的 API 接口
 *
 * @author Simple Boot
 */
@Service
@Validated
public class ApiErrorLogApiImpl implements ApiErrorLogCommonApi {

    @Resource
    private ApiErrorLogService apiErrorLogService;

    @Override
    public void createApiErrorLog(ApiErrorLogCreateReqDTO createDTO) {
        apiErrorLogService.createApiErrorLog(createDTO);
    }

}
