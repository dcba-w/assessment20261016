package cn.simple.boot.module.infra.dal.mysql.demo.demo03.inner;

import cn.simple.boot.framework.common.pojo.PageResult;
import cn.simple.boot.framework.mybatis.core.mapper.BaseMapperX;
import cn.simple.boot.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.simple.boot.module.infra.controller.admin.demo.demo03.inner.vo.Demo03StudentInnerPageReqVO;
import cn.simple.boot.module.infra.dal.dataobject.demo.demo03.Demo03StudentDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学生 Mapper
 *
 * @author Simple Boot
 */
@Mapper
public interface Demo03StudentInnerMapper extends BaseMapperX<Demo03StudentDO> {

    default PageResult<Demo03StudentDO> selectPage(Demo03StudentInnerPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<Demo03StudentDO>()
                .likeIfPresent(Demo03StudentDO::getName, reqVO.getName())
                .eqIfPresent(Demo03StudentDO::getSex, reqVO.getSex())
                .eqIfPresent(Demo03StudentDO::getDescription, reqVO.getDescription())
                .betweenIfPresent(Demo03StudentDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(Demo03StudentDO::getId));
    }

}