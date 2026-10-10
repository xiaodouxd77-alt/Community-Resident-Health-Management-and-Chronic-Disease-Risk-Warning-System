package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.dto.IndicatorCreateRequest;
import com.cdvtc.demo.dto.IndicatorQueryRequest;
import com.cdvtc.demo.entity.AlertRecord;
import com.cdvtc.demo.entity.HealthIndicatorRecord;
import com.cdvtc.demo.entity.HealthProfile;
import com.cdvtc.demo.entity.RiskAssessment;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.enums.IndicatorType;
import com.cdvtc.demo.enums.MeasurementContext;
import com.cdvtc.demo.enums.RiskLevel;
import com.cdvtc.demo.mapper.AlertRecordMapper;
import com.cdvtc.demo.mapper.HealthIndicatorRecordMapper;
import com.cdvtc.demo.vo.AssessmentVO;
import com.cdvtc.demo.vo.IndicatorRecordVO;
import com.cdvtc.demo.vo.IndicatorSubmitResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 居民健康指标录入与查询。
 *
 * 数据校验策略（避免把开发便利当作业务标准）：
 *   - 格式错误 / 明显不合理的数值 -> 拒绝保存，提示用户检查后重新输入；
 *   - 数值合理但临床异常（超出阈值规则）-> 正常保存并进入风险评估、生成预警；
 *   - 校验区间为"可接受的测量区间"，不是疾病诊断阈值。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorService extends ServiceImpl<HealthIndicatorRecordMapper, HealthIndicatorRecord> {

    // 可接受的测量区间（用于过滤明显错误的数据，非临床诊断阈值）
    private static final BigDecimal BP_SYSTOLIC_MIN = new BigDecimal("40");
    private static final BigDecimal BP_SYSTOLIC_MAX = new BigDecimal("300");
    private static final BigDecimal BP_DIASTOLIC_MIN = new BigDecimal("20");
    private static final BigDecimal BP_DIASTOLIC_MAX = new BigDecimal("200");
    private static final BigDecimal GLUCOSE_MIN = new BigDecimal("0.5");
    private static final BigDecimal GLUCOSE_MAX = new BigDecimal("50");
    private static final BigDecimal WEIGHT_MIN = new BigDecimal("1");
    private static final BigDecimal WEIGHT_MAX = new BigDecimal("500");
    private static final BigDecimal HEART_RATE_MIN = new BigDecimal("20");
    private static final BigDecimal HEART_RATE_MAX = new BigDecimal("300");

    private final HealthProfileService healthProfileService;
    private final RiskAssessmentService riskAssessmentService;
    private final AlertService alertService;
    private final IndicatorAssembler indicatorAssembler;
    private final AlertRecordMapper alertRecordMapper;

    // ============================ 录入 ============================

    /**
     * 居民录入健康指标：保存记录 -> 风险评估 -> 生成预警，全部在同一事务内完成。
     * 任一环节失败则整体回滚，不会出现"提示成功但数据库缺失评估"的情况。
     */
    @Transactional
    public IndicatorSubmitResultVO submit(User user, IndicatorCreateRequest request) {
        HealthProfile profile = healthProfileService.getOrCreate(user);

        IndicatorType type = IndicatorType.fromCode(request.getIndicatorType())
                .orElseThrow(() -> new BusinessException(400, "不支持的指标类型: " + request.getIndicatorType()));
        validate(type, request);

        // 幂等：同一 requestId 重复提交，直接返回既有结果，不重复评估/预警
        HealthIndicatorRecord duplicated = findByRequestId(request.getRequestId());
        if (duplicated != null) {
            return assembleResult(duplicated, true);
        }

        HealthIndicatorRecord record = new HealthIndicatorRecord();
        record.setResidentId(profile.getId());
        record.setIndicatorType(type.name());
        record.setNumericValue(request.getNumericValue());
        record.setSecondaryValue(type.isRequiresSecondaryValue() ? request.getSecondaryValue() : null);
        record.setUnit(type.getDefaultUnit());
        record.setMeasuredAt(request.getMeasuredAt());
        record.setMeasurementContext(type.isMeasurementContextRequired()
                ? request.getMeasurementContext().trim().toUpperCase() : null);
        record.setSource("MANUAL");
        record.setRemark(request.getRemark());
        record.setRequestId(request.getRequestId());

        try {
            save(record);
        } catch (DuplicateKeyException e) {
            // 并发重复提交：唯一约束兜底，返回既有结果
            HealthIndicatorRecord existing = findByRequestId(request.getRequestId());
            if (existing == null) {
                throw new BusinessException(500, "指标保存失败，请稍后重试");
            }
            return assembleResult(existing, true);
        }

        RiskAssessmentService.Outcome outcome = riskAssessmentService.evaluate(record);
        RiskAssessment assessment = riskAssessmentService.persist(record, outcome);
        AlertRecord alert = alertService.createIfNeeded(record, assessment, outcome);

        log.info("居民指标录入完成 residentId={}, recordId={}, riskLevel={}, alertId={}",
                profile.getId(), record.getId(), outcome.getRiskLevel(), alert == null ? null : alert.getId());

        return assembleResult(record, assessment, alert, false);
    }

    private void validate(IndicatorType type, IndicatorCreateRequest request) {
        if (!type.acceptsUnit(request.getUnit())) {
            throw new BusinessException(400, "「" + type.getLabel() + "」的单位应为 " + type.getDefaultUnit());
        }
        LocalDateTime measuredAt = request.getMeasuredAt();
        if (measuredAt.isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new BusinessException(400, "测量时间不能晚于当前时间");
        }
        if (measuredAt.isBefore(LocalDateTime.of(1900, 1, 1, 0, 0))) {
            throw new BusinessException(400, "测量时间不合理，请重新填写");
        }

        BigDecimal primary = request.getNumericValue();
        switch (type) {
            case BLOOD_PRESSURE -> {
                requirePlausible(primary, BP_SYSTOLIC_MIN, BP_SYSTOLIC_MAX, "收缩压(mmHg)");
                BigDecimal diastolic = request.getSecondaryValue();
                if (diastolic == null) {
                    throw new BusinessException(400, "血压需同时填写收缩压和舒张压");
                }
                requirePlausible(diastolic, BP_DIASTOLIC_MIN, BP_DIASTOLIC_MAX, "舒张压(mmHg)");
                if (primary.compareTo(diastolic) <= 0) {
                    throw new BusinessException(400, "收缩压应大于舒张压，请检查输入");
                }
            }
            case BLOOD_GLUCOSE -> {
                if (MeasurementContext.fromCode(request.getMeasurementContext()).isEmpty()) {
                    throw new BusinessException(400, "血糖需选择测量场景（空腹/餐后2小时/随机）");
                }
                if (request.getSecondaryValue() != null) {
                    throw new BusinessException(400, "血糖指标不需要填写第二数值");
                }
                requirePlausible(primary, GLUCOSE_MIN, GLUCOSE_MAX, "血糖(mmol/L)");
            }
            case WEIGHT -> requirePlausible(primary, WEIGHT_MIN, WEIGHT_MAX, "体重(kg)");
            case HEART_RATE -> requirePlausible(primary, HEART_RATE_MIN, HEART_RATE_MAX, "心率(bpm)");
        }
    }

    private void requirePlausible(BigDecimal value, BigDecimal min, BigDecimal max, String label) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new BusinessException(400, label + "数值超出可接受范围（"
                    + min.toPlainString() + "~" + max.toPlainString() + "），请检查后重新输入");
        }
    }

    private HealthIndicatorRecord findByRequestId(String requestId) {
        return lambdaQuery().eq(HealthIndicatorRecord::getRequestId, requestId).one();
    }

    private IndicatorSubmitResultVO assembleResult(HealthIndicatorRecord record, boolean idempotent) {
        RiskAssessment assessment = riskAssessmentService.findByIndicatorRecordId(record.getId());
        AlertRecord alert = assessment == null ? null : alertRecordMapper.selectOne(
                new LambdaQueryWrapper<AlertRecord>().eq(AlertRecord::getAssessmentId, assessment.getId()));
        return assembleResult(record, assessment, alert, idempotent);
    }

    private IndicatorSubmitResultVO assembleResult(HealthIndicatorRecord record, RiskAssessment assessment,
                                                   AlertRecord alert, boolean idempotent) {
        IndicatorSubmitResultVO result = new IndicatorSubmitResultVO();
        result.setRecordSaved(true);
        result.setAssessmentCompleted(assessment != null);
        result.setAlertCreated(alert != null);
        result.setRecord(indicatorAssembler.toVO(record, assessment, alert));
        result.setAssessment(assessment == null ? null
                : riskAssessmentService.toVO(assessment, record, alert));
        result.setAlert(alert == null ? null : alertService.toVOList(List.of(alert)).get(0));

        String riskLabel = assessment == null ? null
                : RiskLevel.fromCode(assessment.getRiskLevel()).map(RiskLevel::getLabel).orElse("");
        String prefix = idempotent ? "该指标已提交过，返回原结果：" : "";
        if (alert != null) {
            result.setMessage(prefix + "指标已保存，本次评估为" + riskLabel + "，已生成预警，请在“我的预警”中查看。");
        } else {
            result.setMessage(prefix + "指标已保存，本次评估为" + riskLabel + "，未触发预警。");
        }
        return result;
    }

    // ============================ 查询 ============================

    public IPage<IndicatorRecordVO> pageResidentIndicators(Long residentId, IndicatorQueryRequest query) {
        LambdaQueryWrapper<HealthIndicatorRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthIndicatorRecord::getResidentId, residentId);
        if (StringUtils.hasText(query.getIndicatorType())) {
            IndicatorType type = IndicatorType.fromCode(query.getIndicatorType())
                    .orElseThrow(() -> new BusinessException(400, "不支持的指标类型: " + query.getIndicatorType()));
            wrapper.eq(HealthIndicatorRecord::getIndicatorType, type.name());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(HealthIndicatorRecord::getMeasuredAt, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(HealthIndicatorRecord::getMeasuredAt, query.getEndTime());
        }
        wrapper.orderByDesc(HealthIndicatorRecord::getMeasuredAt)
                .orderByDesc(HealthIndicatorRecord::getId);

        IPage<HealthIndicatorRecord> recordPage = page(new Page<>(normalizePage(query), normalizeSize(query)), wrapper);
        Page<IndicatorRecordVO> voPage = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        voPage.setRecords(indicatorAssembler.toVOList(recordPage.getRecords()));
        return voPage;
    }

    /** 居民查看自己的单条指标详情，非本人资源返回 404。 */
    public IndicatorRecordVO getResidentIndicator(Long residentId, Long id) {
        HealthIndicatorRecord record = getById(id);
        if (record == null || !Objects.equals(record.getResidentId(), residentId)) {
            throw new BusinessException(404, "指标记录不存在");
        }
        List<IndicatorRecordVO> list = indicatorAssembler.toVOList(List.of(record));
        return list.get(0);
    }

    public IPage<AssessmentVO> pageResidentAssessments(Long residentId, Integer page, Integer size) {
        LambdaQueryWrapper<RiskAssessment> wrapper = new LambdaQueryWrapper<RiskAssessment>()
                .eq(RiskAssessment::getResidentId, residentId)
                .orderByDesc(RiskAssessment::getAssessedAt)
                .orderByDesc(RiskAssessment::getId);
        IPage<RiskAssessment> assessmentPage =
                riskAssessmentService.page(new Page<>(norm(page), norm(size)), wrapper);

        List<RiskAssessment> assessments = assessmentPage.getRecords();
        Map<Long, HealthIndicatorRecord> records = assessments.isEmpty() ? Collections.emptyMap()
                : listByIds(assessments.stream().map(RiskAssessment::getIndicatorRecordId).distinct().toList())
                .stream().collect(Collectors.toMap(HealthIndicatorRecord::getId, Function.identity(), (a, b) -> a));
        List<Long> assessmentIds = assessments.stream().map(RiskAssessment::getId).toList();
        Map<Long, AlertRecord> alerts = assessmentIds.isEmpty() ? Collections.emptyMap()
                : alertRecordMapper.selectList(new LambdaQueryWrapper<AlertRecord>()
                        .in(AlertRecord::getAssessmentId, assessmentIds))
                .stream().collect(Collectors.toMap(AlertRecord::getAssessmentId, Function.identity(), (a, b) -> a));

        List<AssessmentVO> voList = assessments.stream()
                .map(a -> riskAssessmentService.toVO(a, records.get(a.getIndicatorRecordId()),
                        alerts.get(a.getId())))
                .toList();

        Page<AssessmentVO> voPage = new Page<>(assessmentPage.getCurrent(), assessmentPage.getSize(),
                assessmentPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    private int normalizePage(IndicatorQueryRequest query) {
        return query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
    }

    private int normalizeSize(IndicatorQueryRequest query) {
        int size = query.getSize() == null ? 10 : query.getSize();
        return size < 1 || size > 100 ? 10 : size;
    }

    private int norm(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }
}
