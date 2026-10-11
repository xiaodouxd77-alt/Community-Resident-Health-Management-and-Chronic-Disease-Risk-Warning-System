package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.dto.HighRiskResidentQueryRequest;
import com.cdvtc.demo.entity.*;
import com.cdvtc.demo.enums.AlertStatus;
import com.cdvtc.demo.enums.DiagnosisStatus;
import com.cdvtc.demo.enums.DiseaseType;
import com.cdvtc.demo.enums.Gender;
import com.cdvtc.demo.enums.ManagementStatus;
import com.cdvtc.demo.enums.RiskLevel;
import com.cdvtc.demo.mapper.*;
import com.cdvtc.demo.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.Period;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 高风险居民筛查。风险等级取最近一次风险评估结果，未处理预警与已核实慢病为独立维度，
 * 三者不做相互推导。所有查询以医生管理范围为边界（管理员传 null 表示全库）。
 */
@Service
@RequiredArgsConstructor
public class HighRiskResidentService {

    private static final int DETAIL_ALERT_LIMIT = 20;
    private static final int DETAIL_INDICATOR_LIMIT = 10;

    private final HealthStatisticsMapper statisticsMapper;
    private final DoctorResidentService doctorResidentService;
    private final HealthProfileService healthProfileService;
    private final ChronicDiseaseService chronicDiseaseService;
    private final ChronicDiseaseRecordMapper chronicDiseaseRecordMapper;
    private final AlertService alertService;
    private final AlertRecordMapper alertRecordMapper;
    private final RiskAssessmentService riskAssessmentService;
    private final IndicatorAssembler indicatorAssembler;
    private final HealthIndicatorRecordMapper indicatorRecordMapper;
    private final FollowupRecordMapper followupRecordMapper;
    private final InterventionRecordMapper interventionRecordMapper;

    // ============================ 分页筛查 ============================

    public IPage<HighRiskResidentVO> page(Long doctorId, HighRiskResidentQueryRequest query) {
        List<Long> scoped = doctorResidentService.listResidentIdsOfDoctor(doctorId);
        if (scoped.isEmpty()) {
            return new Page<>(normalizePage(query.getPage()), normalizeSize(query.getSize()), 0);
        }
        return doPage(scoped, query);
    }

    public IPage<HighRiskResidentVO> pageAll(HighRiskResidentQueryRequest query) {
        return doPage(null, query);
    }

    private IPage<HighRiskResidentVO> doPage(List<Long> scopedIds, HighRiskResidentQueryRequest query) {
        validateEnums(query);

        Page<HighRiskResidentVO> page = new Page<>(normalizePage(query.getPage()), normalizeSize(query.getSize()));
        IPage<HighRiskResidentVO> result = statisticsMapper.pageHighRiskResidents(page, scopedIds, query);

        List<HighRiskResidentVO> rows = result.getRecords();
        if (!rows.isEmpty()) {
            enrichRows(rows);
        }
        return result;
    }

    private void enrichRows(List<HighRiskResidentVO> rows) {
        List<Long> residentIds = rows.stream().map(HighRiskResidentVO::getResidentId).toList();
        Map<Long, List<ChronicSummaryVO>> chronicMap = loadConfirmedChronic(residentIds);
        for (HighRiskResidentVO vo : rows) {
            vo.setGenderLabel(Gender.labelOf(vo.getGender()));
            vo.setAge(ageOf(vo.getBirthDate()));
            vo.setLatestRiskLevelLabel(RiskLevel.fromCode(vo.getLatestRiskLevel())
                    .map(RiskLevel::getLabel).orElse(null));
            if (!StringUtils.hasText(vo.getRealName())) {
                vo.setRealName(vo.getResidentNo());
            }
            vo.setChronicSummaries(chronicMap.getOrDefault(vo.getResidentId(), Collections.emptyList()));
        }
    }

    private Map<Long, List<ChronicSummaryVO>> loadConfirmedChronic(List<Long> residentIds) {
        if (residentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ChronicDiseaseRecord> records = chronicDiseaseRecordMapper.selectList(
                new LambdaQueryWrapper<ChronicDiseaseRecord>()
                        .in(ChronicDiseaseRecord::getResidentId, residentIds)
                        .eq(ChronicDiseaseRecord::getDiagnosisStatus, DiagnosisStatus.CONFIRMED.name())
                        .orderByDesc(ChronicDiseaseRecord::getCreateTime)
                        .orderByDesc(ChronicDiseaseRecord::getId));
        Map<Long, List<ChronicSummaryVO>> map = new HashMap<>();
        for (ChronicDiseaseRecord r : records) {
            map.computeIfAbsent(r.getResidentId(), k -> new ArrayList<>()).add(toChronicSummary(r));
        }
        return map;
    }

    private ChronicSummaryVO toChronicSummary(ChronicDiseaseRecord r) {
        ChronicSummaryVO vo = new ChronicSummaryVO();
        vo.setRecordId(r.getId());
        vo.setDiseaseType(r.getDiseaseType());
        vo.setDiseaseTypeLabel(DiseaseType.labelOf(r.getDiseaseType()));
        vo.setDiagnosisStatus(r.getDiagnosisStatus());
        vo.setDiagnosisStatusLabel(DiagnosisStatus.labelOf(r.getDiagnosisStatus()));
        vo.setManagementStatus(r.getManagementStatus());
        vo.setManagementStatusLabel(ManagementStatus.labelOf(r.getManagementStatus()));
        return vo;
    }

    // ============================ 详情 ============================

    public HighRiskResidentDetailVO detail(Long doctorId, Long residentId) {
        // 先判存在（404），再判范围（403），与慢病档案接口口径一致
        HealthProfile profile = healthProfileService.getByIdRequired(residentId);
        if (!doctorResidentService.isResidentInDoctorScope(doctorId, profile.getId())) {
            throw new BusinessException(403, "该居民不在您的管理范围内");
        }
        return buildDetail(profile);
    }

    public HighRiskResidentDetailVO detailAll(Long residentId) {
        return buildDetail(healthProfileService.getByIdRequired(residentId));
    }

    private HighRiskResidentDetailVO buildDetail(HealthProfile profile) {
        Long residentId = profile.getId();
        User residentUser = healthProfileService.loadUser(profile.getUserId());

        HighRiskResidentDetailVO detail = new HighRiskResidentDetailVO();
        detail.setResident(healthProfileService.toVO(profile, residentUser));

        // 最近一次风险评估
        RiskAssessment latest = riskAssessmentService.lambdaQuery()
                .eq(RiskAssessment::getResidentId, residentId)
                .orderByDesc(RiskAssessment::getAssessedAt)
                .orderByDesc(RiskAssessment::getId)
                .last("LIMIT 1")
                .one();
        if (latest != null) {
            detail.setLatestRiskLevel(latest.getRiskLevel());
            detail.setLatestRiskLevelLabel(RiskAssessmentService.labelOfRiskLevel(latest.getRiskLevel()));
            detail.setLatestRiskAssessedAt(latest.getAssessedAt());
            HealthIndicatorRecord record = indicatorRecordMapper.selectById(latest.getIndicatorRecordId());
            AlertRecord alert = alertRecordMapper.selectOne(new LambdaQueryWrapper<AlertRecord>()
                    .eq(AlertRecord::getAssessmentId, latest.getId()).last("LIMIT 1"));
            detail.setLatestAssessment(riskAssessmentService.toVO(latest, record, alert));
        }

        // 慢病档案（含资料状态，便于区分已核实/待核实）
        detail.setChronicDiseases(chronicDiseaseService.listByResident(residentId));

        // 预警：未处理数 + 最近若干条
        detail.setUnhandledAlertCount(countAlerts(residentId, false));
        detail.setTotalAlertCount(countAlerts(residentId, true));
        List<AlertRecord> alerts = alertRecordMapper.selectList(new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getResidentId, residentId)
                .orderByDesc(AlertRecord::getCreateTime)
                .orderByDesc(AlertRecord::getId)
                .last("LIMIT " + DETAIL_ALERT_LIMIT));
        detail.setAlerts(alertService.toVOList(alerts));

        // 最近指标
        List<HealthIndicatorRecord> indicators = indicatorRecordMapper.selectList(
                new LambdaQueryWrapper<HealthIndicatorRecord>()
                        .eq(HealthIndicatorRecord::getResidentId, residentId)
                        .orderByDesc(HealthIndicatorRecord::getMeasuredAt)
                        .orderByDesc(HealthIndicatorRecord::getId)
                        .last("LIMIT " + DETAIL_INDICATOR_LIMIT));
        detail.setRecentIndicators(indicatorAssembler.toVOList(indicators));

        detail.setLastMeasuredAt(indicators.isEmpty() ? null : indicators.get(0).getMeasuredAt());

        FollowupRecord lastFollowup = followupRecordMapper.selectOne(new QueryWrapper<FollowupRecord>()
                .eq("resident_id", residentId)
                .orderByDesc("followed_up_at").orderByDesc("id").last("LIMIT 1"));
        detail.setLastFollowupAt(lastFollowup == null ? null : lastFollowup.getFollowedUpAt());

        InterventionRecord lastIntervention = interventionRecordMapper.selectOne(new QueryWrapper<InterventionRecord>()
                .eq("resident_id", residentId)
                .orderByDesc("executed_at").orderByDesc("id").last("LIMIT 1"));
        detail.setLastInterventionAt(lastIntervention == null ? null : lastIntervention.getExecutedAt());

        return detail;
    }

    private Long countAlerts(Long residentId, boolean all) {
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getResidentId, residentId);
        if (!all) {
            wrapper.in(AlertRecord::getStatus, AlertStatus.PENDING.name(), AlertStatus.PROCESSING.name());
        }
        return alertRecordMapper.selectCount(wrapper);
    }

    // ============================ 工具 ============================

    private void validateEnums(HighRiskResidentQueryRequest query) {
        if (StringUtils.hasText(query.getRiskLevel())
                && RiskLevel.fromCode(query.getRiskLevel()).isEmpty()) {
            throw new BusinessException(400, "风险等级取值不正确");
        }
        if (StringUtils.hasText(query.getAlertStatus())
                && AlertStatus.fromCode(query.getAlertStatus()).isEmpty()) {
            throw new BusinessException(400, "预警状态取值不正确");
        }
        if (StringUtils.hasText(query.getDiseaseType())
                && DiseaseType.fromCode(query.getDiseaseType()).isEmpty()) {
            throw new BusinessException(400, "慢病类型取值不正确");
        }
    }

    private Integer ageOf(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        int s = size == null ? 10 : size;
        return s < 1 || s > 100 ? 10 : s;
    }
}
