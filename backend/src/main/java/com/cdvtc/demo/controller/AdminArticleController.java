package com.cdvtc.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.dto.ArticleCreateRequest;
import com.cdvtc.demo.dto.ArticleQueryRequest;
import com.cdvtc.demo.dto.ArticleUpdateRequest;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.service.CurrentUserProvider;
import com.cdvtc.demo.service.HealthArticleService;
import com.cdvtc.demo.vo.ArticleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端健康知识文章维护。
 */
@RestController
@RequestMapping("/api/admin/health-articles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminArticleController {

    private final CurrentUserProvider currentUserProvider;
    private final HealthArticleService articleService;

    @PostMapping
    public Result<ArticleVO> create(@Valid @RequestBody ArticleCreateRequest request) {
        User admin = currentUserProvider.requireCurrentUser();
        return Result.success(articleService.create(admin.getId(), request));
    }

    @GetMapping
    public Result<IPage<ArticleVO>> list(ArticleQueryRequest query) {
        return Result.success(articleService.pageForAdmin(query));
    }

    @GetMapping("/{id}")
    public Result<ArticleVO> detail(@PathVariable Long id) {
        return Result.success(articleService.getForAdmin(id));
    }

    @PutMapping("/{id}")
    public Result<ArticleVO> update(@PathVariable Long id, @Valid @RequestBody ArticleUpdateRequest request) {
        return Result.success(articleService.update(id, request));
    }

    @PostMapping("/{id}/publish")
    public Result<ArticleVO> publish(@PathVariable Long id) {
        return Result.success(articleService.publish(id));
    }

    @PostMapping("/{id}/offline")
    public Result<ArticleVO> offline(@PathVariable Long id) {
        return Result.success(articleService.offline(id));
    }
}
