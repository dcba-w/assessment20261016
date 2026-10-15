package cn.simple.boot.module.infra.dal.mysql.demo.demo03.erp;

import cn.simple.boot.framework.common.pojo.PageResult;
import cn.simple.boot.framework.mybatis.core.mapper.BaseMapperX;
import cn.simple.boot.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.simple.boot.module.infra.controller.admin.demo.demo03.erp.vo.Demo03StudentErpPageReqVO;
import cn.simple.boot.module.infra.dal.dataobject.demo.demo03.Demo03StudentDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学生 Mapper
 *
 * @author Simple Boot
 */
@Mapper
public interface Demo03StudentErpMapper extends BaseMapperX<Demo03StudentDO> {

    default PageResult<Demo03StudentDO> selectPage(Demo03StudentErpPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<Demo03StudentDO>()
                .likeIfPresent(Demo03StudentDO::getName, reqVO.getName())
                .eqIfPresent(Demo03StudentDO::getSex, reqVO.getSex())
                .eqIfPresent(Demo03StudentDO::getDescription, reqVO.getDescription())
                .betweenIfPresent(Demo03StudentDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(Demo03StudentDO::getId));
    }

}