package com.cdvtc.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.dto.AlertQueryRequest;
import com.cdvtc.demo.dto.HealthProfileUpdateRequest;
import com.cdvtc.demo.dto.IndicatorCreateRequest;
import com.cdvtc.demo.dto.IndicatorQueryRequest;
import com.cdvtc.demo.entity.HealthProfile;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.service.AlertService;
import com.cdvtc.demo.service.ChronicDiseaseService;
import com.cdvtc.demo.service.CommunityService;
import com.cdvtc.demo.service.CurrentUserProvider;
import com.cdvtc.demo.service.HealthProfileService;
import com.cdvtc.demo.service.IndicatorService;
import com.cdvtc.demo.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 居民端接口。所有数据归属均由当前登录用户解析，且仅限本人。
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESIDENT')")
public class HealthController {

    private final CurrentUserProvider currentUserProvider;
    private final HealthProfileService healthProfileService;
    private final IndicatorService indicatorService;
    private final AlertService alertService;
    private final ChronicDiseaseService chronicDiseaseService;
    private final CommunityService communityService;

    @GetMapping("/profile")
    public Result<HealthProfileVO> getProfile() {
        User user = currentUserProvider.requireCurrentUser();
        HealthProfile profile = healthProfileService.getByUserId(user.getId());
        return Result.success(profile == null ? null : healthProfileService.toVO(profile, user));
    }

    @PostMapping("/profile")
    public Result<HealthProfileVO> createProfile(@Valid @RequestBody HealthProfileUpdateRequest request) {
        User user = currentUserProvider.requireCurrentUser();
        return Result.success(healthProfileService.createProfile(user, request));
    }

    @PutMapping("/profile")
    public Result<HealthProfileVO> updateProfile(@Valid @RequestBody HealthProfileUpdateRequest request) {
        User user = currentUserProvider.requireCurrentUser();
        return Result.success(healthProfileService.updateProfile(user, request));
    }

    /** 社区参照数据，供居民选择所属社区。 */
    @GetMapping("/communities")
    public Result<List<CommunityVO>> listCommunities() {
        return Result.success(communityService.listAll());
    }

    /** 我的慢病档案（仅本人，且不可修改资料状态）。 */
    @GetMapping("/chronic-diseases")
    public Result<List<ChronicDiseaseVO>> listMyChronicDiseases() {
        User user = currentUserProvider.requireCurrentUser();
        HealthProfile profile = healthProfileService.getByUserId(user.getId());
        if (profile == null) {
            return Result.success(List.of());
        }
        return Result.success(chronicDiseaseService.listByResident(profile.getId()));
    }

    @PostMapping("/indicators")
    public Result<IndicatorSubmitResultVO> createIndicator(@Valid @RequestBody IndicatorCreateRequest request) {
        User user = currentUserProvider.requireCurrentUser();
        return Result.success(indicatorService.submit(user, request));
    }

    @GetMapping("/indicators")
    public Result<IPage<IndicatorRecordVO>> listIndicators(IndicatorQueryRequest query) {
        User user = currentUserProvider.requireCurrentUser();
        HealthProfile profile = healthProfileService.getByUserId(user.getId());
        if (profile == null) {
            return Result.success(emptyPage(query.getPage(), query.getSize()));
        }
        return Result.success(indicatorService.pageResidentIndicators(profile.getId(), query));
    }

    @GetMapping("/indicators/{id}")
    public Result<IndicatorRecordVO> getIndicator(@PathVariable Long id) {
        User user = currentUserProvider.requireCurrentUser();
        HealthProfile profile = requireProfile(user);
        return Result.success(indicatorService.getResidentIndicator(profile.getId(), id));
    }

    @GetMapping("/assessments")
    public Result<IPage<AssessmentVO>> listAssessments(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        User user = currentUserProvider.requireCurrentUser();
        HealthProfile profile = healthProfileService.getByUserId(user.getId());
        if (profile == null) {
            return Result.success(emptyPage(page, size));
        }
        return Result.success(indicatorService.pageResidentAssessments(profile.getId(), page, size));
    }

    @GetMapping("/alerts")
    public Result<IPage<AlertVO>> listAlerts(AlertQueryRequest query) {
        User user = currentUserProvider.requireCurrentUser();
        HealthProfile profile = healthProfileService.getByUserId(user.getId());
        if (profile == null) {
            return Result.success(emptyPage(query.getPage(), query.getSize()));
        }
        return Result.success(alertService.pageResidentAlerts(profile.getId(), query));
    }

    @GetMapping("/alerts/{id}")
    public Result<AlertDetailVO> getAlert(@PathVariable Long id) {
        User user = currentUserProvider.requireCurrentUser();
        HealthProfile profile = requireProfile(user);
        return Result.success(alertService.getResidentAlertDetail(profile.getId(), id));
    }

    private HealthProfile requireProfile(User user) {
        HealthProfile profile = healthProfileService.getByUserId(user.getId());
        if (profile == null) {
            throw new BusinessException(404, "尚未建立健康档案");
        }
        return profile;
    }

    private <T> IPage<T> emptyPage(Integer page, Integer size) {
        return new Page<>(page == null || page < 1 ? 1 : page,
                size == null || size < 1 ? 10 : size, 0);
    }
}
