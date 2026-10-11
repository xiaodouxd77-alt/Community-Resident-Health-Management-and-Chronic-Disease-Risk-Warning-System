package com.cdvtc.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.dto.ConsultationCreateRequest;
import com.cdvtc.demo.dto.ConsultationMessageRequest;
import com.cdvtc.demo.dto.ConsultationQueryRequest;
import com.cdvtc.demo.entity.HealthProfile;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.service.ConsultationService;
import com.cdvtc.demo.service.CurrentUserProvider;
import com.cdvtc.demo.service.HealthProfileService;
import com.cdvtc.demo.vo.ConsultationDetailVO;
import com.cdvtc.demo.vo.ConsultationMessageVO;
import com.cdvtc.demo.vo.ConsultationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 居民端在线咨询。数据归属完全由登录身份解析，居民只能访问本人咨询。
 */
@RestController
@RequestMapping("/api/resident")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESIDENT')")
public class ResidentConsultationController {

    private final CurrentUserProvider currentUserProvider;
    private final HealthProfileService healthProfileService;
    private final ConsultationService consultationService;

    @PostMapping("/consultations")
    public Result<ConsultationVO> create(@Valid @RequestBody ConsultationCreateRequest request) {
        return Result.success(consultationService.create(requireProfile().getId(), request));
    }

    @GetMapping("/consultations")
    public Result<IPage<ConsultationVO>> list(ConsultationQueryRequest query) {
        return Result.success(consultationService.pageForResident(requireProfile().getId(), query));
    }

    @GetMapping("/consultations/{id}")
    public Result<ConsultationDetailVO> detail(@PathVariable Long id) {
        return Result.success(consultationService.getForResident(requireProfile().getId(), id));
    }

    @GetMapping("/consultations/{id}/messages")
    public Result<List<ConsultationMessageVO>> messages(@PathVariable Long id) {
        return Result.success(consultationService.messagesForResident(requireProfile().getId(), id));
    }

    @PostMapping("/consultations/{id}/messages")
    public Result<ConsultationMessageVO> sendMessage(@PathVariable Long id,
                                                     @Valid @RequestBody ConsultationMessageRequest request) {
        User user = currentUserProvider.requireCurrentUser();
        return Result.success(consultationService.addResidentMessage(
                requireProfile().getId(), user.getId(), id, request));
    }

    @PostMapping("/consultations/{id}/cancel")
    public Result<ConsultationVO> cancel(@PathVariable Long id) {
        return Result.success(consultationService.cancel(requireProfile().getId(), id));
    }

    private HealthProfile requireProfile() {
        User user = currentUserProvider.requireCurrentUser();
        HealthProfile profile = healthProfileService.getByUserId(user.getId());
        if (profile == null) {
            throw new BusinessException(404, "尚未建立健康档案");
        }
        return profile;
    }
}
