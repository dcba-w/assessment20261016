package cn.simple.boot.module.system.service.sms;

import cn.simple.boot.framework.test.core.ut.BaseMockitoUnitTest;
import cn.simple.boot.module.system.api.sms.dto.code.SmsCodeSendReqDTO;
import cn.simple.boot.module.system.api.sms.dto.code.SmsCodeUseReqDTO;
import cn.simple.boot.module.system.dal.dataobject.sms.SmsCodeDO;
import cn.simple.boot.module.system.dal.mysql.sms.SmsCodeMapper;
import cn.simple.boot.module.system.enums.sms.SmsSceneEnum;
import cn.simple.boot.module.system.framework.sms.config.SmsCodeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;

import static cn.simple.boot.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.simple.boot.module.system.enums.ErrorCodeConstants.SMS_CODE_EXPIRED;
import static cn.simple.boot.module.system.enums.ErrorCodeConstants.SMS_CODE_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmsCodeServiceImplTest extends BaseMockitoUnitTest {

    private static final String MOBILE = "13800138000";
    private static final Integer SCENE = SmsSceneEnum.ADMIN_MEMBER_LOGIN.getScene();

    @InjectMocks
    private SmsCodeServiceImpl smsCodeService;

    @Mock
    private SmsCodeProperties smsCodeProperties;
    @Mock
    private SmsCodeMapper smsCodeMapper;
    @Mock
    private SmsSendService smsSendService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        when(smsCodeProperties.getBeginCode()).thenReturn(100000);
        when(smsCodeProperties.getEndCode()).thenReturn(999999);
        when(smsCodeProperties.getExpireTimes()).thenReturn(Duration.ofMinutes(5));
    }

    @Test
    void sendSmsCodeGeneratesSixDigitRandomCode() {
        when(smsCodeMapper.selectLastByMobile(MOBILE, null, null)).thenReturn(null);

        smsCodeService.sendSmsCode(new SmsCodeSendReqDTO()
                .setMobile(MOBILE).setScene(SCENE).setCreateIp("127.0.0.1"));

        ArgumentCaptor<SmsCodeDO> captor = ArgumentCaptor.forClass(SmsCodeDO.class);
        verify(smsCodeMapper).insert(captor.capture());
        int code = Integer.parseInt(captor.getValue().getCode());
        assertEquals(6, captor.getValue().getCode().length());
        assertTrue(code >= 100000 && code <= 999999);
        verify(stringRedisTemplate).delete("sms_code:verify_fail:" + SCENE + ":" + MOBILE);
    }

    @Test
    void fifthInvalidAttemptInvalidatesCurrentCode() {
        SmsCodeDO current = currentCode("123456");
        when(smsCodeMapper.selectLastByMobile(MOBILE, null, SCENE)).thenReturn(current);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("sms_code:verify_fail:" + SCENE + ":" + MOBILE)).thenReturn(5L);

        assertServiceException(() -> smsCodeService.useSmsCode(useRequest("654321")), SMS_CODE_NOT_FOUND);

        ArgumentCaptor<SmsCodeDO> captor = ArgumentCaptor.forClass(SmsCodeDO.class);
        verify(smsCodeMapper).updateById(captor.capture());
        assertEquals(current.getId(), captor.getValue().getId());
        assertTrue(captor.getValue().getUsed());
    }

    @Test
    void successfulUseClearsFailureCounter() {
        SmsCodeDO current = currentCode("123456");
        when(smsCodeMapper.selectLastByMobile(MOBILE, null, SCENE)).thenReturn(current);

        smsCodeService.useSmsCode(useRequest("123456"));

        verify(smsCodeMapper).updateById(any(SmsCodeDO.class));
        verify(stringRedisTemplate).delete("sms_code:verify_fail:" + SCENE + ":" + MOBILE);
    }

    @Test
    void expiredCodeIsRejected() {
        SmsCodeDO current = currentCode("123456");
        current.setCreateTime(LocalDateTime.now().minusMinutes(6));
        when(smsCodeProperties.getExpireTimes()).thenReturn(Duration.ofMinutes(5));
        when(smsCodeMapper.selectLastByMobile(MOBILE, null, SCENE)).thenReturn(current);

        assertServiceException(() -> smsCodeService.useSmsCode(useRequest("123456")), SMS_CODE_EXPIRED);
    }

    private static SmsCodeDO currentCode(String code) {
        SmsCodeDO smsCode = SmsCodeDO.builder().id(1L).mobile(MOBILE).scene(SCENE).code(code)
                .used(false).build();
        smsCode.setCreateTime(LocalDateTime.now());
        return smsCode;
    }

    private static SmsCodeUseReqDTO useRequest(String code) {
        return new SmsCodeUseReqDTO().setMobile(MOBILE).setScene(SCENE)
                .setCode(code).setUsedIp("127.0.0.1");
    }
}
