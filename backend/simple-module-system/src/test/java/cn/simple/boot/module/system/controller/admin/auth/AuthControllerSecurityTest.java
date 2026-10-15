package cn.simple.boot.module.system.controller.admin.auth;

import cn.simple.boot.framework.ratelimiter.core.annotation.RateLimiter;
import cn.simple.boot.module.system.controller.admin.auth.vo.AuthLoginReqVO;
import cn.simple.boot.module.system.controller.admin.auth.vo.AuthResetPasswordReqVO;
import cn.simple.boot.module.system.controller.admin.auth.vo.AuthSmsLoginReqVO;
import cn.simple.boot.module.system.controller.admin.auth.vo.AuthSmsSendReqVO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthControllerSecurityTest {

    @Test
    void authEndpointsUseIndependentRateLimitKeys() throws Exception {
        assertRateLimit("login", AuthLoginReqVO.class, 10, "'login:' + #reqVO.username");
        assertRateLimit("smsLogin", AuthSmsLoginReqVO.class, 5, "'sms-login:' + #reqVO.mobile");
        assertRateLimit("sendLoginSmsCode", AuthSmsSendReqVO.class, 3, "'sms-send:' + #reqVO.mobile");
        assertRateLimit("resetPassword", AuthResetPasswordReqVO.class, 5, "'sms-reset:' + #reqVO.mobile");
    }

    private static void assertRateLimit(String methodName, Class<?> parameterType,
                                        int expectedCount, String expectedKey) throws Exception {
        Method method = AuthController.class.getMethod(methodName, parameterType);
        RateLimiter limiter = method.getAnnotation(RateLimiter.class);
        assertEquals(60, limiter.time());
        assertEquals(expectedCount, limiter.count());
        assertEquals(expectedKey, limiter.keyArg());
    }
}
