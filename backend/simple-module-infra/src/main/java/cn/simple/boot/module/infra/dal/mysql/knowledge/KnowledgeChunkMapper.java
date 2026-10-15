package cn.simple.boot.module.infra.dal.mysql.knowledge;

import cn.simple.boot.framework.mybatis.core.mapper.BaseMapperX;
import cn.simple.boot.module.infra.dal.dataobject.knowledge.KnowledgeChunkDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgeChunkMapper extends BaseMapperX<KnowledgeChunkDO> {
}
