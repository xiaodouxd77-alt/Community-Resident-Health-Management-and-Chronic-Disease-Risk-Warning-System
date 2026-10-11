package com.cdvtc.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.entity.HealthProfile;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.service.CurrentUserProvider;
import com.cdvtc.demo.service.FollowupPlanService;
import com.cdvtc.demo.service.FollowupRecordService;
import com.cdvtc.demo.service.HealthProfileService;
import com.cdvtc.demo.vo.FollowupPlanVO;
import com.cdvtc.demo.vo.FollowupRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 居民端随访接口：仅限本人，且只返回允许向居民展示的计划与记录。
 * 数据归属完全由当前登录用户解析，不信任前端传入的 residentId。
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESIDENT')")
public class ResidentFollowupController {

    private final CurrentUserProvider currentUserProvider;
    private final HealthProfileService healthProfileService;
    private final FollowupPlanService followupPlanService;
    private final FollowupRecordService followupRecordService;

    @GetMapping("/followup-plans")
    public Result<List<FollowupPlanVO>> myPlans() {
        HealthProfile profile = currentProfile();
        if (profile == null) {
            return Result.success(List.of());
        }
        return Result.success(followupPlanService.listForResident(profile.getId()));
    }

    @GetMapping("/followup-records")
    public Result<IPage<FollowupRecordVO>> myRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        HealthProfile profile = currentProfile();
        if (profile == null) {
            return Result.success(new Page<>(page, size, 0));
        }
        return Result.success(followupRecordService.listForResident(profile.getId(), page, size));
    }

    @GetMapping("/followup-records/{id}")
    public Result<FollowupRecordVO> myRecordDetail(@PathVariable Long id) {
        HealthProfile profile = currentProfile();
        if (profile == null) {
            throw new BusinessException(404, "尚未建立健康档案");
        }
        return Result.success(followupRecordService.getForResident(profile.getId(), id));
    }

    private HealthProfile currentProfile() {
        User user = currentUserProvider.requireCurrentUser();
        return healthProfileService.getByUserId(user.getId());
    }
}
