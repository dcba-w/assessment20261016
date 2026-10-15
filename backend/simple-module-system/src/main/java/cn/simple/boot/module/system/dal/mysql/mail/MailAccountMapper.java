package cn.simple.boot.module.system.dal.mysql.mail;

import cn.simple.boot.framework.common.pojo.PageResult;
import cn.simple.boot.framework.mybatis.core.mapper.BaseMapperX;
import cn.simple.boot.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.simple.boot.module.system.controller.admin.mail.vo.account.MailAccountPageReqVO;
import cn.simple.boot.module.system.dal.dataobject.mail.MailAccountDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MailAccountMapper extends BaseMapperX<MailAccountDO> {

    default PageResult<MailAccountDO> selectPage(MailAccountPageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<MailAccountDO>()
                .likeIfPresent(MailAccountDO::getMail, pageReqVO.getMail())
                .likeIfPresent(MailAccountDO::getUsername , pageReqVO.getUsername())
                .orderByDesc(MailAccountDO::getId));
    }

}
