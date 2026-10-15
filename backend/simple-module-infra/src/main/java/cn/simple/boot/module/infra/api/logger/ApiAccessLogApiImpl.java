package cn.simple.boot.module.infra.api.logger;

import cn.simple.boot.framework.common.biz.infra.logger.ApiAccessLogCommonApi;
import cn.simple.boot.framework.common.biz.infra.logger.dto.ApiAccessLogCreateReqDTO;
import cn.simple.boot.module.infra.service.logger.ApiAccessLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * API 访问日志的 API 实现类
 *
 * @author Simple Boot
 */
@Service
@Validated
public class ApiAccessLogApiImpl implements ApiAccessLogCommonApi {

    @Resource
    private ApiAccessLogService apiAccessLogService;

    @Override
    public void createApiAccessLog(ApiAccessLogCreateReqDTO createDTO) {
        apiAccessLogService.createApiAccessLog(createDTO);
    }

}
