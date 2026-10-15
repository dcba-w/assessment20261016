package cn.simple.boot.module.system.convert.user;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import cn.simple.boot.framework.common.util.collection.CollectionUtils;
import cn.simple.boot.framework.common.util.collection.MapUtils;
import cn.simple.boot.framework.common.util.object.BeanUtils;
import cn.simple.boot.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import cn.simple.boot.module.system.controller.admin.dept.vo.post.PostSimpleRespVO;
import cn.simple.boot.module.system.controller.admin.permission.vo.role.RoleSimpleRespVO;
import cn.simple.boot.module.system.controller.admin.user.vo.profile.UserProfileRespVO;
import cn.simple.boot.module.system.controller.admin.user.vo.user.UserRespVO;
import cn.simple.boot.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
import cn.simple.boot.module.system.dal.dataobject.dept.DeptDO;
import cn.simple.boot.module.system.dal.dataobject.dept.PostDO;
import cn.simple.boot.module.system.dal.dataobject.permission.RoleDO;
import cn.simple.boot.module.system.dal.dataobject.user.AdminUserDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserConvert {

    UserConvert INSTANCE = Mappers.getMapper(UserConvert.class);

    default List<UserRespVO> convertList(List<AdminUserDO> list, Map<Long, DeptDO> deptMap) {
        return CollectionUtils.convertList(list, user -> {
            UserRespVO userVO = convert(user, deptMap.get(user.getDeptId()));
            // 列表/导出脱敏；详情 get 走 convert() 仍返回明文，避免编辑回填把 **** 写回库
            if (StrUtil.isNotBlank(userVO.getMobile())) {
                userVO.setMobile(DesensitizedUtil.mobilePhone(userVO.getMobile()));
            }
            return userVO;
        });
    }

    default UserRespVO convert(AdminUserDO user, DeptDO dept) {
        UserRespVO userVO = BeanUtils.toBean(user, UserRespVO.class);
        if (dept != null) {
            userVO.setDeptName(dept.getName());
        }
        return userVO;
    }

    default List<UserSimpleRespVO> convertSimpleList(List<AdminUserDO> list, Map<Long, DeptDO> deptMap) {
        return CollectionUtils.convertList(list, user -> {
            UserSimpleRespVO userVO = BeanUtils.toBean(user, UserSimpleRespVO.class);
            MapUtils.findAndThen(deptMap, user.getDeptId(), dept -> userVO.setDeptName(dept.getName()));
            return userVO;
        });
    }

    default UserProfileRespVO convert(AdminUserDO user, List<RoleDO> userRoles,
                                      DeptDO dept, List<PostDO> posts) {
        UserProfileRespVO userVO = BeanUtils.toBean(user, UserProfileRespVO.class);
        userVO.setRoles(BeanUtils.toBean(userRoles, RoleSimpleRespVO.class));
        userVO.setDept(BeanUtils.toBean(dept, DeptSimpleRespVO.class));
        userVO.setPosts(BeanUtils.toBean(posts, PostSimpleRespVO.class));
        return userVO;
    }

}
