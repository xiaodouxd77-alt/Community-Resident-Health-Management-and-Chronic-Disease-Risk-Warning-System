package com.cdvtc.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.dto.ConsultationMessageRequest;
import com.cdvtc.demo.dto.ConsultationQueryRequest;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.service.ConsultationService;
import com.cdvtc.demo.service.CurrentUserProvider;
import com.cdvtc.demo.vo.ConsultationDetailVO;
import com.cdvtc.demo.vo.ConsultationMessageVO;
import com.cdvtc.demo.vo.ConsultationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 医生端在线咨询。仅能查看/处置授权范围内居民的咨询。
 */
@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorConsultationController {

    private final CurrentUserProvider currentUserProvider;
    private final ConsultationService consultationService;

    @GetMapping("/consultations")
    public Result<IPage<ConsultationVO>> list(ConsultationQueryRequest query) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(consultationService.pageForDoctor(doctor.getId(), query));
    }

    @GetMapping("/consultations/{id}")
    public Result<ConsultationDetailVO> detail(@PathVariable Long id) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(consultationService.getForDoctor(doctor.getId(), id));
    }

    @GetMapping("/consultations/{id}/messages")
    public Result<List<ConsultationMessageVO>> messages(@PathVariable Long id) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(consultationService.messagesForDoctor(doctor.getId(), id));
    }

    @PostMapping("/consultations/{id}/accept")
    public Result<ConsultationVO> accept(@PathVariable Long id) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(consultationService.accept(doctor.getId(), id));
    }

    @PostMapping("/consultations/{id}/messages")
    public Result<ConsultationMessageVO> reply(@PathVariable Long id,
                                               @Valid @RequestBody ConsultationMessageRequest request) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(consultationService.addDoctorMessage(doctor.getId(), id, request));
    }

    @PostMapping("/consultations/{id}/resolve")
    public Result<ConsultationVO> resolve(@PathVariable Long id) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(consultationService.resolve(doctor.getId(), id));
    }

    @PostMapping("/consultations/{id}/close")
    public Result<ConsultationVO> close(@PathVariable Long id) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(consultationService.close(doctor.getId(), id));
    }
}
