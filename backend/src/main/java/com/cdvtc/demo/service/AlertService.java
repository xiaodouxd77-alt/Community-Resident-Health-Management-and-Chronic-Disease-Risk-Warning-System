package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.dto.AlertHandleRequest;
import com.cdvtc.demo.dto.AlertQueryRequest;
import com.cdvtc.demo.entity.*;
import com.cdvtc.demo.enums.AlertStatus;
import com.cdvtc.demo.enums.HandlingAction;
import com.cdvtc.demo.enums.IndicatorType;
import com.cdvtc.demo.enums.RiskLevel;
import com.cdvtc.demo.mapper.AlertHandlingRecordMapper;
import com.cdvtc.demo.mapper.AlertRecordMapper;
import com.cdvtc.demo.mapper.HealthIndicatorRecordMapper;
import com.cdvtc.demo.mapper.UserMapper;
import com.cdvtc.demo.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService extends ServiceImpl<AlertRecordMapper, AlertRecord> {

    private static final DateTimeFormatter CODE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int RECENT_INDICATOR_LIMIT = 10;

    private final DoctorResidentService doctorResidentService;
    private final HealthProfileService healthProfileService;
    private final IndicatorAssembler indicatorAssembler;
    private final RiskAssessmentService riskAssessmentService;
    private final AlertHandlingRecordMapper handlingRecordMapper;
    private final HealthIndicatorRecordMapper indicatorRecordMapper;
    private final UserMapper userMapper;

    // ============================ 创建 ============================

    /**
     * 评估命中"中风险及以上"时创建预警。以 assessment_id 唯一约束保证幂等。
     * 返回 null 表示本次不产生预警。
     */
    @Transactional
    public AlertRecord createIfNeeded(HealthIndicatorRecord record, RiskAssessment assessment,
                                      RiskAssessmentService.Outcome outcome) {
        if (!outcome.getRiskLevel().shouldAlert() || outcome.getMatchedRules().isEmpty()) {
            return null;
        }
        AlertRecord existing = lambdaQuery().eq(AlertRecord::getAssessmentId, assessment.getId()).one();
        if (existing != null) {
            return existing;
        }

        MatchedRuleVO topRule = outcome.getMatchedRules().stream()
                .max(Comparator.comparingInt(m -> RiskLevel.fromCode(m.getRiskLevel())
                        .orElse(RiskLevel.LOW).getPriority()))
                .orElseThrow();

        IndicatorType type = IndicatorType.fromCode(record.getIndicatorType()).orElse(null);
        String typeLabel = type != null ? type.getLabel() : record.getIndicatorType();
        RiskLevel riskLevel = outcome.getRiskLevel();

        AlertRecord alert = new AlertRecord();
        alert.setAlertNo(generateCode("AL"));
        alert.setResidentId(record.getResidentId());
        alert.setAssessmentId(assessment.getId());
        alert.setIndicatorRecordId(record.getId());
        alert.setAlertType(topRule.getAlertType());
        alert.setRiskLevel(riskLevel.name());
        alert.setAlertTitle(typeLabel + "异常预警（" + riskLevel.getLabel() + "）");
        alert.setAlertReason(buildReason(record, topRule));
        alert.setStatus(AlertStatus.PENDING.name());
        // 有明确授权关系时指派责任医生，否则待分配（不随机分配）
        alert.setAssignedDoctorId(doctorResidentService.findDoctorIdForResident(record.getResidentId()));

        try {
            save(alert);
        } catch (DuplicateKeyException e) {
            return lambdaQuery().eq(AlertRecord::getAssessmentId, assessment.getId()).one();
        }
        return alert;
    }

    private String buildReason(HealthIndicatorRecord record, MatchedRuleVO rule) {
        String unit = record.getUnit() == null ? "" : record.getUnit();
        String reason = "命中规则[" + rule.getRuleName() + "]：" + rule.getValueFieldLabel() + " "
                + plain(rule.getActualValue()) + " " + unit + " " + rule.getOperator() + " "
                + plain(rule.getThresholdValue()) + " " + unit + "；" + rule.getDescription();
        return reason.length() > 490 ? reason.substring(0, 490) : reason;
    }

    // ============================ 居民端查询 ============================

    public IPage<AlertVO> pageResidentAlerts(Long residentId, AlertQueryRequest query) {
        LambdaQueryWrapper<AlertRecord> wrapper = baseQueryWrapper(query);
        wrapper.eq(AlertRecord::getResidentId, residentId);
        return pageToVO(page(new Page<>(normalizePage(query), normalizeSize(query)), wrapper));
    }

    public AlertDetailVO getResidentAlertDetail(Long residentId, Long alertId) {
        AlertRecord alert = requireAlert(alertId);
        if (!Objects.equals(alert.getResidentId(), residentId)) {
            // 不暴露他人资源是否存在
            throw new BusinessException(404, "预警不存在");
        }
        return buildDetail(alert);
    }

    // ============================ 医生端查询 ============================

    public IPage<AlertVO> pageDoctorAlerts(Long doctorId, AlertQueryRequest query) {
        List<Long> scopedResidentIds = doctorResidentService.listResidentIdsOfDoctor(doctorId);
        if (scopedResidentIds.isEmpty()) {
            return new Page<>(normalizePage(query), normalizeSize(query), 0);
        }
        if (query.getResidentId() != null && !scopedResidentIds.contains(query.getResidentId())) {
            throw new BusinessException(403, "无权查看该居民的数据");
        }

        LambdaQueryWrapper<AlertRecord> wrapper = baseQueryWrapper(query);
        if (query.getResidentId() != null) {
            wrapper.eq(AlertRecord::getResidentId, query.getResidentId());
        } else {
            wrapper.in(AlertRecord::getResidentId, scopedResidentIds);
        }

        if (StringUtils.hasText(query.getKeyword())) {
            List<Long> matchedResidentIds = matchResidentIdsByKeyword(query.getKeyword(), scopedResidentIds);
            if (matchedResidentIds.isEmpty()) {
                return new Page<>(normalizePage(query), normalizeSize(query), 0);
            }
            wrapper.in(AlertRecord::getResidentId, matchedResidentIds);
        }

        return pageToVO(page(new Page<>(normalizePage(query), normalizeSize(query)), wrapper));
    }

    public AlertDetailVO getDoctorAlertDetail(Long doctorId, Long alertId) {
        AlertRecord alert = requireAlert(alertId);
        assertDoctorScope(doctorId, alert);
        return buildDetail(alert);
    }

    // ============================ 医生端处理 ============================

    @Transactional
    public AlertDetailVO handle(Long doctorId, Long alertId, AlertHandleRequest request) {
        AlertRecord alert = requireAlert(alertId);
        assertDoctorScope(doctorId, alert);

        AlertStatus current = AlertStatus.fromCode(alert.getStatus()).orElse(AlertStatus.PENDING);
        HandlingAction action = HandlingAction.fromCode(request.getActionType())
                .orElseThrow(() -> new BusinessException(400, "不支持的处理动作: " + request.getActionType()));

        if (current == AlertStatus.RESOLVED) {
            throw new BusinessException(409, "该预警已完成处理，如需重新处理请使用重开操作");
        }
        if (action == HandlingAction.RESOLVE && current == AlertStatus.PENDING) {
            throw new BusinessException(409, "请先开始处理，再完成本次预警处置");
        }
        if (request.getNextFollowupAt() != null
                && request.getNextFollowupAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(400, "建议随访时间不能早于当前时间");
        }
        if (!StringUtils.hasText(request.getHandlingComment())) {
            throw new BusinessException(400, "处理意见不能为空");
        }

        AlertHandlingRecord handling = new AlertHandlingRecord();
        handling.setAlertId(alert.getId());
        handling.setDoctorId(doctorId);
        handling.setActionType(action.name());
        handling.setHandlingComment(request.getHandlingComment().trim());
        boolean followup = Boolean.TRUE.equals(request.getFollowupRequired());
        handling.setFollowupRequired(followup ? 1 : 0);
        handling.setNextFollowupAt(followup ? request.getNextFollowupAt() : null);
        handlingRecordMapper.insert(handling);

        alert.setStatus(action.getTargetStatus().name());
        if (alert.getAssignedDoctorId() == null) {
            alert.setAssignedDoctorId(doctorId);
        }
        updateById(alert);

        return buildDetail(alert);
    }

    public List<HandlingRecordVO> listHandlingRecords(Long alertId) {
        List<AlertHandlingRecord> records = handlingRecordMapper.selectList(
                new LambdaQueryWrapper<AlertHandlingRecord>()
                        .eq(AlertHandlingRecord::getAlertId, alertId)
                        .orderByAsc(AlertHandlingRecord::getCreateTime)
                        .orderByAsc(AlertHandlingRecord::getId));
        Map<Long, User> doctors = loadUsers(records.stream()
                .map(AlertHandlingRecord::getDoctorId).filter(Objects::nonNull).distinct().toList());
        return records.stream().map(r -> toHandlingVO(r, doctors)).toList();
    }

    // ============================ 组装 ============================

    public List<AlertVO> toVOList(List<AlertRecord> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> residentIds = alerts.stream().map(AlertRecord::getResidentId).distinct().toList();
        List<Long> recordIds = alerts.stream().map(AlertRecord::getIndicatorRecordId).distinct().toList();
        List<Long> doctorIds = alerts.stream().map(AlertRecord::getAssignedDoctorId)
                .filter(Objects::nonNull).distinct().toList();
        List<Long> alertIds = alerts.stream().map(AlertRecord::getId).toList();

        Map<Long, HealthProfile> profiles = loadProfiles(residentIds);
        Map<Long, HealthIndicatorRecord> records = loadRecords(recordIds);
        Map<Long, User> doctors = loadUsers(doctorIds);

        Map<Long, AlertHandlingRecord> latestHandling = new HashMap<>();
        List<AlertHandlingRecord> handlingRecords = handlingRecordMapper.selectList(
                new LambdaQueryWrapper<AlertHandlingRecord>()
                        .in(AlertHandlingRecord::getAlertId, alertIds)
                        .orderByDesc(AlertHandlingRecord::getCreateTime)
                        .orderByDesc(AlertHandlingRecord::getId));
        for (AlertHandlingRecord hr : handlingRecords) {
            latestHandling.putIfAbsent(hr.getAlertId(), hr);
        }

        return alerts.stream()
                .map(a -> toVO(a, profiles.get(a.getResidentId()),
                        records.get(a.getIndicatorRecordId()), doctors.get(a.getAssignedDoctorId()),
                        latestHandling.get(a.getId())))
                .toList();
    }

    private AlertVO toVO(AlertRecord alert, HealthProfile profile, HealthIndicatorRecord record,
                         User doctor, AlertHandlingRecord latest) {
        AlertVO vo = new AlertVO();
        vo.setId(alert.getId());
        vo.setAlertNo(alert.getAlertNo());
        vo.setResidentId(alert.getResidentId());
        vo.setAssessmentId(alert.getAssessmentId());
        vo.setIndicatorRecordId(alert.getIndicatorRecordId());
        vo.setAlertType(alert.getAlertType());
        vo.setRiskLevel(alert.getRiskLevel());
        vo.setRiskLevelLabel(RiskAssessmentService.labelOfRiskLevel(alert.getRiskLevel()));
        vo.setAlertTitle(alert.getAlertTitle());
        vo.setAlertReason(alert.getAlertReason());
        vo.setStatus(alert.getStatus());
        vo.setStatusLabel(AlertStatus.fromCode(alert.getStatus()).map(AlertStatus::getLabel).orElse(alert.getStatus()));
        vo.setAssignedDoctorId(alert.getAssignedDoctorId());
        vo.setCreateTime(alert.getCreateTime());

        if (profile != null) {
            vo.setResidentNo(profile.getResidentNo());
            vo.setResidentName(StringUtils.hasText(profile.getRealName())
                    ? profile.getRealName() : profile.getResidentNo());
        }
        if (doctor != null) {
            vo.setAssignedDoctorName(doctor.displayName());
        }
        if (record != null) {
            vo.setIndicatorType(record.getIndicatorType());
            vo.setIndicatorTypeLabel(IndicatorType.fromCode(record.getIndicatorType())
                    .map(IndicatorType::getLabel).orElse(record.getIndicatorType()));
            vo.setNumericValue(record.getNumericValue());
            vo.setSecondaryValue(record.getSecondaryValue());
            vo.setUnit(record.getUnit());
            vo.setMeasuredAt(record.getMeasuredAt());
        }
        if (latest != null) {
            vo.setLatestHandlingComment(latest.getHandlingComment());
            vo.setLatestHandlingTime(latest.getCreateTime());
        }
        return vo;
    }

    private AlertDetailVO buildDetail(AlertRecord alert) {
        AlertDetailVO detail = new AlertDetailVO();

        AlertVO alertVO = toVOList(List.of(alert)).get(0);
        detail.setAlert(alertVO);

        HealthProfile profile = healthProfileService.getById(alert.getResidentId());
        User residentUser = profile == null ? null : userMapper.selectById(profile.getUserId());
        detail.setResident(profile == null ? null : healthProfileService.toBrief(profile, residentUser));

        HealthIndicatorRecord record = indicatorRecordMapper.selectById(alert.getIndicatorRecordId());
        RiskAssessment assessment = riskAssessmentService.getById(alert.getAssessmentId());
        detail.setAssessment(assessment == null ? null
                : riskAssessmentService.toVO(assessment, record, alert));
        detail.setHandlingRecords(listHandlingRecords(alert.getId()));

        if (record != null) {
            List<HealthIndicatorRecord> recent = indicatorRecordMapper.selectList(
                    new LambdaQueryWrapper<HealthIndicatorRecord>()
                            .eq(HealthIndicatorRecord::getResidentId, alert.getResidentId())
                            .eq(HealthIndicatorRecord::getIndicatorType, record.getIndicatorType())
                            .orderByDesc(HealthIndicatorRecord::getMeasuredAt)
                            .orderByDesc(HealthIndicatorRecord::getId)
                            .last("LIMIT " + RECENT_INDICATOR_LIMIT));
            detail.setRecentIndicators(indicatorAssembler.toVOList(recent));
        } else {
            detail.setRecentIndicators(Collections.emptyList());
        }
        return detail;
    }

    private IPage<AlertVO> pageToVO(IPage<AlertRecord> alertPage) {
        List<AlertVO> voList = toVOList(alertPage.getRecords());
        Page<AlertVO> voPage = new Page<>(alertPage.getCurrent(), alertPage.getSize(), alertPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    // ============================ 工具 ============================

    private LambdaQueryWrapper<AlertRecord> baseQueryWrapper(AlertQueryRequest query) {
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(AlertRecord::getStatus, query.getStatus().trim().toUpperCase());
        }
        if (StringUtils.hasText(query.getRiskLevel())) {
            wrapper.eq(AlertRecord::getRiskLevel, query.getRiskLevel().trim().toUpperCase());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(AlertRecord::getCreateTime, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(AlertRecord::getCreateTime, query.getEndTime());
        }
        wrapper.orderByDesc(AlertRecord::getCreateTime).orderByDesc(AlertRecord::getId);
        return wrapper;
    }

    private List<Long> matchResidentIdsByKeyword(String keyword, List<Long> scopedResidentIds) {
        List<HealthProfile> profiles = healthProfileService.lambdaQuery()
                .in(HealthProfile::getId, scopedResidentIds)
                .and(q -> q.like(HealthProfile::getResidentNo, keyword)
                        .or().like(HealthProfile::getRealName, keyword))
                .list();
        return profiles.stream().map(HealthProfile::getId).toList();
    }

    private AlertRecord requireAlert(Long alertId) {
        AlertRecord alert = getById(alertId);
        if (alert == null) {
            throw new BusinessException(404, "预警不存在");
        }
        return alert;
    }

    private void assertDoctorScope(Long doctorId, AlertRecord alert) {
        boolean assigned = Objects.equals(alert.getAssignedDoctorId(), doctorId);
        boolean inScope = doctorResidentService.isResidentInDoctorScope(doctorId, alert.getResidentId());
        if (!assigned && !inScope) {
            throw new BusinessException(403, "该预警不在您的管理范围内");
        }
    }

    private HandlingRecordVO toHandlingVO(AlertHandlingRecord record, Map<Long, User> doctors) {
        HandlingRecordVO vo = new HandlingRecordVO();
        vo.setId(record.getId());
        vo.setDoctorId(record.getDoctorId());
        User doctor = doctors.get(record.getDoctorId());
        vo.setDoctorName(doctor != null ? doctor.displayName() : null);
        vo.setActionType(record.getActionType());
        vo.setActionLabel(HandlingAction.fromCode(record.getActionType())
                .map(HandlingAction::getLabel).orElse(record.getActionType()));
        vo.setHandlingComment(record.getHandlingComment());
        vo.setFollowupRequired(record.getFollowupRequired() != null && record.getFollowupRequired() == 1);
        vo.setNextFollowupAt(record.getNextFollowupAt());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    private Map<Long, HealthProfile> loadProfiles(List<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return healthProfileService.listByIds(ids).stream()
                .collect(Collectors.toMap(HealthProfile::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, HealthIndicatorRecord> loadRecords(List<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return indicatorRecordMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(HealthIndicatorRecord::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, User> loadUsers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
    }

    private int normalizePage(AlertQueryRequest query) {
        return query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
    }

    private int normalizeSize(AlertQueryRequest query) {
        int size = query.getSize() == null ? 10 : query.getSize();
        return size < 1 || size > 100 ? 10 : size;
    }

    private String plain(java.math.BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String generateCode(String prefix) {
        return prefix + LocalDateTime.now().format(CODE_FMT)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
