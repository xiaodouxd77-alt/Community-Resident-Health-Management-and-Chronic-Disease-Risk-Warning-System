package com.cdvtc.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.dto.ResidentGroupCreateRequest;
import com.cdvtc.demo.dto.ResidentGroupMemberAddRequest;
import com.cdvtc.demo.dto.ResidentGroupQueryRequest;
import com.cdvtc.demo.dto.ResidentGroupUpdateRequest;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.service.CurrentUserProvider;
import com.cdvtc.demo.service.ResidentGroupService;
import com.cdvtc.demo.vo.ResidentGroupMemberVO;
import com.cdvtc.demo.vo.ResidentGroupVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 居民分组管理接口。医生只能查看和修改本人负责或创建的分组。
 */
@RestController
@RequestMapping("/api/doctor/resident-groups")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorResidentGroupController {

    private final CurrentUserProvider currentUserProvider;
    private final ResidentGroupService residentGroupService;

    @GetMapping
    public Result<IPage<ResidentGroupVO>> list(ResidentGroupQueryRequest query) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(residentGroupService.pageForDoctor(doctor.getId(), query));
    }

    @PostMapping
    public Result<ResidentGroupVO> create(@Valid @RequestBody ResidentGroupCreateRequest request) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(residentGroupService.create(doctor.getId(), request));
    }

    @GetMapping("/{groupId}")
    public Result<ResidentGroupVO> get(@PathVariable Long groupId) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(residentGroupService.getForDoctor(doctor.getId(), groupId));
    }

    @PutMapping("/{groupId}")
    public Result<ResidentGroupVO> update(@PathVariable Long groupId,
                                          @Valid @RequestBody ResidentGroupUpdateRequest request) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(residentGroupService.update(doctor.getId(), groupId, request));
    }

    @GetMapping("/{groupId}/members")
    public Result<IPage<ResidentGroupMemberVO>> members(
            @PathVariable Long groupId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(residentGroupService.pageMembers(doctor.getId(), groupId, keyword, page, size));
    }

    @PostMapping("/{groupId}/members")
    public Result<Map<String, Object>> addMembers(@PathVariable Long groupId,
                                                  @Valid @RequestBody ResidentGroupMemberAddRequest request) {
        User doctor = currentUserProvider.requireCurrentUser();
        return Result.success(residentGroupService.addMembers(doctor.getId(), groupId, request));
    }

    @DeleteMapping("/{groupId}/members/{residentId}")
    public Result<Void> removeMember(@PathVariable Long groupId, @PathVariable Long residentId) {
        User doctor = currentUserProvider.requireCurrentUser();
        residentGroupService.removeMember(doctor.getId(), groupId, residentId);
        return Result.success();
    }
}
