package com.cdvtc.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.dto.AlertHandleRequest;
import com.cdvtc.demo.dto.AlertQueryRequest;
import com.cdvtc.demo.dto.IndicatorQueryRequest;
import com.cdvtc.demo.dto.ResidentQueryRequest;
import com.cdvtc.demo.entity.HealthProfile;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.service.*;
import com.cdvtc.demo.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 医生端接口。所有查询与操作都会二次校验居民是否在医生授权范围内。
 */
@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorController {

    private final CurrentUserProvider currentUserProvider;
    private final DoctorResidentService doctorResidentService;
    private final HealthProfileService healthProfileService;
    private final IndicatorService indicatorService;
    private final AlertService alertService;
    private final ChronicDiseaseService chronicDiseaseService;
    private final ResidentGroupService residentGroupService;
    private final CommunityService communityService;

    // ============================ 预警 ============================

    @GetMapping("/alerts")
    public Result<IPage<AlertVO>> listAlerts(AlertQueryRequest query) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(alertService.pageDoctorAlerts(doctor.getId(), query));
    }

    @GetMapping("/alerts/{id}")
    public Result<AlertDetailVO> getAlert(@PathVariable Long id) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(alertService.getDoctorAlertDetail(doctor.getId(), id));
    }

    @GetMapping("/alerts/{id}/handling-records")
    public Result<List<HandlingRecordVO>> listHandlingRecords(@PathVariable Long id) {
        User doctor = currentUserProvider.requireCurrentUser();
        alertService.getDoctorAlertDetail(doctor.getId(), id);
        return Result.success(alertService.listHandlingRecords(id));
    }

    @PostMapping("/alerts/{id}/handling")
    public Result<AlertDetailVO> handleAlert(@PathVariable Long id,
                                             @Valid @RequestBody AlertHandleRequest request) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(alertService.handle(doctor.getId(), id, request));
    }

    // ============================ 居民档案 ============================

    /** 授权范围内的全部居民（供下拉选择，如预警过滤、分组成员选择）。 */
    @GetMapping("/residents")
    public Result<List<ResidentBriefVO>> listResidents() {
        User doctor = currentUserProvider.requireCurrentUser();
        List<Long> residentIds = doctorResidentService.listResidentIdsOfDoctor(doctor.getId());
        return Result.success(healthProfileService.listBriefByIds(residentIds));
    }

    /** 分页检索授权范围内的居民。 */
    @GetMapping("/residents/page")
    public Result<IPage<HealthProfileVO>> pageResidents(ResidentQueryRequest query) {
        User doctor = currentUserProvider.requireCurrentUser();
        List<Long> residentIds = doctorResidentService.listResidentIdsOfDoctor(doctor.getId());
        return Result.success(healthProfileService.pageResidents(residentIds, query));
    }

    @GetMapping("/communities")
    public Result<List<CommunityVO>> listCommunities() {
        return Result.success(communityService.listAll());
    }

    @GetMapping("/residents/{id}")
    public Result<HealthProfileVO> getResident(@PathVariable Long id) {
        User doctor = currentUserProvider.requireCurrentUser();
        requireInScope(doctor.getId(), id);
        HealthProfile profile = healthProfileService.getByIdRequired(id);
        User residentUser = healthProfileService.loadUser(profile.getUserId());
        return Result.success(healthProfileService.toVO(profile, residentUser));
    }

    @GetMapping("/residents/{id}/indicators")
    public Result<IPage<IndicatorRecordVO>> listResidentIndicators(@PathVariable Long id,
                                                                  IndicatorQueryRequest query) {
        User doctor = currentUserProvider.requireCurrentUser();
        requireInScope(doctor.getId(), id);
        return Result.success(indicatorService.pageResidentIndicators(id, query));
    }

    @GetMapping("/residents/{id}/assessments")
    public Result<IPage<AssessmentVO>> listResidentAssessments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        User doctor = currentUserProvider.requireCurrentUser();
        requireInScope(doctor.getId(), id);
        return Result.success(indicatorService.pageResidentAssessments(id, page, size));
    }

    @GetMapping("/residents/{id}/alerts")
    public Result<IPage<AlertVO>> listResidentAlerts(@PathVariable Long id, AlertQueryRequest query) {
        User doctor = currentUserProvider.requireCurrentUser();
        requireInScope(doctor.getId(), id);
        return Result.success(alertService.pageResidentAlerts(id, query));
    }

    @GetMapping("/residents/{id}/chronic-diseases")
    public Result<List<ChronicDiseaseVO>> listResidentChronicDiseases(@PathVariable Long id) {
        User doctor = currentUserProvider.requireCurrentUser();
        requireInScope(doctor.getId(), id);
        return Result.success(chronicDiseaseService.listByResident(id));
    }

    @GetMapping("/residents/{id}/groups")
    public Result<List<ResidentGroupVO>> listResidentGroups(@PathVariable Long id) {
        User doctor = currentUserProvider.requireCurrentUser();
        requireInScope(doctor.getId(), id);
        return Result.success(residentGroupService.listGroupsOfResident(id));
    }

    // ============================ 工具 ============================

    private void requireInScope(Long doctorId, Long residentId) {
        if (!doctorResidentService.isResidentInDoctorScope(doctorId, residentId)) {
            throw new BusinessException(403, "该居民不在您的管理范围内");
        }
    }
}
