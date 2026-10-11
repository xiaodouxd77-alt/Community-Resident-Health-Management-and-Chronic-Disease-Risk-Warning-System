package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.dto.FollowupRecordCreateRequest;
import com.cdvtc.demo.entity.FollowupPlan;
import com.cdvtc.demo.entity.FollowupRecord;
import com.cdvtc.demo.entity.HealthProfile;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.enums.FollowupMethod;
import com.cdvtc.demo.enums.PlanStatus;
import com.cdvtc.demo.mapper.FollowupRecordMapper;
import com.cdvtc.demo.mapper.UserMapper;
import com.cdvtc.demo.vo.FollowupCompleteVO;
import com.cdvtc.demo.vo.FollowupPlanVO;
import com.cdvtc.demo.vo.FollowupRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 随访记录。写入记录与更新计划状态在同一事务内完成，
 * 同一计划保留多条历史记录，新增记录不覆盖历史。
 */
@Service
@RequiredArgsConstructor
public class FollowupRecordService extends ServiceImpl<FollowupRecordMapper, FollowupRecord> {

    private final FollowupPlanService followupPlanService;
    private final HealthProfileService healthProfileService;
    private final UserMapper userMapper;

    // ============================ 医生端 ============================

    /**
     * 填写随访记录（事务）：写入记录 → 计划由「待执行」置「进行中」 →
     * 可选同时完成计划 → 可选按建议时间生成下一次计划。
     */
    @Transactional
    public FollowupCompleteVO createRecord(Long doctorId, Long planId, FollowupRecordCreateRequest request) {
        FollowupPlan plan = followupPlanService.requirePlan(planId);
        followupPlanService.assertDoctorScope(doctorId, plan.getResidentId());

        PlanStatus current = followupPlanService.currentStatus(plan);
        if (current.isTerminal()) {
            throw new BusinessException(409, "计划已" + current.getLabel() + "，无法继续填写随访记录");
        }

        FollowupMethod method = FollowupMethod.fromCode(request.getFollowupMethod())
                .orElseThrow(() -> new BusinessException(400, "随访方式取值不正确"));
        if (request.getFollowedUpAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(400, "实际随访时间不能晚于当前时间");
        }
        if (request.getNextFollowupAt() != null
                && !request.getNextFollowupAt().isAfter(request.getFollowedUpAt())) {
            throw new BusinessException(400, "建议下次随访时间需晚于本次随访时间");
        }

        FollowupRecord record = new FollowupRecord();
        record.setPlanId(plan.getId());
        record.setResidentId(plan.getResidentId());
        record.setDoctorId(doctorId);
        record.setFollowedUpAt(request.getFollowedUpAt());
        record.setFollowupMethod(method.name());
        record.setResidentFeedback(trimOrNull(request.getResidentFeedback()));
        record.setHealthSummary(trimOrNull(request.getHealthSummary()));
        record.setIndicatorSummary(trimOrNull(request.getIndicatorSummary()));
        record.setInterventionAdvice(trimOrNull(request.getInterventionAdvice()));
        record.setFollowupResult(trimOrNull(request.getFollowupResult()));
        record.setNextFollowupAt(request.getNextFollowupAt());
        record.setResidentVisible(toFlag(request.getResidentVisible(), true));
        record.setRemark(trimOrNull(request.getRemark()));
        save(record);

        // 首次填写记录：计划进入进行中
        if (current == PlanStatus.PLANNED) {
            plan.setPlanStatus(PlanStatus.IN_PROGRESS.name());
            followupPlanService.updateById(plan);
        }
        // 可选：同时完成本次计划
        if (Boolean.TRUE.equals(request.getComplete()) && !PlanStatus.COMPLETED.name().equals(plan.getPlanStatus())) {
            plan.setPlanStatus(PlanStatus.COMPLETED.name());
            followupPlanService.updateById(plan);
        }

        // 可选：按建议时间生成下一次计划
        FollowupPlanVO nextPlan = null;
        if (Boolean.TRUE.equals(request.getCreateNextPlan()) && request.getNextFollowupAt() != null) {
            nextPlan = createNextPlan(doctorId, plan, record);
        }

        FollowupCompleteVO vo = new FollowupCompleteVO();
        vo.setRecord(toVO(record, plan, healthProfileService.getById(plan.getResidentId()),
                userMapper.selectById(doctorId)));
        vo.setPlan(followupPlanService.getForDoctor(doctorId, plan.getId()));
        vo.setNextPlan(nextPlan);
        return vo;
    }

    private FollowupPlanVO createNextPlan(Long doctorId, FollowupPlan plan, FollowupRecord record) {
        HealthProfile profile = healthProfileService.getById(plan.getResidentId());
        String residentName = profile != null && StringUtils.hasText(profile.getRealName())
                ? profile.getRealName() : (profile == null ? "" : profile.getResidentNo());

        FollowupPlan next = new FollowupPlan();
        next.setResidentId(plan.getResidentId());
        next.setDoctorId(doctorId);
        next.setChronicDiseaseRecordId(plan.getChronicDiseaseRecordId());
        next.setAlertId(plan.getAlertId());
        next.setPlanTitle("后续随访：" + residentName);
        next.setFollowupPurpose(plan.getFollowupPurpose());
        next.setScheduledAt(record.getNextFollowupAt());
        next.setFollowupMethod(record.getFollowupMethod());
        next.setPlanStatus(PlanStatus.PLANNED.name());
        next.setResidentVisible(record.getResidentVisible());
        followupPlanService.save(next);
        return followupPlanService.getForDoctor(doctorId, next.getId());
    }

    public IPage<FollowupRecordVO> pageForPlan(Long doctorId, Long planId, Integer page, Integer size) {
        FollowupPlan plan = followupPlanService.requirePlan(planId);
        followupPlanService.assertDoctorScope(doctorId, plan.getResidentId());
        int p = normalizePage(page);
        int s = normalizeSize(size);
        IPage<FollowupRecord> recordPage = page(new Page<>(p, s),
                new LambdaQueryWrapper<FollowupRecord>()
                        .eq(FollowupRecord::getPlanId, planId)
                        .orderByDesc(FollowupRecord::getFollowedUpAt)
                        .orderByDesc(FollowupRecord::getId));
        Page<FollowupRecordVO> voPage = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        voPage.setRecords(assemble(recordPage.getRecords()));
        return voPage;
    }

    public FollowupRecordVO getForDoctor(Long doctorId, Long recordId) {
        FollowupRecord record = requireRecord(recordId);
        followupPlanService.assertDoctorScope(doctorId, record.getResidentId());
        return assemble(List.of(record)).get(0);
    }

    public IPage<FollowupRecordVO> listByResidentForDoctor(Long doctorId, Long residentId, Integer page, Integer size) {
        HealthProfile profile = healthProfileService.getByIdRequired(residentId);
        followupPlanService.assertDoctorScope(doctorId, profile.getId());
        int p = normalizePage(page);
        int s = normalizeSize(size);
        IPage<FollowupRecord> recordPage = page(new Page<>(p, s),
                new LambdaQueryWrapper<FollowupRecord>()
                        .eq(FollowupRecord::getResidentId, profile.getId())
                        .orderByDesc(FollowupRecord::getFollowedUpAt)
                        .orderByDesc(FollowupRecord::getId));
        Page<FollowupRecordVO> voPage = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        voPage.setRecords(assemble(recordPage.getRecords()));
        return voPage;
    }

    /** 完成随访计划（不新增记录）。已完成/已取消的计划重复提交会被拒绝。 */
    @Transactional
    public FollowupPlanVO completePlan(Long doctorId, Long planId) {
        FollowupPlan plan = followupPlanService.requirePlan(planId);
        followupPlanService.assertDoctorScope(doctorId, plan.getResidentId());
        PlanStatus current = followupPlanService.currentStatus(plan);
        if (current == PlanStatus.COMPLETED) {
            throw new BusinessException(409, "该计划已完成，请勿重复提交");
        }
        if (current == PlanStatus.CANCELLED) {
            throw new BusinessException(409, "已取消的计划不可完成");
        }
        plan.setPlanStatus(PlanStatus.COMPLETED.name());
        followupPlanService.updateById(plan);
        return followupPlanService.getForDoctor(doctorId, planId);
    }

    // ============================ 居民端 ============================

    /** 居民本人允许公开的随访记录，按随访时间倒序。 */
    public IPage<FollowupRecordVO> listForResident(Long residentId, Integer page, Integer size) {
        int p = normalizePage(page);
        int s = normalizeSize(size);
        IPage<FollowupRecord> recordPage = page(new Page<>(p, s),
                new LambdaQueryWrapper<FollowupRecord>()
                        .eq(FollowupRecord::getResidentId, residentId)
                        .eq(FollowupRecord::getResidentVisible, 1)
                        .orderByDesc(FollowupRecord::getFollowedUpAt)
                        .orderByDesc(FollowupRecord::getId));
        Page<FollowupRecordVO> voPage = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        List<FollowupRecordVO> vos = assemble(recordPage.getRecords());
        // 居民端不展示医生内部备注
        vos.forEach(v -> v.setRemark(null));
        voPage.setRecords(vos);
        return voPage;
    }

    public FollowupRecordVO getForResident(Long residentId, Long recordId) {
        FollowupRecord record = getById(recordId);
        if (record == null || !Objects.equals(record.getResidentId(), residentId)
                || !Integer.valueOf(1).equals(record.getResidentVisible())) {
            // 不存在或不属于本人或不允许公开，统一按不存在处理，避免泄露
            throw new BusinessException(404, "随访记录不存在");
        }
        FollowupRecordVO vo = assemble(List.of(record)).get(0);
        vo.setRemark(null);
        return vo;
    }

    // ============================ 内部 ============================

    private FollowupRecord requireRecord(Long id) {
        FollowupRecord record = getById(id);
        if (record == null) {
            throw new BusinessException(404, "随访记录不存在");
        }
        return record;
    }

    private List<FollowupRecordVO> assemble(List<FollowupRecord> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        Set<Long> residentIds = records.stream().map(FollowupRecord::getResidentId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> doctorIds = records.stream().map(FollowupRecord::getDoctorId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> planIds = records.stream().map(FollowupRecord::getPlanId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, HealthProfile> profiles = healthProfileService.listByIds(residentIds).stream()
                .collect(Collectors.toMap(HealthProfile::getId, Function.identity(), (a, b) -> a));
        Map<Long, User> doctors = doctorIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(doctorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        Map<Long, FollowupPlan> plans = planIds.isEmpty() ? Map.of()
                : followupPlanService.listByIds(planIds).stream()
                .collect(Collectors.toMap(FollowupPlan::getId, Function.identity(), (a, b) -> a));

        return records.stream().map(r -> {
            FollowupRecordVO vo = new FollowupRecordVO();
            vo.setId(r.getId());
            vo.setPlanId(r.getPlanId());
            FollowupPlan plan = plans.get(r.getPlanId());
            if (plan != null) {
                vo.setPlanTitle(plan.getPlanTitle());
            }
            vo.setResidentId(r.getResidentId());
            HealthProfile profile = profiles.get(r.getResidentId());
            if (profile != null) {
                vo.setResidentNo(profile.getResidentNo());
                vo.setResidentName(StringUtils.hasText(profile.getRealName())
                        ? profile.getRealName() : profile.getResidentNo());
            }
            vo.setDoctorId(r.getDoctorId());
            User doctor = doctors.get(r.getDoctorId());
            vo.setDoctorName(doctor == null ? null : doctor.displayName());
            vo.setFollowedUpAt(r.getFollowedUpAt());
            vo.setFollowupMethod(r.getFollowupMethod());
            vo.setFollowupMethodLabel(FollowupMethod.labelOf(r.getFollowupMethod()));
            vo.setResidentFeedback(r.getResidentFeedback());
            vo.setHealthSummary(r.getHealthSummary());
            vo.setIndicatorSummary(r.getIndicatorSummary());
            vo.setInterventionAdvice(r.getInterventionAdvice());
            vo.setFollowupResult(r.getFollowupResult());
            vo.setNextFollowupAt(r.getNextFollowupAt());
            vo.setResidentVisible(toBool(r.getResidentVisible()));
            vo.setRemark(r.getRemark());
            vo.setCreateTime(r.getCreateTime());
            return vo;
        }).toList();
    }

    private FollowupRecordVO toVO(FollowupRecord r, FollowupPlan plan, HealthProfile profile, User doctor) {
        FollowupRecordVO vo = new FollowupRecordVO();
        vo.setId(r.getId());
        vo.setPlanId(r.getPlanId());
        if (plan != null) {
            vo.setPlanTitle(plan.getPlanTitle());
        }
        vo.setResidentId(r.getResidentId());
        if (profile != null) {
            vo.setResidentNo(profile.getResidentNo());
            vo.setResidentName(StringUtils.hasText(profile.getRealName())
                    ? profile.getRealName() : profile.getResidentNo());
        }
        vo.setDoctorId(r.getDoctorId());
        vo.setDoctorName(doctor == null ? null : doctor.displayName());
        vo.setFollowedUpAt(r.getFollowedUpAt());
        vo.setFollowupMethod(r.getFollowupMethod());
        vo.setFollowupMethodLabel(FollowupMethod.labelOf(r.getFollowupMethod()));
        vo.setResidentFeedback(r.getResidentFeedback());
        vo.setHealthSummary(r.getHealthSummary());
        vo.setIndicatorSummary(r.getIndicatorSummary());
        vo.setInterventionAdvice(r.getInterventionAdvice());
        vo.setFollowupResult(r.getFollowupResult());
        vo.setNextFollowupAt(r.getNextFollowupAt());
        vo.setResidentVisible(toBool(r.getResidentVisible()));
        vo.setRemark(r.getRemark());
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }

    private Integer toFlag(Boolean value, boolean defaultValue) {
        boolean v = value == null ? defaultValue : value;
        return v ? 1 : 0;
    }

    private Boolean toBool(Integer value) {
        return value == null || value != 0;
    }

    private String trimOrNull(String value) {
        String v = value == null ? null : value.trim();
        return StringUtils.hasText(v) ? v : null;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        int s = size == null ? 10 : size;
        return s < 1 || s > 100 ? 10 : s;
    }
}
