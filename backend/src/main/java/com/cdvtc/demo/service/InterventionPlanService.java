package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.dto.InterventionPlanCreateRequest;
import com.cdvtc.demo.dto.InterventionPlanQueryRequest;
import com.cdvtc.demo.dto.InterventionPlanUpdateRequest;
import com.cdvtc.demo.entity.*;
import com.cdvtc.demo.enums.*;
import com.cdvtc.demo.mapper.*;
import com.cdvtc.demo.vo.InterventionEffectVO;
import com.cdvtc.demo.vo.InterventionPlanVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 健康干预方案。所有操作以「当前医生管理范围内的居民」为边界；
 * 方案状态流转集中在本 Service 校验，并保留终态不可再变更的约束。
 */
@Service
@RequiredArgsConstructor
public class InterventionPlanService extends ServiceImpl<InterventionPlanMapper, InterventionPlan> {

    private final HealthProfileService healthProfileService;
    private final DoctorResidentService doctorResidentService;
    private final CommunityService communityService;
    private final ChronicDiseaseRecordMapper chronicDiseaseRecordMapper;
    private final AlertRecordMapper alertRecordMapper;
    private final FollowupPlanMapper followupPlanMapper;
    private final InterventionRecordMapper interventionRecordMapper;
    private final UserMapper userMapper;

    // ============================ 医生端 ============================

    @Transactional
    public InterventionPlanVO create(Long doctorId, InterventionPlanCreateRequest request) {
        HealthProfile profile = healthProfileService.getByIdRequired(request.getResidentId());
        assertDoctorScope(doctorId, profile.getId());

        InterventionType type = InterventionType.fromCode(request.getInterventionType())
                .orElseThrow(() -> new BusinessException(400, "干预类型取值不正确"));
        validateDates(request.getStartDate(), request.getEndDate());
        validateLinkage(profile.getId(), request.getChronicDiseaseRecordId(),
                request.getAlertId(), request.getFollowupPlanId());

        InterventionPlan plan = new InterventionPlan();
        plan.setResidentId(profile.getId());
        plan.setDoctorId(doctorId);
        plan.setChronicDiseaseRecordId(request.getChronicDiseaseRecordId());
        plan.setAlertId(request.getAlertId());
        plan.setFollowupPlanId(request.getFollowupPlanId());
        plan.setPlanName(request.getPlanName().trim());
        plan.setInterventionType(type.name());
        plan.setInterventionGoal(trimOrNull(request.getInterventionGoal()));
        plan.setInterventionContent(trimOrNull(request.getInterventionContent()));
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setPlanStatus(InterventionStatus.DRAFT.name());
        plan.setResidentVisible(toFlag(request.getResidentVisible(), true));
        plan.setRemark(trimOrNull(request.getRemark()));
        save(plan);

        return getForDoctor(doctorId, plan.getId());
    }

    public IPage<InterventionPlanVO> pageForDoctor(Long doctorId, InterventionPlanQueryRequest query) {
        int page = normalizePage(query.getPage());
        int size = normalizeSize(query.getSize());
        List<Long> scoped = doctorResidentService.listResidentIdsOfDoctor(doctorId);
        if (scoped.isEmpty()) {
            return new Page<>(page, size, 0);
        }

        LambdaQueryWrapper<InterventionPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(InterventionPlan::getResidentId, scoped);
        applyFilters(wrapper, query, scoped);
        wrapper.orderByDesc(InterventionPlan::getCreateTime).orderByDesc(InterventionPlan::getId);

        IPage<InterventionPlan> planPage = page(new Page<>(page, size), wrapper);
        Page<InterventionPlanVO> voPage = new Page<>(planPage.getCurrent(), planPage.getSize(), planPage.getTotal());
        voPage.setRecords(assemble(planPage.getRecords()));
        return voPage;
    }

    public InterventionPlanVO getForDoctor(Long doctorId, Long id) {
        InterventionPlan plan = requirePlan(id);
        assertDoctorScope(doctorId, plan.getResidentId());
        return assemble(List.of(plan)).get(0);
    }

    @Transactional
    public InterventionPlanVO update(Long doctorId, Long id, InterventionPlanUpdateRequest request) {
        InterventionPlan plan = requirePlan(id);
        assertDoctorScope(doctorId, plan.getResidentId());
        InterventionStatus current = currentStatus(plan);
        if (current.isTerminal()) {
            throw new BusinessException(409, "已" + current.getLabel() + "的方案不可修改");
        }

        LocalDate start = request.getStartDate() != null ? request.getStartDate() : plan.getStartDate();
        LocalDate end = request.getEndDate() != null ? request.getEndDate() : plan.getEndDate();
        validateDates(start, end);

        if (request.getPlanName() != null) {
            if (!StringUtils.hasText(request.getPlanName())) {
                throw new BusinessException(400, "方案名称不能为空");
            }
            plan.setPlanName(request.getPlanName().trim());
        }
        if (request.getInterventionType() != null) {
            plan.setInterventionType(InterventionType.fromCode(request.getInterventionType())
                    .orElseThrow(() -> new BusinessException(400, "干预类型取值不正确")).name());
        }
        Long chronicId = request.getChronicDiseaseRecordId() != null
                ? request.getChronicDiseaseRecordId() : plan.getChronicDiseaseRecordId();
        Long alertId = request.getAlertId() != null ? request.getAlertId() : plan.getAlertId();
        Long followupId = request.getFollowupPlanId() != null
                ? request.getFollowupPlanId() : plan.getFollowupPlanId();
        validateLinkage(plan.getResidentId(), chronicId, alertId, followupId);

        if (request.getChronicDiseaseRecordId() != null) {
            plan.setChronicDiseaseRecordId(request.getChronicDiseaseRecordId());
        }
        if (request.getAlertId() != null) {
            plan.setAlertId(request.getAlertId());
        }
        if (request.getFollowupPlanId() != null) {
            plan.setFollowupPlanId(request.getFollowupPlanId());
        }
        if (request.getInterventionGoal() != null) {
            plan.setInterventionGoal(trimOrNull(request.getInterventionGoal()));
        }
        if (request.getInterventionContent() != null) {
            plan.setInterventionContent(trimOrNull(request.getInterventionContent()));
        }
        plan.setStartDate(start);
        plan.setEndDate(end);
        if (request.getResidentVisible() != null) {
            plan.setResidentVisible(toFlag(request.getResidentVisible(), true));
        }
        if (request.getRemark() != null) {
            plan.setRemark(trimOrNull(request.getRemark()));
        }
        updateById(plan);
        return getForDoctor(doctorId, id);
    }

    /** 启动 / 恢复：草稿或已暂停 → 执行中。 */
    @Transactional
    public InterventionPlanVO activate(Long doctorId, Long id) {
        InterventionPlan plan = requirePlan(id);
        assertDoctorScope(doctorId, plan.getResidentId());
        InterventionStatus current = currentStatus(plan);
        if (current == InterventionStatus.ACTIVE) {
            throw new BusinessException(409, "该方案已在执行中");
        }
        if (!(current == InterventionStatus.DRAFT || current == InterventionStatus.SUSPENDED)) {
            throw new BusinessException(409, "当前状态（" + current.getLabel() + "）不可启动");
        }
        plan.setPlanStatus(InterventionStatus.ACTIVE.name());
        updateById(plan);
        return getForDoctor(doctorId, id);
    }

    /** 暂停：仅执行中可暂停。 */
    @Transactional
    public InterventionPlanVO suspend(Long doctorId, Long id) {
        InterventionPlan plan = requirePlan(id);
        assertDoctorScope(doctorId, plan.getResidentId());
        InterventionStatus current = currentStatus(plan);
        if (current != InterventionStatus.ACTIVE) {
            throw new BusinessException(409, "仅执行中的方案可以暂停");
        }
        plan.setPlanStatus(InterventionStatus.SUSPENDED.name());
        updateById(plan);
        return getForDoctor(doctorId, id);
    }

    /** 完成：执行中或已暂停 → 已完成。 */
    @Transactional
    public InterventionPlanVO complete(Long doctorId, Long id) {
        InterventionPlan plan = requirePlan(id);
        assertDoctorScope(doctorId, plan.getResidentId());
        InterventionStatus current = currentStatus(plan);
        if (current == InterventionStatus.COMPLETED) {
            throw new BusinessException(409, "该方案已完成，请勿重复提交");
        }
        if (current.isTerminal()) {
            throw new BusinessException(409, "已取消的方案不可完成");
        }
        if (current == InterventionStatus.DRAFT) {
            throw new BusinessException(409, "草稿方案需先启动执行后再完成");
        }
        plan.setPlanStatus(InterventionStatus.COMPLETED.name());
        updateById(plan);
        return getForDoctor(doctorId, id);
    }

    /** 取消：未结束的方案均可取消（含草稿、执行中、已暂停）。 */
    @Transactional
    public InterventionPlanVO cancel(Long doctorId, Long id) {
        InterventionPlan plan = requirePlan(id);
        assertDoctorScope(doctorId, plan.getResidentId());
        InterventionStatus current = currentStatus(plan);
        if (current.isTerminal()) {
            throw new BusinessException(409, "已" + current.getLabel() + "的方案不可取消");
        }
        plan.setPlanStatus(InterventionStatus.CANCELLED.name());
        updateById(plan);
        return getForDoctor(doctorId, id);
    }

    public List<InterventionPlanVO> listByResidentForDoctor(Long doctorId, Long residentId) {
        HealthProfile profile = healthProfileService.getByIdRequired(residentId);
        assertDoctorScope(doctorId, profile.getId());
        List<InterventionPlan> plans = lambdaQuery()
                .eq(InterventionPlan::getResidentId, profile.getId())
                .orderByDesc(InterventionPlan::getCreateTime)
                .orderByDesc(InterventionPlan::getId)
                .list();
        return assemble(plans);
    }

    /** 待处理 / 需调整：执行中或已暂停，且最近一次执行情况为「未执行 / 需要调整」或尚无记录。 */
    public List<InterventionPlanVO> todo(Long doctorId) {
        List<Long> scoped = doctorResidentService.listResidentIdsOfDoctor(doctorId);
        if (scoped.isEmpty()) {
            return List.of();
        }
        List<InterventionPlan> plans = lambdaQuery()
                .in(InterventionPlan::getResidentId, scoped)
                .in(InterventionPlan::getPlanStatus,
                        InterventionStatus.ACTIVE.name(), InterventionStatus.SUSPENDED.name())
                .orderByDesc(InterventionPlan::getCreateTime)
                .orderByDesc(InterventionPlan::getId)
                .list();
        if (plans.isEmpty()) {
            return List.of();
        }
        Map<Long, InterventionRecord> latest = latestRecordMap(plans.stream().map(InterventionPlan::getId).toList());
        List<InterventionPlan> attention = plans.stream().filter(p -> {
            InterventionRecord last = latest.get(p.getId());
            if (last == null || !StringUtils.hasText(last.getExecutionStatus())) {
                return true;
            }
            return ExecutionStatus.fromCode(last.getExecutionStatus())
                    .map(ExecutionStatus::needsAttention).orElse(true);
        }).toList();
        return assemble(attention);
    }

    /** 方案阶段性效果（来自执行记录，不新增存储）。 */
    public InterventionEffectVO effect(Long doctorId, Long id) {
        InterventionPlan plan = requirePlan(id);
        assertDoctorScope(doctorId, plan.getResidentId());
        List<InterventionRecord> records = interventionRecordMapper.selectList(
                new LambdaQueryWrapper<InterventionRecord>()
                        .eq(InterventionRecord::getInterventionPlanId, id)
                        .orderByDesc(InterventionRecord::getExecutedAt)
                        .orderByDesc(InterventionRecord::getId));

        InterventionEffectVO vo = new InterventionEffectVO();
        vo.setPlanId(plan.getId());
        vo.setPlanName(plan.getPlanName());
        vo.setRecordCount((long) records.size());
        vo.setDoctorRecordCount(records.stream()
                .filter(r -> OperatorRole.DOCTOR.name().equals(r.getOperatorRole())).count());
        vo.setResidentFeedbackCount(records.stream()
                .filter(r -> OperatorRole.RESIDENT.name().equals(r.getOperatorRole())).count());

        InterventionRecord latestDoctor = records.stream()
                .filter(r -> OperatorRole.DOCTOR.name().equals(r.getOperatorRole()))
                .findFirst().orElse(null);
        InterventionRecord latest = latestDoctor != null ? latestDoctor : records.stream().findFirst().orElse(null);
        if (latest != null) {
            vo.setLastExecutedAt(latest.getExecutedAt());
        }
        if (latestDoctor != null) {
            vo.setLatestExecutionStatus(latestDoctor.getExecutionStatus());
            vo.setLatestExecutionStatusLabel(ExecutionStatus.labelOf(latestDoctor.getExecutionStatus()));
            vo.setLatestEffectSummary(latestDoctor.getEffectSummary());
            vo.setLatestNextAction(latestDoctor.getNextAction());
        }
        boolean needsAttention = latestDoctor == null
                || !StringUtils.hasText(latestDoctor.getExecutionStatus())
                || ExecutionStatus.fromCode(latestDoctor.getExecutionStatus())
                .map(ExecutionStatus::needsAttention).orElse(true);
        vo.setNeedsAttention(needsAttention);
        return vo;
    }

    // ============================ 居民端 ============================

    /** 居民本人可见的方案（resident_visible=1），按创建时间倒序。 */
    public List<InterventionPlanVO> listForResident(Long residentId) {
        List<InterventionPlan> plans = lambdaQuery()
                .eq(InterventionPlan::getResidentId, residentId)
                .eq(InterventionPlan::getResidentVisible, 1)
                .orderByDesc(InterventionPlan::getCreateTime)
                .orderByDesc(InterventionPlan::getId)
                .list();
        List<InterventionPlanVO> vos = assemble(plans);
        vos.forEach(v -> v.setRemark(null));
        return vos;
    }

    public InterventionPlanVO getForResident(Long residentId, Long id) {
        InterventionPlan plan = getById(id);
        if (plan == null || !Objects.equals(plan.getResidentId(), residentId)
                || !Integer.valueOf(1).equals(plan.getResidentVisible())) {
            throw new BusinessException(404, "干预方案不存在");
        }
        InterventionPlanVO vo = assemble(List.of(plan)).get(0);
        vo.setRemark(null);
        return vo;
    }

    // ============================ 供记录服务复用 ============================

    public InterventionPlan requirePlan(Long id) {
        InterventionPlan plan = getById(id);
        if (plan == null) {
            throw new BusinessException(404, "干预方案不存在");
        }
        return plan;
    }

    public InterventionStatus currentStatus(InterventionPlan plan) {
        return InterventionStatus.fromCode(plan.getPlanStatus()).orElse(InterventionStatus.DRAFT);
    }

    public void assertDoctorScope(Long doctorId, Long residentId) {
        if (!doctorResidentService.isResidentInDoctorScope(doctorId, residentId)) {
            throw new BusinessException(403, "该居民不在您的管理范围内");
        }
    }

    public InterventionPlanVO toVO(InterventionPlan plan) {
        return assemble(List.of(plan)).get(0);
    }

    // ============================ 内部 ============================

    private void applyFilters(LambdaQueryWrapper<InterventionPlan> wrapper,
                              InterventionPlanQueryRequest query, List<Long> scoped) {
        if (StringUtils.hasText(query.getPlanStatus())) {
            InterventionStatus status = InterventionStatus.fromCode(query.getPlanStatus())
                    .orElseThrow(() -> new BusinessException(400, "方案状态取值不正确"));
            wrapper.eq(InterventionPlan::getPlanStatus, status.name());
        }
        if (StringUtils.hasText(query.getInterventionType())) {
            InterventionType type = InterventionType.fromCode(query.getInterventionType())
                    .orElseThrow(() -> new BusinessException(400, "干预类型取值不正确"));
            wrapper.eq(InterventionPlan::getInterventionType, type.name());
        }
        if (query.getResidentId() != null) {
            if (!scoped.contains(query.getResidentId())) {
                throw new BusinessException(403, "该居民不在您的管理范围内");
            }
            wrapper.eq(InterventionPlan::getResidentId, query.getResidentId());
        }
        if (query.getStartDateFrom() != null) {
            wrapper.ge(InterventionPlan::getStartDate, query.getStartDateFrom());
        }
        if (query.getStartDateTo() != null) {
            wrapper.le(InterventionPlan::getStartDate, query.getStartDateTo());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            List<Long> matched = matchResidentIdsByKeyword(query.getKeyword(), scoped);
            if (matched.isEmpty()) {
                wrapper.eq(InterventionPlan::getResidentId, -1L);
            } else {
                wrapper.in(InterventionPlan::getResidentId, matched);
            }
        }
    }

    private List<Long> matchResidentIdsByKeyword(String keyword, List<Long> scoped) {
        return healthProfileService.lambdaQuery()
                .in(HealthProfile::getId, scoped)
                .and(q -> q.like(HealthProfile::getResidentNo, keyword)
                        .or().like(HealthProfile::getRealName, keyword))
                .list()
                .stream().map(HealthProfile::getId).toList();
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessException(400, "开始日期不能晚于结束日期");
        }
    }

    /** 关联项必须存在且属于同一居民。 */
    private void validateLinkage(Long residentId, Long chronicId, Long alertId, Long followupId) {
        if (chronicId != null) {
            ChronicDiseaseRecord chronic = chronicDiseaseRecordMapper.selectById(chronicId);
            if (chronic == null) {
                throw new BusinessException(404, "关联的慢病档案不存在");
            }
            if (!Objects.equals(chronic.getResidentId(), residentId)) {
                throw new BusinessException(400, "关联的慢病档案不属于该居民");
            }
        }
        if (alertId != null) {
            AlertRecord alert = alertRecordMapper.selectById(alertId);
            if (alert == null) {
                throw new BusinessException(404, "关联的预警不存在");
            }
            if (!Objects.equals(alert.getResidentId(), residentId)) {
                throw new BusinessException(400, "关联的预警不属于该居民");
            }
        }
        if (followupId != null) {
            FollowupPlan followup = followupPlanMapper.selectById(followupId);
            if (followup == null) {
                throw new BusinessException(404, "关联的随访计划不存在");
            }
            if (!Objects.equals(followup.getResidentId(), residentId)) {
                throw new BusinessException(400, "关联的随访计划不属于该居民");
            }
        }
    }

    /** 每个方案最近一条执行记录（按 executed_at、id 倒序取首条）。 */
    private Map<Long, InterventionRecord> latestRecordMap(List<Long> planIds) {
        if (planIds == null || planIds.isEmpty()) {
            return Map.of();
        }
        List<InterventionRecord> records = interventionRecordMapper.selectList(
                new QueryWrapper<InterventionRecord>()
                        .in("intervention_plan_id", planIds)
                        .orderByDesc("executed_at")
                        .orderByDesc("id"));
        Map<Long, InterventionRecord> latest = new HashMap<>();
        for (InterventionRecord r : records) {
            latest.putIfAbsent(r.getInterventionPlanId(), r);
        }
        return latest;
    }

    /** 批量组装 VO：一次性加载居民、医生、社区、慢病、预警、随访计划与记录统计，避免逐条查询。 */
    private List<InterventionPlanVO> assemble(List<InterventionPlan> plans) {
        if (plans == null || plans.isEmpty()) {
            return List.of();
        }
        Set<Long> residentIds = plans.stream().map(InterventionPlan::getResidentId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> doctorIds = plans.stream().map(InterventionPlan::getDoctorId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, HealthProfile> profiles = healthProfileService.listByIds(residentIds).stream()
                .collect(Collectors.toMap(HealthProfile::getId, Function.identity(), (a, b) -> a));
        Map<Long, User> users = doctorIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(doctorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        Map<Long, String> communityNames = communityService.nameMap(
                profiles.values().stream().map(HealthProfile::getCommunityId).filter(Objects::nonNull).toList());

        Set<Long> residentUserIds = profiles.values().stream().map(HealthProfile::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> residentUsers = residentUserIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(residentUserIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        Set<Long> chronicIds = plans.stream().map(InterventionPlan::getChronicDiseaseRecordId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ChronicDiseaseRecord> chronicMap = chronicIds.isEmpty() ? Map.of()
                : chronicDiseaseRecordMapper.selectBatchIds(chronicIds).stream()
                .collect(Collectors.toMap(ChronicDiseaseRecord::getId, Function.identity(), (a, b) -> a));

        Set<Long> alertIds = plans.stream().map(InterventionPlan::getAlertId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, AlertRecord> alertMap = alertIds.isEmpty() ? Map.of()
                : alertRecordMapper.selectBatchIds(alertIds).stream()
                .collect(Collectors.toMap(AlertRecord::getId, Function.identity(), (a, b) -> a));

        Set<Long> followupIds = plans.stream().map(InterventionPlan::getFollowupPlanId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, FollowupPlan> followupMap = followupIds.isEmpty() ? Map.of()
                : followupPlanMapper.selectBatchIds(followupIds).stream()
                .collect(Collectors.toMap(FollowupPlan::getId, Function.identity(), (a, b) -> a));

        Map<Long, Long> counts = new HashMap<>();
        Map<Long, LocalDateTime> lastAt = new HashMap<>();
        Map<Long, String> latestStatus = new HashMap<>();
        if (!plans.isEmpty()) {
            List<InterventionRecord> records = interventionRecordMapper.selectList(
                    new QueryWrapper<InterventionRecord>()
                            .in("intervention_plan_id", plans.stream().map(InterventionPlan::getId).toList())
                            .orderByDesc("executed_at")
                            .orderByDesc("id"));
            for (InterventionRecord r : records) {
                Long pid = r.getInterventionPlanId();
                counts.merge(pid, 1L, Long::sum);
                // 记录已按时间倒序，首次出现的即为最近一条
                if (!lastAt.containsKey(pid)) {
                    lastAt.put(pid, r.getExecutedAt());
                    latestStatus.put(pid, r.getExecutionStatus());
                }
            }
        }

        List<InterventionPlanVO> result = new ArrayList<>(plans.size());
        for (InterventionPlan plan : plans) {
            InterventionPlanVO vo = new InterventionPlanVO();
            vo.setId(plan.getId());
            vo.setResidentId(plan.getResidentId());

            HealthProfile profile = profiles.get(plan.getResidentId());
            if (profile != null) {
                vo.setResidentNo(profile.getResidentNo());
                vo.setResidentName(StringUtils.hasText(profile.getRealName())
                        ? profile.getRealName() : profile.getResidentNo());
                vo.setGender(profile.getGender());
                vo.setGenderLabel(Gender.labelOf(profile.getGender()));
                vo.setBirthDate(profile.getBirthDate());
                vo.setCommunityName(communityNames.get(profile.getCommunityId()));
                User residentUser = residentUsers.get(profile.getUserId());
                if (residentUser != null) {
                    vo.setPhone(residentUser.getPhone());
                }
            }
            User doctor = users.get(plan.getDoctorId());
            vo.setDoctorId(plan.getDoctorId());
            vo.setDoctorName(doctor == null ? null : doctor.displayName());

            vo.setChronicDiseaseRecordId(plan.getChronicDiseaseRecordId());
            ChronicDiseaseRecord chronic = plan.getChronicDiseaseRecordId() == null ? null
                    : chronicMap.get(plan.getChronicDiseaseRecordId());
            if (chronic != null) {
                vo.setChronicDiseaseType(chronic.getDiseaseType());
                vo.setChronicDiseaseTypeLabel(DiseaseType.labelOf(chronic.getDiseaseType()));
            }
            vo.setAlertId(plan.getAlertId());
            AlertRecord alert = plan.getAlertId() == null ? null : alertMap.get(plan.getAlertId());
            if (alert != null) {
                vo.setAlertTitle(alert.getAlertTitle());
                vo.setAlertRiskLevel(alert.getRiskLevel());
                vo.setAlertRiskLevelLabel(RiskLevel.fromCode(alert.getRiskLevel())
                        .map(RiskLevel::getLabel).orElse(alert.getRiskLevel()));
            }
            vo.setFollowupPlanId(plan.getFollowupPlanId());
            FollowupPlan followup = plan.getFollowupPlanId() == null ? null
                    : followupMap.get(plan.getFollowupPlanId());
            if (followup != null) {
                vo.setFollowupPlanTitle(followup.getPlanTitle());
            }

            vo.setPlanName(plan.getPlanName());
            vo.setInterventionType(plan.getInterventionType());
            vo.setInterventionTypeLabel(InterventionType.labelOf(plan.getInterventionType()));
            vo.setInterventionGoal(plan.getInterventionGoal());
            vo.setInterventionContent(plan.getInterventionContent());
            vo.setStartDate(plan.getStartDate());
            vo.setEndDate(plan.getEndDate());

            InterventionStatus status = currentStatus(plan);
            vo.setPlanStatus(status.name());
            vo.setPlanStatusLabel(status.getLabel());

            vo.setResidentVisible(toBool(plan.getResidentVisible()));
            vo.setRemark(plan.getRemark());
            vo.setRecordCount(counts.getOrDefault(plan.getId(), 0L));
            vo.setLastExecutedAt(lastAt.get(plan.getId()));
            vo.setLatestExecutionStatus(latestStatus.get(plan.getId()));
            vo.setLatestExecutionStatusLabel(ExecutionStatus.labelOf(latestStatus.get(plan.getId())));
            vo.setCreateTime(plan.getCreateTime());
            vo.setUpdateTime(plan.getUpdateTime());
            result.add(vo);
        }
        return result;
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
