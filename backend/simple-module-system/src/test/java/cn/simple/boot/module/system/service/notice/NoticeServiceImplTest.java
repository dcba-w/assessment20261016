package cn.simple.boot.module.system.service.notice;

import cn.simple.boot.framework.test.core.ut.BaseMockitoUnitTest;
import cn.simple.boot.module.system.controller.admin.notice.vo.NoticeSaveReqVO;
import cn.simple.boot.module.system.dal.dataobject.notice.NoticeDO;
import cn.simple.boot.module.system.dal.mysql.notice.NoticeMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.simple.boot.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.simple.boot.module.system.enums.ErrorCodeConstants.NOTICE_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link NoticeServiceImpl} 的单元测试，同时作为 Mockito 测试的示例
 */
class NoticeServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private NoticeServiceImpl noticeService;

    @Mock
    private NoticeMapper noticeMapper;

    @Test
    void testCreateNotice_success() {
        // 准备参数
        NoticeSaveReqVO reqVO = new NoticeSaveReqVO();
        reqVO.setTitle("测试公告");
        reqVO.setType(1);
        reqVO.setContent("测试内容");
        reqVO.setStatus(0);
        // mock 插入时由数据库回填主键
        when(noticeMapper.insert(any(NoticeDO.class))).thenAnswer(invocation -> {
            NoticeDO notice = invocation.getArgument(0);
            notice.setId(1024L);
            return 1;
        });

        // 调用
        Long noticeId = noticeService.createNotice(reqVO);

        // 断言
        assertEquals(1024L, noticeId);
        verify(noticeMapper).insert(any(NoticeDO.class));
    }

    @Test
    void testUpdateNotice_notExists() {
        // 准备参数
        NoticeSaveReqVO reqVO = new NoticeSaveReqVO();
        reqVO.setId(1024L);
        // mock 公告不存在
        when(noticeMapper.selectById(eq(1024L))).thenReturn(null);

        // 调用并断言抛出业务异常
        assertServiceException(() -> noticeService.updateNotice(reqVO), NOTICE_NOT_FOUND);
    }

    @Test
    void testDeleteNotice_success() {
        // mock 公告存在
        NoticeDO notice = new NoticeDO();
        notice.setId(1024L);
        when(noticeMapper.selectById(eq(1024L))).thenReturn(notice);

        // 调用
        noticeService.deleteNotice(1024L);

        // 断言
        verify(noticeMapper).deleteById(eq(1024L));
    }

}
