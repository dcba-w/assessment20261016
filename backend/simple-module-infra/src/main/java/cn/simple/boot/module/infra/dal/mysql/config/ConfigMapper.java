package cn.simple.boot.module.infra.dal.mysql.config;

import cn.simple.boot.framework.common.pojo.PageResult;
import cn.simple.boot.framework.mybatis.core.mapper.BaseMapperX;
import cn.simple.boot.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.simple.boot.module.infra.controller.admin.config.vo.ConfigPageReqVO;
import cn.simple.boot.module.infra.dal.dataobject.config.ConfigDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConfigMapper extends BaseMapperX<ConfigDO> {

    default ConfigDO selectByKey(String key) {
        return selectOne(ConfigDO::getConfigKey, key);
    }

    default PageResult<ConfigDO> selectPage(ConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ConfigDO>()
                .likeIfPresent(ConfigDO::getName, reqVO.getName())
                .likeIfPresent(ConfigDO::getConfigKey, reqVO.getKey())
                .eqIfPresent(ConfigDO::getType, reqVO.getType())
                .betweenIfPresent(ConfigDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ConfigDO::getId));
    }

}
