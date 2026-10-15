package cn.simple.boot.module.system.api.logger;

import cn.simple.boot.framework.common.biz.system.logger.OperateLogCommonApi;
import cn.simple.boot.framework.common.pojo.PageResult;
import cn.simple.boot.module.system.api.logger.dto.OperateLogPageReqDTO;
import cn.simple.boot.module.system.api.logger.dto.OperateLogRespDTO;

/**
 * 操作日志 API 接口
 *
 * @author Simple Boot
 */
public interface OperateLogApi extends OperateLogCommonApi {

    /**
     * 获取指定模块的指定数据的操作日志分页
     *
     * @param pageReqDTO 请求
     * @return 操作日志分页
     */
    PageResult<OperateLogRespDTO> getOperateLogPage(OperateLogPageReqDTO pageReqDTO);

}
