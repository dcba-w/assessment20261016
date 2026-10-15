package cn.simple.boot.framework.test.core.ut;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * 纯 Mockito 的单元测试基类
 *
 * 不加载 Spring 上下文，启动速度快，适合 Service 层的逻辑测试：
 *  1. 被测类使用 {@code @InjectMocks} 声明
 *  2. 被测类的依赖使用 {@code @Mock} 声明
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // 宽松模式，避免多余的 stub 直接导致测试失败
public class BaseMockitoUnitTest {
}
