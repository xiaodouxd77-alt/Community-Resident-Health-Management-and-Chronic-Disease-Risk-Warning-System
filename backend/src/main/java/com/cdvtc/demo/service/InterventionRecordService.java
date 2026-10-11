package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.dto.InterventionFeedbackRequest;
import com.cdvtc.demo.dto.InterventionRecordCreateRequest;
import com.cdvtc.demo.entity.HealthProfile;
import com.cdvtc.demo.entity.InterventionPlan;
import com.cdvtc.demo.entity.InterventionRecord;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.enums.ExecutionStatus;
import com.cdvtc.demo.enums.InterventionStatus;
import com.cdvtc.demo.enums.OperatorRole;
import com.cdvtc.demo.mapper.InterventionRecordMapper;
import com.cdvtc.demo.mapper.UserMapper;
import com.cdvtc.demo.vo.InterventionRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 干预执行记录。医生执行记录与居民反馈同表不同行：
 * 医生写入 DOCTOR 行，居民只能新增 RESIDENT 行，双方互不覆盖，历史追加保留。
 */
@Service
@RequiredArgsConstructor
public class InterventionRecordService extends ServiceImpl<InterventionRecordMapper, InterventionRecord> {

    private final InterventionPlanService interventionPlanService;
    private final HealthProfileService healthProfileService;
    private final UserMapper userMapper;

    // ============================ 医生端 ============================

    @Transactional
    public InterventionRecordVO createRecord(Long doctorId, Long planId, InterventionRecordCreateRequest request) {
        InterventionPlan plan = interventionPlanService.requirePlan(planId);
        interventionPlanService.assertDoctorScope(doctorId, plan.getResidentId());

        InterventionStatus status = interventionPlanService.currentStatus(plan);
        if (status.isTerminal()) {
            throw new BusinessException(409, "方案已" + status.getLabel() + "，无法继续记录执行情况");
        }
        if (status == InterventionStatus.DRAFT) {
            throw new BusinessException(409, "草稿方案需先启动执行后再记录");
        }
        if (request.getExecutedAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(400, "实际执行时间不能晚于当前时间");
        }
        ExecutionStatus executionStatus = null;
        if (StringUtils.hasText(request.getExecutionStatus())) {
            executionStatus = ExecutionStatus.fromCode(request.getExecutionStatus())
                    .orElseThrow(() -> new BusinessException(400, "执行情况取值不正确"));
        }

        InterventionRecord record = new InterventionRecord();
        record.setInterventionPlanId(plan.getId());
        record.setResidentId(plan.getResidentId());
        record.setOperatorId(doctorId);
        record.setOperatorRole(OperatorRole.DOCTOR.name());
        record.setExecutedAt(request.getExecutedAt());
        record.setExecutionContent(trimOrNull(request.getExecutionContent()));
        record.setResidentFeedback(trimOrNull(request.getResidentFeedback()));
        record.setExecutionStatus(executionStatus == null ? null : executionStatus.name());
        record.setEffectSummary(trimOrNull(request.getEffectSummary()));
        record.setNextAction(trimOrNull(request.getNextAction()));
        record.setResidentVisible(toFlag(request.getResidentVisible(), true));
        record.setRemark(trimOrNull(request.getRemark()));
        save(record);

        return toVO(record, plan, healthProfileService.getById(plan.getResidentId()), userMapper.selectById(doctorId));
    }

    public IPage<InterventionRecordVO> pageForPlan(Long doctorId, Long planId, Integer page, Integer size) {
        InterventionPlan plan = interventionPlanService.requirePlan(planId);
        interventionPlanService.assertDoctorScope(doctorId, plan.getResidentId());
        int p = normalizePage(page);
        int s = normalizeSize(size);
        IPage<InterventionRecord> recordPage = page(new Page<>(p, s),
                new LambdaQueryWrapper<InterventionRecord>()
                        .eq(InterventionRecord::getInterventionPlanId, planId)
                        .orderByDesc(InterventionRecord::getExecutedAt)
                        .orderByDesc(InterventionRecord::getId));
        Page<InterventionRecordVO> voPage = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        voPage.setRecords(assemble(recordPage.getRecords()));
        return voPage;
    }

    public InterventionRecordVO getForDoctor(Long doctorId, Long recordId) {
        InterventionRecord record = requireRecord(recordId);
        interventionPlanService.assertDoctorScope(doctorId, record.getResidentId());
        return assemble(List.of(record)).get(0);
    }

    public IPage<InterventionRecordVO> listByResidentForDoctor(Long doctorId, Long residentId, Integer page, Integer size) {
        HealthProfile profile = healthProfileService.getByIdRequired(residentId);
        interventionPlanService.assertDoctorScope(doctorId, profile.getId());
        int p = normalizePage(page);
        int s = normalizeSize(size);
        IPage<InterventionRecord> recordPage = page(new Page<>(p, s),
                new LambdaQueryWrapper<InterventionRecord>()
                        .eq(InterventionRecord::getResidentId, profile.getId())
                        .orderByDesc(InterventionRecord::getExecutedAt)
                        .orderByDesc(InterventionRecord::getId));
        Page<InterventionRecordVO> voPage = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        voPage.setRecords(assemble(recordPage.getRecords()));
        return voPage;
    }

    // ============================ 居民端 ============================

    /** 居民对本人可见方案提交执行反馈（新增 RESIDENT 行，不触碰医生记录）。 */
    @Transactional
    public InterventionRecordVO addFeedback(Long residentId, Long operatorUserId, Long planId,
                                            InterventionFeedbackRequest request) {
        InterventionPlan plan = interventionPlanService.requirePlan(planId);
        if (!Objects.equals(plan.getResidentId(), residentId)
                || !Integer.valueOf(1).equals(plan.getResidentVisible())) {
            throw new BusinessException(404, "干预方案不存在");
        }
        InterventionStatus status = interventionPlanService.currentStatus(plan);
        if (!status.isExecutable()) {
            throw new BusinessException(409, "方案当前状态（" + status.getLabel() + "）不可提交反馈");
        }
        LocalDateTime executedAt = request.getExecutedAt() == null ? LocalDateTime.now() : request.getExecutedAt();
        if (executedAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException(400, "反馈时间不能晚于当前时间");
        }
        ExecutionStatus executionStatus = null;
        if (StringUtils.hasText(request.getExecutionStatus())) {
            executionStatus = ExecutionStatus.fromCode(request.getExecutionStatus())
                    .orElseThrow(() -> new BusinessException(400, "执行情况取值不正确"));
        }

        InterventionRecord record = new InterventionRecord();
        record.setInterventionPlanId(plan.getId());
        record.setResidentId(plan.getResidentId());
        record.setOperatorId(operatorUserId);
        record.setOperatorRole(OperatorRole.RESIDENT.name());
        record.setExecutedAt(executedAt);
        record.setResidentFeedback(trimOrNull(request.getResidentFeedback()));
        record.setExecutionStatus(executionStatus == null ? null : executionStatus.name());
        // 居民反馈默认对其可见
        record.setResidentVisible(1);
        save(record);

        return toVO(record, plan, healthProfileService.getById(plan.getResidentId()),
                userMapper.selectById(operatorUserId));
    }

    /** 居民查看本人方案的公开执行记录（resident_visible=1），不展示医生内部备注。 */
    public IPage<InterventionRecordVO> listForResident(Long residentId, Long planId, Integer page, Integer size) {
        InterventionPlan plan = interventionPlanService.requirePlan(planId);
        if (!Objects.equals(plan.getResidentId(), residentId)
                || !Integer.valueOf(1).equals(plan.getResidentVisible())) {
            throw new BusinessException(404, "干预方案不存在");
        }
        int p = normalizePage(page);
        int s = normalizeSize(size);
        IPage<InterventionRecord> recordPage = page(new Page<>(p, s),
                new LambdaQueryWrapper<InterventionRecord>()
                        .eq(InterventionRecord::getInterventionPlanId, planId)
                        .eq(InterventionRecord::getResidentVisible, 1)
                        .orderByDesc(InterventionRecord::getExecutedAt)
                        .orderByDesc(InterventionRecord::getId));
        Page<InterventionRecordVO> voPage = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        List<InterventionRecordVO> vos = assemble(recordPage.getRecords());
        vos.forEach(v -> v.setRemark(null));
        voPage.setRecords(vos);
        return voPage;
    }

    // ============================ 内部 ============================

    private InterventionRecord requireRecord(Long id) {
        InterventionRecord record = getById(id);
        if (record == null) {
            throw new BusinessException(404, "执行记录不存在");
        }
        return record;
    }

    private List<InterventionRecordVO> assemble(List<InterventionRecord> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        Set<Long> residentIds = records.stream().map(InterventionRecord::getResidentId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> operatorIds = records.stream().map(InterventionRecord::getOperatorId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> planIds = records.stream().map(InterventionRecord::getInterventionPlanId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, HealthProfile> profiles = healthProfileService.listByIds(residentIds).stream()
                .collect(Collectors.toMap(HealthProfile::getId, Function.identity(), (a, b) -> a));
        Map<Long, User> operators = operatorIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(operatorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        Map<Long, InterventionPlan> plans = planIds.isEmpty() ? Map.of()
                : interventionPlanService.listByIds(planIds).stream()
                .collect(Collectors.toMap(InterventionPlan::getId, Function.identity(), (a, b) -> a));

        return records.stream().map(r -> {
            InterventionRecordVO vo = new InterventionRecordVO();
            vo.setId(r.getId());
            vo.setInterventionPlanId(r.getInterventionPlanId());
            InterventionPlan plan = plans.get(r.getInterventionPlanId());
            if (plan != null) {
                vo.setPlanName(plan.getPlanName());
            }
            vo.setResidentId(r.getResidentId());
            HealthProfile profile = profiles.get(r.getResidentId());
            if (profile != null) {
                vo.setResidentNo(profile.getResidentNo());
                vo.setResidentName(StringUtils.hasText(profile.getRealName())
                        ? profile.getRealName() : profile.getResidentNo());
            }
            vo.setOperatorId(r.getOperatorId());
            User operator = operators.get(r.getOperatorId());
            vo.setOperatorName(operator == null ? null : operator.displayName());
            vo.setOperatorRole(r.getOperatorRole());
            vo.setOperatorRoleLabel(OperatorRole.labelOf(r.getOperatorRole()));
            vo.setExecutedAt(r.getExecutedAt());
            vo.setExecutionContent(r.getExecutionContent());
            vo.setResidentFeedback(r.getResidentFeedback());
            vo.setExecutionStatus(r.getExecutionStatus());
            vo.setExecutionStatusLabel(ExecutionStatus.labelOf(r.getExecutionStatus()));
            vo.setEffectSummary(r.getEffectSummary());
            vo.setNextAction(r.getNextAction());
            vo.setResidentVisible(toBool(r.getResidentVisible()));
            vo.setRemark(r.getRemark());
            vo.setCreateTime(r.getCreateTime());
            return vo;
        }).toList();
    }

    private InterventionRecordVO toVO(InterventionRecord r, InterventionPlan plan,
                                      HealthProfile profile, User operator) {
        InterventionRecordVO vo = new InterventionRecordVO();
        vo.setId(r.getId());
        vo.setInterventionPlanId(r.getInterventionPlanId());
        if (plan != null) {
            vo.setPlanName(plan.getPlanName());
        }
        vo.setResidentId(r.getResidentId());
        if (profile != null) {
            vo.setResidentNo(profile.getResidentNo());
            vo.setResidentName(StringUtils.hasText(profile.getRealName())
                    ? profile.getRealName() : profile.getResidentNo());
        }
        vo.setOperatorId(r.getOperatorId());
        vo.setOperatorName(operator == null ? null : operator.displayName());
        vo.setOperatorRole(r.getOperatorRole());
        vo.setOperatorRoleLabel(OperatorRole.labelOf(r.getOperatorRole()));
        vo.setExecutedAt(r.getExecutedAt());
        vo.setExecutionContent(r.getExecutionContent());
        vo.setResidentFeedback(r.getResidentFeedback());
        vo.setExecutionStatus(r.getExecutionStatus());
        vo.setExecutionStatusLabel(ExecutionStatus.labelOf(r.getExecutionStatus()));
        vo.setEffectSummary(r.getEffectSummary());
        vo.setNextAction(r.getNextAction());
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
