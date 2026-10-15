package cn.simple.boot.module.system.dal.mysql.ai;

import cn.simple.boot.framework.mybatis.core.mapper.BaseMapperX;
import cn.simple.boot.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.simple.boot.module.system.dal.dataobject.ai.AiCommandEventDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AiCommandEventMapper extends BaseMapperX<AiCommandEventDO> {

    default AiCommandEventDO selectByUserAndTask(Long userId, String task) {
        return selectOne(new LambdaQueryWrapperX<AiCommandEventDO>()
                .eq(AiCommandEventDO::getUserId, userId)
                .eq(AiCommandEventDO::getTask, task));
    }

    default List<AiCommandEventDO> selectListByUserId(Long userId, int limit) {
        return selectList(new LambdaQueryWrapperX<AiCommandEventDO>()
                .eq(AiCommandEventDO::getUserId, userId)
                .orderByDesc(AiCommandEventDO::getUpdateTime)
                .last("LIMIT " + limit));
    }

}
