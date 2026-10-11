package com.cdvtc.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.dto.NoticeCreateRequest;
import com.cdvtc.demo.dto.NoticeQueryRequest;
import com.cdvtc.demo.dto.NoticeUpdateRequest;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.service.CurrentUserProvider;
import com.cdvtc.demo.service.NoticeService;
import com.cdvtc.demo.vo.NoticeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端社区公告维护。
 */
@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNoticeController {

    private final CurrentUserProvider currentUserProvider;
    private final NoticeService noticeService;

    @PostMapping
    public Result<NoticeVO> create(@Valid @RequestBody NoticeCreateRequest request) {
        User admin = currentUserProvider.requireCurrentUser();
        return Result.success(noticeService.create(admin.getId(), request));
    }

    @GetMapping
    public Result<IPage<NoticeVO>> list(NoticeQueryRequest query) {
        return Result.success(noticeService.pageForAdmin(query));
    }

    @GetMapping("/{id}")
    public Result<NoticeVO> detail(@PathVariable Long id) {
        return Result.success(noticeService.getForAdmin(id));
    }

    @PutMapping("/{id}")
    public Result<NoticeVO> update(@PathVariable Long id, @Valid @RequestBody NoticeUpdateRequest request) {
        return Result.success(noticeService.update(id, request));
    }

    @PostMapping("/{id}/publish")
    public Result<NoticeVO> publish(@PathVariable Long id) {
        return Result.success(noticeService.publish(id));
    }

    @PostMapping("/{id}/offline")
    public Result<NoticeVO> offline(@PathVariable Long id) {
        return Result.success(noticeService.offline(id));
    }
}
