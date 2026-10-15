package cn.simple.boot.module.system.service.auth;

import cn.simple.boot.framework.test.core.ut.BaseMockitoUnitTest;
import cn.simple.boot.module.system.api.sms.SmsCodeApi;
import cn.simple.boot.module.system.controller.admin.auth.vo.AuthSmsSendReqVO;
import cn.simple.boot.module.system.enums.sms.SmsSceneEnum;
import cn.simple.boot.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdminAuthServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AdminAuthServiceImpl authService;

    @Mock
    private AdminUserService userService;
    @Mock
    private SmsCodeApi smsCodeApi;

    @Test
    void sendSmsCodeDoesNotRevealUnregisteredMobile() {
        AuthSmsSendReqVO request = AuthSmsSendReqVO.builder()
                .mobile("13800138000")
                .scene(SmsSceneEnum.ADMIN_MEMBER_LOGIN.getScene())
                .build();
        when(userService.getUserByMobile(request.getMobile())).thenReturn(null);

        assertDoesNotThrow(() -> authService.sendSmsCode(request));

        verifyNoInteractions(smsCodeApi);
    }
}
