package cn.simple.boot.module.system.service.sms;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.simple.boot.module.system.api.sms.dto.code.SmsCodeSendReqDTO;
import cn.simple.boot.module.system.api.sms.dto.code.SmsCodeUseReqDTO;
import cn.simple.boot.module.system.api.sms.dto.code.SmsCodeValidateReqDTO;
import cn.simple.boot.module.system.dal.dataobject.sms.SmsCodeDO;
import cn.simple.boot.module.system.dal.mysql.sms.SmsCodeMapper;
import cn.simple.boot.module.system.enums.sms.SmsSceneEnum;
import cn.simple.boot.module.system.framework.sms.config.SmsCodeProperties;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Objects;

import static cn.hutool.core.util.RandomUtil.randomInt;
import static cn.simple.boot.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.simple.boot.framework.common.util.date.DateUtils.isToday;
import static cn.simple.boot.module.system.enums.ErrorCodeConstants.*;

/**
 * 短信验证码 Service 实现类
 *
 * @author Simple Boot
 */
@Service
@Validated
public class SmsCodeServiceImpl implements SmsCodeService {

    private static final String VERIFY_FAIL_KEY = "sms_code:verify_fail:%d:%s";
    private static final long MAX_VERIFY_FAILURES = 5L;

    @Resource
    private SmsCodeProperties smsCodeProperties;

    @Resource
    private SmsCodeMapper smsCodeMapper;

    @Resource
    private SmsSendService smsSendService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void sendSmsCode(SmsCodeSendReqDTO reqDTO) {
        SmsSceneEnum sceneEnum = SmsSceneEnum.getCodeByScene(reqDTO.getScene());
        Assert.notNull(sceneEnum, "验证码场景({}) 查找不到配置", reqDTO.getScene());
        // 创建验证码
        String code = createSmsCode(reqDTO.getMobile(), reqDTO.getScene(), reqDTO.getCreateIp());
        // 发送验证码
        smsSendService.sendSingleSms(reqDTO.getMobile(), null, null,
                sceneEnum.getTemplateCode(), MapUtil.of("code", code));
    }

    private String createSmsCode(String mobile, Integer scene, String ip) {
        // 校验是否可以发送验证码，不用筛选场景
        SmsCodeDO lastSmsCode = smsCodeMapper.selectLastByMobile(mobile, null, null);
        if (lastSmsCode != null) {
            if (LocalDateTimeUtil.between(lastSmsCode.getCreateTime(), LocalDateTime.now()).toMillis()
                    < smsCodeProperties.getSendFrequency().toMillis()) { // 发送过于频繁
                throw exception(SMS_CODE_SEND_TOO_FAST);
            }
            if (isToday(lastSmsCode.getCreateTime()) && // 必须是今天，才能计算超过当天的上限
                    lastSmsCode.getTodayIndex() >= smsCodeProperties.getSendMaximumQuantityPerDay()) { // 超过当天发送的上限。
                throw exception(SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_DAY);
            }
            // TODO Admin：提升，每个 IP 每天可发送数量
            // TODO Admin：提升，每个 IP 每小时可发送数量
        }

        // 创建验证码记录
        String code = String.format("%0" + smsCodeProperties.getEndCode().toString().length() + "d",
                randomInt(smsCodeProperties.getBeginCode(), smsCodeProperties.getEndCode() + 1));
        SmsCodeDO newSmsCode = SmsCodeDO.builder().mobile(mobile).code(code).scene(scene)
                .todayIndex(lastSmsCode != null && isToday(lastSmsCode.getCreateTime()) ? lastSmsCode.getTodayIndex() + 1 : 1)
                .createIp(ip).used(false).build();
        smsCodeMapper.insert(newSmsCode);
        clearVerifyFailures(mobile, scene);
        return code;
    }

    @Override
    public void useSmsCode(SmsCodeUseReqDTO reqDTO) {
        // 检测验证码是否有效
        SmsCodeDO lastSmsCode = validateSmsCode0(reqDTO.getMobile(), reqDTO.getCode(), reqDTO.getScene());
        // 使用验证码
        smsCodeMapper.updateById(SmsCodeDO.builder().id(lastSmsCode.getId())
                .used(true).usedTime(LocalDateTime.now()).usedIp(reqDTO.getUsedIp()).build());
        clearVerifyFailures(reqDTO.getMobile(), reqDTO.getScene());
    }

    @Override
    public void validateSmsCode(SmsCodeValidateReqDTO reqDTO) {
        validateSmsCode0(reqDTO.getMobile(), reqDTO.getCode(), reqDTO.getScene());
    }

    private SmsCodeDO validateSmsCode0(String mobile, String code, Integer scene) {
        // 只查询当前场景的最新验证码，避免攻击者通过旧验证码绕过失败次数限制
        SmsCodeDO lastSmsCode = smsCodeMapper.selectLastByMobile(mobile, null, scene);
        if (lastSmsCode == null) {
            throw exception(SMS_CODE_NOT_FOUND);
        }
        if (!Objects.equals(lastSmsCode.getCode(), code)) {
            recordVerifyFailure(lastSmsCode);
            throw exception(SMS_CODE_NOT_FOUND);
        }
        // 超过时间
        if (LocalDateTimeUtil.between(lastSmsCode.getCreateTime(), LocalDateTime.now()).toMillis()
                >= smsCodeProperties.getExpireTimes().toMillis()) { // 验证码已过期
            throw exception(SMS_CODE_EXPIRED);
        }
        // 判断验证码是否已被使用
        if (Boolean.TRUE.equals(lastSmsCode.getUsed())) {
            throw exception(SMS_CODE_USED);
        }
        return lastSmsCode;
    }

    private void recordVerifyFailure(SmsCodeDO smsCode) {
        String key = formatVerifyFailKey(smsCode.getMobile(), smsCode.getScene());
        Long failures = stringRedisTemplate.opsForValue().increment(key);
        if (Objects.equals(failures, 1L)) {
            stringRedisTemplate.expire(key, smsCodeProperties.getExpireTimes());
        }
        if (failures != null && failures >= MAX_VERIFY_FAILURES) {
            smsCodeMapper.updateById(SmsCodeDO.builder().id(smsCode.getId())
                    .used(true).usedTime(LocalDateTime.now()).build());
        }
    }

    private void clearVerifyFailures(String mobile, Integer scene) {
        stringRedisTemplate.delete(formatVerifyFailKey(mobile, scene));
    }

    private static String formatVerifyFailKey(String mobile, Integer scene) {
        return String.format(VERIFY_FAIL_KEY, scene, mobile);
    }

}
