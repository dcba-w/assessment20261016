package cn.simple.boot.framework.test.core.util;

import cn.simple.boot.framework.common.exception.ErrorCode;
import cn.simple.boot.framework.common.exception.ServiceException;
import cn.simple.boot.framework.common.exception.util.ServiceExceptionUtil;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 单元测试的断言工具类
 */
public class AssertUtils {

    /**
     * 断言执行会抛出指定错误码的 {@link ServiceException}
     *
     * @param executable 被断言的执行逻辑
     * @param errorCode  期望的错误码
     * @param params     错误提示的占位符参数
     */
    public static void assertServiceException(Executable executable, ErrorCode errorCode, Object... params) {
        ServiceException exception = assertThrows(ServiceException.class, executable);
        assertEquals(errorCode.getCode(), exception.getCode());
        assertEquals(ServiceExceptionUtil.doFormat(errorCode.getCode(), errorCode.getMsg(), params),
                exception.getMessage());
    }

}
