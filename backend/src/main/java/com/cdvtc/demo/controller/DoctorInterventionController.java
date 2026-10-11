package com.cdvtc.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.dto.InterventionPlanCreateRequest;
import com.cdvtc.demo.dto.InterventionPlanQueryRequest;
import com.cdvtc.demo.dto.InterventionPlanUpdateRequest;
import com.cdvtc.demo.dto.InterventionRecordCreateRequest;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.service.CurrentUserProvider;
import com.cdvtc.demo.service.InterventionPlanService;
import com.cdvtc.demo.service.InterventionRecordService;
import com.cdvtc.demo.vo.InterventionEffectVO;
import com.cdvtc.demo.vo.InterventionPlanVO;
import com.cdvtc.demo.vo.InterventionRecordVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 医生端健康干预接口。所有操作都会二次校验居民是否在医生的授权范围内。
 */
@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorInterventionController {

    private final CurrentUserProvider currentUserProvider;
    private final InterventionPlanService interventionPlanService;
    private final InterventionRecordService interventionRecordService;

    // ---------------- 干预方案 ----------------

    @PostMapping("/intervention-plans")
    public Result<InterventionPlanVO> createPlan(@Valid @RequestBody InterventionPlanCreateRequest request) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(interventionPlanService.create(doctor.getId(), request));
    }

    @GetMapping("/intervention-plans")
    public Result<IPage<InterventionPlanVO>> listPlans(InterventionPlanQueryRequest query) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(interventionPlanService.pageForDoctor(doctor.getId(), query));
    }

    /** 待处理 / 需要调整的方案。 */
    @GetMapping("/intervention-plans/todo")
    public Result<List<InterventionPlanVO>> todoPlans() {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(interventionPlanService.todo(doctor.getId()));
    }

    @GetMapping("/intervention-plans/{id}")
    public Result<InterventionPlanVO> getPlan(@PathVariable Long id) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(interventionPlanService.getForDoctor(doctor.getId(), id));
    }

    @PutMapping("/intervention-plans/{id}")
    public Result<InterventionPlanVO> updatePlan(@PathVariable Long id,
                                                 @Valid @RequestBody InterventionPlanUpdateRequest request) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(interventionPlanService.update(doctor.getId(), id, request));
    }

    @PostMapping("/intervention-plans/{id}/activate")
    public Result<InterventionPlanVO> activatePlan(@PathVariable Long id) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(interventionPlanService.activate(doctor.getId(), id));
    }

    @PostMapping("/intervention-plans/{id}/suspend")
    public Result<InterventionPlanVO> suspendPlan(@PathVariable Long id) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(interventionPlanService.suspend(doctor.getId(), id));
    }

    @PostMapping("/intervention-plans/{id}/complete")
    public Result<InterventionPlanVO> completePlan(@PathVariable Long id) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(interventionPlanService.complete(doctor.getId(), id));
    }

    @PostMapping("/intervention-plans/{id}/cancel")
    public Result<InterventionPlanVO> cancelPlan(@PathVariable Long id) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(interventionPlanService.cancel(doctor.getId(), id));
    }

    /** 方案阶段性效果。 */
    @GetMapping("/intervention-plans/{id}/effect")
    public Result<InterventionEffectVO> planEffect(@PathVariable Long id) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(interventionPlanService.effect(doctor.getId(), id));
    }

    // ---------------- 干预执行记录 ----------------

    @PostMapping("/intervention-plans/{id}/records")
    public Result<InterventionRecordVO> createRecord(@PathVariable Long id,
                                                     @Valid @RequestBody InterventionRecordCreateRequest request) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(interventionRecordService.createRecord(doctor.getId(), id, request));
    }

    @GetMapping("/intervention-plans/{id}/records")
    public Result<IPage<InterventionRecordVO>> listRecords(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(interventionRecordService.pageForPlan(doctor.getId(), id, page, size));
    }

    @GetMapping("/intervention-records/{id}")
    public Result<InterventionRecordVO> getRecord(@PathVariable Long id) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(interventionRecordService.getForDoctor(doctor.getId(), id));
    }

    // ---------------- 按居民维度 ----------------

    @GetMapping("/residents/{residentId}/intervention-plans")
    public Result<List<InterventionPlanVO>> listResidentPlans(@PathVariable Long residentId) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(interventionPlanService.listByResidentForDoctor(doctor.getId(), residentId));
    }

    @GetMapping("/residents/{residentId}/intervention-records")
    public Result<IPage<InterventionRecordVO>> listResidentRecords(
            @PathVariable Long residentId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(interventionRecordService.listByResidentForDoctor(doctor.getId(), residentId, page, size));
    }
}
