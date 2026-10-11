package com.cdvtc.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.dto.InterventionFeedbackRequest;
import com.cdvtc.demo.entity.HealthProfile;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.service.CurrentUserProvider;
import com.cdvtc.demo.service.HealthProfileService;
import com.cdvtc.demo.service.InterventionPlanService;
import com.cdvtc.demo.service.InterventionRecordService;
import com.cdvtc.demo.vo.InterventionPlanVO;
import com.cdvtc.demo.vo.InterventionRecordVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 居民端健康干预接口：仅限本人，且只返回允许向居民展示的方案与记录。
 * 数据归属完全由当前登录用户解析，不信任前端传入的 residentId。
 * 居民只能提交反馈，不能修改医生的方案、执行记录或方案状态。
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESIDENT')")
public class ResidentInterventionController {

    private final CurrentUserProvider currentUserProvider;
    private final HealthProfileService healthProfileService;
    private final InterventionPlanService interventionPlanService;
    private final InterventionRecordService interventionRecordService;

    @GetMapping("/intervention-plans")
    public Result<List<InterventionPlanVO>> myPlans() {
        HealthProfile profile = currentProfile();
        if (profile == null) {
            return Result.success(List.of());
        }
        return Result.success(interventionPlanService.listForResident(profile.getId()));
    }

    @GetMapping("/intervention-plans/{id}")
    public Result<InterventionPlanVO> myPlanDetail(@PathVariable Long id) {
        HealthProfile profile = requireProfile();
        return Result.success(interventionPlanService.getForResident(profile.getId(), id));
    }

    @GetMapping("/intervention-plans/{id}/records")
    public Result<IPage<InterventionRecordVO>> myPlanRecords(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        HealthProfile profile = requireProfile();
        return Result.success(interventionRecordService.listForResident(profile.getId(), id, page, size));
    }

    @PostMapping("/intervention-plans/{id}/feedback")
    public Result<InterventionRecordVO> submitFeedback(@PathVariable Long id,
                                                       @Valid @RequestBody InterventionFeedbackRequest request) {
        HealthProfile profile = requireProfile();
        User user = currentUserProvider.requireCurrentUser();
        return Result.success(interventionRecordService.addFeedback(
                profile.getId(), user.getId(), id, request));
    }

    private HealthProfile currentProfile() {
        User user = currentUserProvider.requireCurrentUser();
        return healthProfileService.getByUserId(user.getId());
    }

    private HealthProfile requireProfile() {
        HealthProfile profile = currentProfile();
        if (profile == null) {
            throw new BusinessException(404, "尚未建立健康档案");
        }
        return profile;
    }
}
