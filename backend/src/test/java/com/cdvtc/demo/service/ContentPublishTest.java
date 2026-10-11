package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.dto.ArticleCreateRequest;
import com.cdvtc.demo.dto.ArticleQueryRequest;
import com.cdvtc.demo.dto.NoticeCreateRequest;
import com.cdvtc.demo.dto.NoticeQueryRequest;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.enums.PublishStatus;
import com.cdvtc.demo.mapper.UserMapper;
import com.cdvtc.demo.vo.ArticleVO;
import com.cdvtc.demo.vo.NoticeVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 健康知识与公告的草稿/发布/下架/有效期测试。
 * 使用真实 MySQL，事务内完成回滚，不污染数据。
 */
@SpringBootTest
@Transactional
class ContentPublishTest {

    @Autowired
    private HealthArticleService articleService;
    @Autowired
    private NoticeService noticeService;
    @Autowired
    private UserMapper userMapper;

    private Long adminId() {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "admin")).getId();
    }

    private ArticleVO createArticle() {
        ArticleCreateRequest req = new ArticleCreateRequest();
        req.setTitle("高血压日常管理");
        req.setSummary("饮食与运动建议");
        req.setContent("低盐饮食，规律运动，按时测量血压。");
        req.setCategory("CHRONIC");
        return articleService.create(adminId(), req);
    }

    private NoticeVO createNotice(LocalDateTime from, LocalDateTime until) {
        NoticeCreateRequest req = new NoticeCreateRequest();
        req.setTitle("社区义诊通知");
        req.setContent("本周六上午在社区服务中心开展免费义诊。");
        req.setEffectiveFrom(from);
        req.setEffectiveUntil(until);
        req.setIsTop(true);
        return noticeService.create(adminId(), req);
    }

    private long countPublishedArticles() {
        ArticleQueryRequest q = new ArticleQueryRequest();
        q.setPage(1);
        q.setSize(100);
        return articleService.pagePublished(q).getRecords().stream()
                .filter(a -> a.getTitle().equals("高血压日常管理")).count();
    }

    @Test
    void 草稿居民不可见_发布后可见_下架后不可见() {
        ArticleVO article = createArticle();
        assertEquals(PublishStatus.DRAFT.name(), article.getPublishStatus());
        assertEquals(0, countPublishedArticles());
        assertEquals(404, assertThrows(BusinessException.class,
                () -> articleService.getPublished(article.getId())).getCode().intValue());

        ArticleVO published = articleService.publish(article.getId());
        assertEquals(PublishStatus.PUBLISHED.name(), published.getPublishStatus());
        assertNotNull(published.getPublishedAt());
        assertEquals(1, countPublishedArticles());

        ArticleVO offline = articleService.offline(article.getId());
        assertEquals(PublishStatus.OFFLINE.name(), offline.getPublishStatus());
        assertEquals(0, countPublishedArticles());
        assertEquals(404, assertThrows(BusinessException.class,
                () -> articleService.getPublished(article.getId())).getCode().intValue());
    }

    @Test
    void 重复发布被拒绝() {
        ArticleVO article = createArticle();
        articleService.publish(article.getId());
        assertEquals(409, assertThrows(BusinessException.class,
                () -> articleService.publish(article.getId())).getCode().intValue());
    }

    @Test
    void 仅已发布可下架() {
        ArticleVO article = createArticle();
        assertEquals(409, assertThrows(BusinessException.class,
                () -> articleService.offline(article.getId())).getCode().intValue());
    }

    @Test
    void 公告未发布居民不可见() {
        NoticeVO notice = createNotice(null, null);
        assertEquals(PublishStatus.DRAFT.name(), notice.getPublishStatus());
        assertEquals(404, assertThrows(BusinessException.class,
                () -> noticeService.getEffective(notice.getId())).getCode().intValue());
    }

    @Test
    void 公告有效期内可见_过期不可见() {
        NoticeVO active = createNotice(LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(7));
        noticeService.publish(active.getId());
        assertNotNull(noticeService.getEffective(active.getId()));

        NoticeVO expired = createNotice(null, LocalDateTime.now().minusHours(1));
        noticeService.publish(expired.getId());
        assertEquals(404, assertThrows(BusinessException.class,
                () -> noticeService.getEffective(expired.getId())).getCode().intValue());

        NoticeQueryRequest q = new NoticeQueryRequest();
        q.setPage(1);
        q.setSize(100);
        assertTrue(noticeService.pageEffective(q).getRecords().stream()
                .anyMatch(n -> n.getId().equals(active.getId())));
        assertFalse(noticeService.pageEffective(q).getRecords().stream()
                .anyMatch(n -> n.getId().equals(expired.getId())));
    }

    @Test
    void 生效时间晚于结束时间被拒绝() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> createNotice(LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(1)));
        assertEquals(400, ex.getCode().intValue());
    }

    @Test
    void 未发布公告的详情对居民不可见_下架后同样不可见() {
        NoticeVO notice = createNotice(null, null);
        noticeService.publish(notice.getId());
        assertNotNull(noticeService.getEffective(notice.getId()));
        noticeService.offline(notice.getId());
        assertEquals(404, assertThrows(BusinessException.class,
                () -> noticeService.getEffective(notice.getId())).getCode().intValue());
    }
}
