package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.dto.FollowupPlanCreateRequest;
import com.cdvtc.demo.dto.FollowupPlanQueryRequest;
import com.cdvtc.demo.dto.FollowupPlanUpdateRequest;
import com.cdvtc.demo.entity.*;
import com.cdvtc.demo.enums.*;
import com.cdvtc.demo.mapper.*;
import com.cdvtc.demo.vo.FollowupPlanVO;
import com.cdvtc.demo.vo.FollowupStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 随访计划。所有操作都以「当前医生管理范围内的居民」为边界，
 * 计划是否逾期由 scheduledAt 与当前状态动态计算，不落库。
 */
@Service
@RequiredArgsConstructor
public class FollowupPlanService extends ServiceImpl<FollowupPlanMapper, FollowupPlan> {

    private final HealthProfileService healthProfileService;
    private final DoctorResidentService doctorResidentService;
    private final CommunityService communityService;
    private final ChronicDiseaseRecordMapper chronicDiseaseRecordMapper;
    private final AlertRecordMapper alertRecordMapper;
    private final UserMapper userMapper;
    private final FollowupRecordMapper followupRecordMapper;

    // ============================ 医生端 ============================

    @Transactional
    public FollowupPlanVO create(Long doctorId, FollowupPlanCreateRequest request) {
        HealthProfile profile = healthProfileService.getByIdRequired(request.getResidentId());
        assertDoctorScope(doctorId, profile.getId());

        FollowupMethod method = FollowupMethod.fromCode(request.getFollowupMethod())
                .orElseThrow(() -> new BusinessException(400, "随访方式取值不正确"));
        if (request.getScheduledAt() == null) {
            throw new BusinessException(400, "计划随访时间不能为空");
        }
        validateLinkage(profile.getId(), request.getChronicDiseaseRecordId(), request.getAlertId());

        FollowupPlan plan = new FollowupPlan();
        plan.setResidentId(profile.getId());
        plan.setDoctorId(doctorId);
        plan.setChronicDiseaseRecordId(request.getChronicDiseaseRecordId());
        plan.setAlertId(request.getAlertId());
        plan.setPlanTitle(request.getPlanTitle().trim());
        plan.setFollowupPurpose(trimOrNull(request.getFollowupPurpose()));
        plan.setScheduledAt(request.getScheduledAt());
        plan.setFollowupMethod(method.name());
        plan.setPlanStatus(PlanStatus.PLANNED.name());
        plan.setResidentVisible(toFlag(request.getResidentVisible(), true));
        plan.setRemark(trimOrNull(request.getRemark()));
        save(plan);

        return getForDoctor(doctorId, plan.getId());
    }

    public IPage<FollowupPlanVO> pageForDoctor(Long doctorId, FollowupPlanQueryRequest query) {
        int page = normalizePage(query.getPage());
        int size = normalizeSize(query.getSize());
        List<Long> scoped = doctorResidentService.listResidentIdsOfDoctor(doctorId);
        if (scoped.isEmpty()) {
            return new Page<>(page, size, 0);
        }

        LambdaQueryWrapper<FollowupPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(FollowupPlan::getResidentId, scoped);

        applyFilters(wrapper, query, scoped);

        boolean overdueOnly = Boolean.TRUE.equals(query.getOverdueOnly());
        if (overdueOnly) {
            // 逾期 = 未结束且计划时间已过
            wrapper.in(FollowupPlan::getPlanStatus, PlanStatus.PLANNED.name(), PlanStatus.IN_PROGRESS.name())
                    .lt(FollowupPlan::getScheduledAt, LocalDateTime.now());
        }

        wrapper.orderByAsc(FollowupPlan::getScheduledAt).orderByDesc(FollowupPlan::getId);

        IPage<FollowupPlan> planPage = page(new Page<>(page, size), wrapper);
        Page<FollowupPlanVO> voPage = new Page<>(planPage.getCurrent(), planPage.getSize(), planPage.getTotal());
        voPage.setRecords(assemble(planPage.getRecords()));
        return voPage;
    }

    public FollowupPlanVO getForDoctor(Long doctorId, Long id) {
        FollowupPlan plan = requirePlan(id);
        assertDoctorScope(doctorId, plan.getResidentId());
        return assemble(List.of(plan)).get(0);
    }

    @Transactional
    public FollowupPlanVO update(Long doctorId, Long id, FollowupPlanUpdateRequest request) {
        FollowupPlan plan = requirePlan(id);
        assertDoctorScope(doctorId, plan.getResidentId());
        PlanStatus current = currentStatus(plan);
        if (current.isTerminal()) {
            throw new BusinessException(409, "已" + current.getLabel() + "的计划不可修改");
        }
        if (request.getPlanTitle() != null) {
            if (!StringUtils.hasText(request.getPlanTitle())) {
                throw new BusinessException(400, "计划标题不能为空");
            }
            plan.setPlanTitle(request.getPlanTitle().trim());
        }
        if (request.getFollowupPurpose() != null) {
            plan.setFollowupPurpose(trimOrNull(request.getFollowupPurpose()));
        }
        if (request.getScheduledAt() != null) {
            plan.setScheduledAt(request.getScheduledAt());
        }
        if (request.getFollowupMethod() != null) {
            plan.setFollowupMethod(FollowupMethod.fromCode(request.getFollowupMethod())
                    .orElseThrow(() -> new BusinessException(400, "随访方式取值不正确")).name());
        }
        if (request.getResidentVisible() != null) {
            plan.setResidentVisible(toFlag(request.getResidentVisible(), true));
        }
        if (request.getRemark() != null) {
            plan.setRemark(trimOrNull(request.getRemark()));
        }
        updateById(plan);
        return getForDoctor(doctorId, id);
    }

    @Transactional
    public FollowupPlanVO cancel(Long doctorId, Long id) {
        FollowupPlan plan = requirePlan(id);
        assertDoctorScope(doctorId, plan.getResidentId());
        PlanStatus current = currentStatus(plan);
        if (current == PlanStatus.CANCELLED) {
            throw new BusinessException(409, "该计划已取消");
        }
        if (current == PlanStatus.COMPLETED) {
            throw new BusinessException(409, "已完成的计划不可取消");
        }
        plan.setPlanStatus(PlanStatus.CANCELLED.name());
        updateById(plan);
        return getForDoctor(doctorId, id);
    }

    /** 待办计划：待执行/进行中，按计划时间升序（逾期在前）。 */
    public List<FollowupPlanVO> todo(Long doctorId, int limit) {
        List<Long> scoped = doctorResidentService.listResidentIdsOfDoctor(doctorId);
        if (scoped.isEmpty()) {
            return List.of();
        }
        int top = limit <= 0 ? 10 : Math.min(limit, 50);
        List<FollowupPlan> plans = lambdaQuery()
                .in(FollowupPlan::getResidentId, scoped)
                .in(FollowupPlan::getPlanStatus, PlanStatus.PLANNED.name(), PlanStatus.IN_PROGRESS.name())
                .orderByAsc(FollowupPlan::getScheduledAt)
                .last("LIMIT " + top)
                .list();
        return assemble(plans);
    }

    /** 即将到期或已逾期：未结束且计划时间在 now+days 之内。 */
    public List<FollowupPlanVO> upcoming(Long doctorId, int days) {
        List<Long> scoped = doctorResidentService.listResidentIdsOfDoctor(doctorId);
        if (scoped.isEmpty()) {
            return List.of();
        }
        int d = days <= 0 ? 7 : Math.min(days, 90);
        LocalDateTime deadline = LocalDateTime.now().plusDays(d);
        List<FollowupPlan> plans = lambdaQuery()
                .in(FollowupPlan::getResidentId, scoped)
                .in(FollowupPlan::getPlanStatus, PlanStatus.PLANNED.name(), PlanStatus.IN_PROGRESS.name())
                .lt(FollowupPlan::getScheduledAt, deadline)
                .orderByAsc(FollowupPlan::getScheduledAt)
                .list();
        return assemble(plans);
    }

    public FollowupStatsVO stats(Long doctorId) {
        FollowupStatsVO vo = new FollowupStatsVO();
        List<Long> scoped = doctorResidentService.listResidentIdsOfDoctor(doctorId);
        if (scoped.isEmpty()) {
            vo.setPlannedCount(0L);
            vo.setInProgressCount(0L);
            vo.setCompletedCount(0L);
            vo.setCancelledCount(0L);
            vo.setOverdueCount(0L);
            return vo;
        }
        vo.setPlannedCount(countByStatus(scoped, PlanStatus.PLANNED));
        vo.setInProgressCount(countByStatus(scoped, PlanStatus.IN_PROGRESS));
        vo.setCompletedCount(countByStatus(scoped, PlanStatus.COMPLETED));
        vo.setCancelledCount(countByStatus(scoped, PlanStatus.CANCELLED));
        vo.setOverdueCount(lambdaQuery()
                .in(FollowupPlan::getResidentId, scoped)
                .in(FollowupPlan::getPlanStatus, PlanStatus.PLANNED.name(), PlanStatus.IN_PROGRESS.name())
                .lt(FollowupPlan::getScheduledAt, LocalDateTime.now())
                .count());
        return vo;
    }

    public List<FollowupPlanVO> listByResidentForDoctor(Long doctorId, Long residentId) {
        HealthProfile profile = healthProfileService.getByIdRequired(residentId);
        assertDoctorScope(doctorId, profile.getId());
        List<FollowupPlan> plans = lambdaQuery()
                .eq(FollowupPlan::getResidentId, profile.getId())
                .orderByDesc(FollowupPlan::getScheduledAt)
                .orderByDesc(FollowupPlan::getId)
                .list();
        return assemble(plans);
    }

    // ============================ 居民端 ============================

    /** 居民本人可见的计划（resident_visible=1），按计划时间倒序。 */
    public List<FollowupPlanVO> listForResident(Long residentId) {
        List<FollowupPlan> plans = lambdaQuery()
                .eq(FollowupPlan::getResidentId, residentId)
                .eq(FollowupPlan::getResidentVisible, 1)
                .orderByDesc(FollowupPlan::getScheduledAt)
                .orderByDesc(FollowupPlan::getId)
                .list();
        List<FollowupPlanVO> vos = assemble(plans);
        // 居民端不展示医生内部备注
        vos.forEach(v -> v.setRemark(null));
        return vos;
    }

    // ============================ 供记录服务复用 ============================

    public FollowupPlan requirePlan(Long id) {
        FollowupPlan plan = getById(id);
        if (plan == null) {
            throw new BusinessException(404, "随访计划不存在");
        }
        return plan;
    }

    public PlanStatus currentStatus(FollowupPlan plan) {
        return PlanStatus.fromCode(plan.getPlanStatus()).orElse(PlanStatus.PLANNED);
    }

    public void assertDoctorScope(Long doctorId, Long residentId) {
        if (!doctorResidentService.isResidentInDoctorScope(doctorId, residentId)) {
            throw new BusinessException(403, "该居民不在您的管理范围内");
        }
    }

    public FollowupPlanVO toVO(FollowupPlan plan) {
        return assemble(List.of(plan)).get(0);
    }

    // ============================ 内部 ============================

    private void applyFilters(LambdaQueryWrapper<FollowupPlan> wrapper,
                              FollowupPlanQueryRequest query, List<Long> scoped) {
        if (StringUtils.hasText(query.getPlanStatus())) {
            PlanStatus status = PlanStatus.fromCode(query.getPlanStatus())
                    .orElseThrow(() -> new BusinessException(400, "计划状态取值不正确"));
            wrapper.eq(FollowupPlan::getPlanStatus, status.name());
        }
        if (StringUtils.hasText(query.getFollowupMethod())) {
            FollowupMethod method = FollowupMethod.fromCode(query.getFollowupMethod())
                    .orElseThrow(() -> new BusinessException(400, "随访方式取值不正确"));
            wrapper.eq(FollowupPlan::getFollowupMethod, method.name());
        }
        if (query.getResidentId() != null) {
            if (!scoped.contains(query.getResidentId())) {
                throw new BusinessException(403, "该居民不在您的管理范围内");
            }
            wrapper.eq(FollowupPlan::getResidentId, query.getResidentId());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(FollowupPlan::getScheduledAt, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(FollowupPlan::getScheduledAt, query.getEndTime());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            List<Long> matched = matchResidentIdsByKeyword(query.getKeyword(), scoped);
            if (matched.isEmpty()) {
                wrapper.eq(FollowupPlan::getResidentId, -1L);
            } else {
                wrapper.in(FollowupPlan::getResidentId, matched);
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

    private long countByStatus(List<Long> scoped, PlanStatus status) {
        return lambdaQuery()
                .in(FollowupPlan::getResidentId, scoped)
                .eq(FollowupPlan::getPlanStatus, status.name())
                .count();
    }

    private void validateLinkage(Long residentId, Long chronicId, Long alertId) {
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
    }

    /** 批量组装 VO：一次性加载居民、医生、社区、慢病、预警与记录统计，避免逐条查询。 */
    private List<FollowupPlanVO> assemble(List<FollowupPlan> plans) {
        if (plans == null || plans.isEmpty()) {
            return List.of();
        }
        Set<Long> residentIds = plans.stream().map(FollowupPlan::getResidentId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> doctorIds = plans.stream().map(FollowupPlan::getDoctorId)
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

        Set<Long> chronicIds = plans.stream().map(FollowupPlan::getChronicDiseaseRecordId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ChronicDiseaseRecord> chronicMap = chronicIds.isEmpty() ? Map.of()
                : chronicDiseaseRecordMapper.selectBatchIds(chronicIds).stream()
                .collect(Collectors.toMap(ChronicDiseaseRecord::getId, Function.identity(), (a, b) -> a));

        Set<Long> alertIds = plans.stream().map(FollowupPlan::getAlertId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, AlertRecord> alertMap = alertIds.isEmpty() ? Map.of()
                : alertRecordMapper.selectBatchIds(alertIds).stream()
                .collect(Collectors.toMap(AlertRecord::getId, Function.identity(), (a, b) -> a));

        Map<Long, long[]> counts = new HashMap<>();
        Map<Long, LocalDateTime> lastAt = new HashMap<>();
        if (!plans.isEmpty()) {
            QueryWrapper<FollowupRecord> qw = new QueryWrapper<>();
            qw.select("plan_id", "COUNT(*) AS cnt", "MAX(followed_up_at) AS last_at")
                    .in("plan_id", plans.stream().map(FollowupPlan::getId).toList())
                    .groupBy("plan_id");
            for (Map<String, Object> row : followupRecordMapper.selectMaps(qw)) {
                Long pid = toLong(row.get("plan_id"));
                if (pid == null) {
                    continue;
                }
                counts.put(pid, new long[]{toLong(row.get("cnt")) == null ? 0L : toLong(row.get("cnt"))});
                lastAt.put(pid, toDateTime(row.get("last_at")));
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<FollowupPlanVO> result = new ArrayList<>(plans.size());
        for (FollowupPlan plan : plans) {
            FollowupPlanVO vo = new FollowupPlanVO();
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
            }
            User doctor = users.get(plan.getDoctorId());
            vo.setDoctorId(plan.getDoctorId());
            vo.setDoctorName(doctor == null ? null : doctor.displayName());
            // 联系电话取居民账号手机号
            if (profile != null) {
                User residentUser = residentUsers.get(profile.getUserId());
                if (residentUser != null) {
                    vo.setPhone(residentUser.getPhone());
                }
            }

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

            vo.setPlanTitle(plan.getPlanTitle());
            vo.setFollowupPurpose(plan.getFollowupPurpose());
            vo.setScheduledAt(plan.getScheduledAt());
            vo.setFollowupMethod(plan.getFollowupMethod());
            vo.setFollowupMethodLabel(FollowupMethod.labelOf(plan.getFollowupMethod()));

            PlanStatus status = currentStatus(plan);
            vo.setPlanStatus(status.name());
            vo.setPlanStatusLabel(status.getLabel());

            boolean overdue = status.isOpen() && plan.getScheduledAt() != null
                    && plan.getScheduledAt().isBefore(now);
            vo.setOverdue(overdue);
            vo.setOverdueLabel(overdue ? "已逾期" : null);

            vo.setResidentVisible(toBool(plan.getResidentVisible()));
            vo.setRemark(plan.getRemark());

            long[] c = counts.get(plan.getId());
            vo.setRecordCount(c == null ? 0L : c[0]);
            vo.setLastFollowedUpAt(lastAt.get(plan.getId()));
            vo.setCreateTime(plan.getCreateTime());
            vo.setUpdateTime(plan.getUpdateTime());
            result.add(vo);
        }
        return result;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime toDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime dt) {
            return dt;
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        return null;
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
