package com.cdvtc.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.dto.FollowupPlanCreateRequest;
import com.cdvtc.demo.dto.FollowupPlanQueryRequest;
import com.cdvtc.demo.dto.FollowupPlanUpdateRequest;
import com.cdvtc.demo.dto.FollowupRecordCreateRequest;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.service.CurrentUserProvider;
import com.cdvtc.demo.service.FollowupPlanService;
import com.cdvtc.demo.service.FollowupRecordService;
import com.cdvtc.demo.vo.FollowupCompleteVO;
import com.cdvtc.demo.vo.FollowupPlanVO;
import com.cdvtc.demo.vo.FollowupRecordVO;
import com.cdvtc.demo.vo.FollowupStatsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 医生端随访接口。所有操作都会二次校验居民是否在医生的授权范围内。
 */
@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorFollowupController {

    private final CurrentUserProvider currentUserProvider;
    private final FollowupPlanService followupPlanService;
    private final FollowupRecordService followupRecordService;

    // ---------------- 随访计划 ----------------

    @PostMapping("/followup-plans")
    public Result<FollowupPlanVO> createPlan(@Valid @RequestBody FollowupPlanCreateRequest request) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(followupPlanService.create(doctor.getId(), request));
    }

    @GetMapping("/followup-plans")
    public Result<IPage<FollowupPlanVO>> listPlans(FollowupPlanQueryRequest query) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(followupPlanService.pageForDoctor(doctor.getId(), query));
    }

    /** 待办计划（待执行/进行中，按计划时间升序）。 */
    @GetMapping("/followup-plans/todo")
    public Result<List<FollowupPlanVO>> todoPlans(@RequestParam(defaultValue = "10") Integer limit) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(followupPlanService.todo(doctor.getId(), limit));
    }

    /** 即将到期或已逾期（默认 7 天内）。 */
    @GetMapping("/followup-plans/upcoming")
    public Result<List<FollowupPlanVO>> upcomingPlans(@RequestParam(defaultValue = "7") Integer days) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(followupPlanService.upcoming(doctor.getId(), days));
    }

    /** 工作台统计（真实数据）。 */
    @GetMapping("/followup-plans/stats")
    public Result<FollowupStatsVO> planStats() {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(followupPlanService.stats(doctor.getId()));
    }

    @GetMapping("/followup-plans/{id}")
    public Result<FollowupPlanVO> getPlan(@PathVariable Long id) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(followupPlanService.getForDoctor(doctor.getId(), id));
    }

    @PutMapping("/followup-plans/{id}")
    public Result<FollowupPlanVO> updatePlan(@PathVariable Long id,
                                             @Valid @RequestBody FollowupPlanUpdateRequest request) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(followupPlanService.update(doctor.getId(), id, request));
    }

    @PostMapping("/followup-plans/{id}/cancel")
    public Result<FollowupPlanVO> cancelPlan(@PathVariable Long id) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(followupPlanService.cancel(doctor.getId(), id));
    }

    @PostMapping("/followup-plans/{id}/complete")
    public Result<FollowupPlanVO> completePlan(@PathVariable Long id) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(followupRecordService.completePlan(doctor.getId(), id));
    }

    // ---------------- 随访记录 ----------------

    @PostMapping("/followup-plans/{planId}/records")
    public Result<FollowupCompleteVO> createRecord(@PathVariable Long planId,
                                                   @Valid @RequestBody FollowupRecordCreateRequest request) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(followupRecordService.createRecord(doctor.getId(), planId, request));
    }

    @GetMapping("/followup-plans/{planId}/records")
    public Result<IPage<FollowupRecordVO>> listRecords(
            @PathVariable Long planId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(followupRecordService.pageForPlan(doctor.getId(), planId, page, size));
    }

    @GetMapping("/followup-records/{id}")
    public Result<FollowupRecordVO> getRecord(@PathVariable Long id) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(followupRecordService.getForDoctor(doctor.getId(), id));
    }

    // ---------------- 按居民维度 ----------------

    @GetMapping("/residents/{residentId}/followup-plans")
    public Result<List<FollowupPlanVO>> listResidentPlans(@PathVariable Long residentId) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(followupPlanService.listByResidentForDoctor(doctor.getId(), residentId));
    }

    @GetMapping("/residents/{residentId}/followup-records")
    public Result<IPage<FollowupRecordVO>> listResidentRecords(
            @PathVariable Long residentId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(followupRecordService.listByResidentForDoctor(doctor.getId(), residentId, page, size));
    }
}
