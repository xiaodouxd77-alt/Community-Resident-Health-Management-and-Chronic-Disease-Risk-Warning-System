package com.cdvtc.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.dto.ArticleQueryRequest;
import com.cdvtc.demo.dto.NoticeQueryRequest;
import com.cdvtc.demo.service.HealthArticleService;
import com.cdvtc.demo.service.NoticeService;
import com.cdvtc.demo.vo.ArticleVO;
import com.cdvtc.demo.vo.NoticeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 居民端健康知识与社区公告：只读已发布（公告还需在有效期内）内容。
 */
@RestController
@RequestMapping("/api/resident")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESIDENT')")
public class ResidentContentController {

    private final HealthArticleService articleService;
    private final NoticeService noticeService;

    @GetMapping("/health-articles")
    public Result<IPage<ArticleVO>> articles(ArticleQueryRequest query) {
        return Result.success(articleService.pagePublished(query));
    }

    @GetMapping("/health-articles/{id}")
    public Result<ArticleVO> articleDetail(@PathVariable Long id) {
        return Result.success(articleService.getPublished(id));
    }

    @GetMapping("/notices")
    public Result<IPage<NoticeVO>> notices(NoticeQueryRequest query) {
        return Result.success(noticeService.pageEffective(query));
    }

    @GetMapping("/notices/{id}")
    public Result<NoticeVO> noticeDetail(@PathVariable Long id) {
        return Result.success(noticeService.getEffective(id));
    }
}
