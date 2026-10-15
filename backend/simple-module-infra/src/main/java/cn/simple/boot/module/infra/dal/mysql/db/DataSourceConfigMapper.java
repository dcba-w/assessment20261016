package cn.simple.boot.module.infra.dal.mysql.db;

import cn.simple.boot.framework.mybatis.core.mapper.BaseMapperX;
import cn.simple.boot.module.infra.dal.dataobject.db.DataSourceConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据源配置 Mapper
 *
 * @author Simple Boot
 */
@Mapper
public interface DataSourceConfigMapper extends BaseMapperX<DataSourceConfigDO> {
}
