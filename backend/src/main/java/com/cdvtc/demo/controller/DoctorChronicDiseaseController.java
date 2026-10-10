package com.cdvtc.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.dto.*;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.service.ChronicDiseaseService;
import com.cdvtc.demo.service.CurrentUserProvider;
import com.cdvtc.demo.vo.ChronicDiseaseVO;
import com.cdvtc.demo.vo.ChronicHistoryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 慢病患者建档与管理接口。所有操作都会二次校验居民是否在医生授权范围内。
 */
@RestController
@RequestMapping("/api/doctor/chronic-diseases")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorChronicDiseaseController {

    private final CurrentUserProvider currentUserProvider;
    private final ChronicDiseaseService chronicDiseaseService;

    @GetMapping
    public Result<IPage<ChronicDiseaseVO>> list(ChronicDiseaseQueryRequest query) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(chronicDiseaseService.pageForDoctor(doctor.getId(), query));
    }

    @PostMapping
    public Result<ChronicDiseaseVO> create(@Valid @RequestBody ChronicDiseaseCreateRequest request) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(chronicDiseaseService.create(doctor.getId(), request));
    }

    @GetMapping("/{id}")
    public Result<ChronicDiseaseVO> get(@PathVariable Long id) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(chronicDiseaseService.getForDoctor(doctor.getId(), id));
    }

    @PutMapping("/{id}")
    public Result<ChronicDiseaseVO> update(@PathVariable Long id,
                                           @Valid @RequestBody ChronicDiseaseUpdateRequest request) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(chronicDiseaseService.update(doctor.getId(), id, request));
    }

    @PostMapping("/{id}/review")
    public Result<ChronicDiseaseVO> review(@PathVariable Long id,
                                           @Valid @RequestBody ChronicDiseaseReviewRequest request) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(chronicDiseaseService.review(doctor.getId(), id, request));
    }

    @PostMapping("/{id}/status")
    public Result<ChronicDiseaseVO> changeStatus(@PathVariable Long id,
                                                 @Valid @RequestBody ChronicDiseaseStatusRequest request) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(chronicDiseaseService.changeStatus(doctor.getId(), id, request));
    }

    @GetMapping("/{id}/history")
    public Result<List<ChronicHistoryVO>> history(@PathVariable Long id) {
        User doctor = currentUserProvider.requireCurrentUser();
        // 先做范围校验，再返回变更历史
        chronicDiseaseService.getForDoctor(doctor.getId(), id);
        return Result.success(chronicDiseaseService.history(id));
    }
}
