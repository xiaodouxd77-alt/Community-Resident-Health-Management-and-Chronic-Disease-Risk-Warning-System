package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cdvtc.demo.entity.AlertRecord;
import com.cdvtc.demo.entity.HealthIndicatorRecord;
import com.cdvtc.demo.entity.RiskAssessment;
import com.cdvtc.demo.enums.IndicatorType;
import com.cdvtc.demo.enums.MeasurementContext;
import com.cdvtc.demo.enums.RiskLevel;
import com.cdvtc.demo.mapper.AlertRecordMapper;
import com.cdvtc.demo.mapper.RiskAssessmentMapper;
import com.cdvtc.demo.vo.IndicatorRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 指标记录 -> VO 转换，并补充关联的评估、预警信息（批量查询避免 N+1）。
 * 由居民端与医生端共用。
 */
@Component
@RequiredArgsConstructor
public class IndicatorAssembler {

    private final RiskAssessmentMapper assessmentMapper;
    private final AlertRecordMapper alertRecordMapper;

    public List<IndicatorRecordVO> toVOList(List<HealthIndicatorRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> recordIds = records.stream().map(HealthIndicatorRecord::getId).toList();

        Map<Long, RiskAssessment> assessmentByRecord = assessmentMapper.selectList(
                        new LambdaQueryWrapper<RiskAssessment>()
                                .in(RiskAssessment::getIndicatorRecordId, recordIds))
                .stream()
                .collect(Collectors.toMap(RiskAssessment::getIndicatorRecordId, Function.identity(), (a, b) -> a));

        List<Long> assessmentIds = assessmentByRecord.values().stream()
                .map(RiskAssessment::getId).toList();
        Map<Long, AlertRecord> alertByAssessment = assessmentIds.isEmpty()
                ? Collections.emptyMap()
                : alertRecordMapper.selectList(
                        new LambdaQueryWrapper<AlertRecord>()
                                .in(AlertRecord::getAssessmentId, assessmentIds))
                .stream()
                .collect(Collectors.toMap(AlertRecord::getAssessmentId, Function.identity(), (a, b) -> a));

        return records.stream()
                .map(r -> {
                    RiskAssessment assessment = assessmentByRecord.get(r.getId());
                    AlertRecord alert = assessment == null ? null : alertByAssessment.get(assessment.getId());
                    return toVO(r, assessment, alert);
                })
                .toList();
    }

    public IndicatorRecordVO toVO(HealthIndicatorRecord record, RiskAssessment assessment, AlertRecord alert) {
        IndicatorRecordVO vo = new IndicatorRecordVO();
        vo.setId(record.getId());
        vo.setIndicatorType(record.getIndicatorType());
        vo.setIndicatorTypeLabel(IndicatorType.fromCode(record.getIndicatorType())
                .map(IndicatorType::getLabel).orElse(record.getIndicatorType()));
        vo.setNumericValue(record.getNumericValue());
        vo.setSecondaryValue(record.getSecondaryValue());
        vo.setUnit(record.getUnit());
        vo.setMeasuredAt(record.getMeasuredAt());
        vo.setMeasurementContext(record.getMeasurementContext());
        vo.setMeasurementContextLabel(MeasurementContext.fromCode(record.getMeasurementContext())
                .map(MeasurementContext::getLabel).orElse(null));
        vo.setSource(record.getSource());
        vo.setRemark(record.getRemark());
        vo.setCreateTime(record.getCreateTime());

        if (assessment != null) {
            vo.setAssessmentId(assessment.getId());
            vo.setAssessmentRiskLevel(assessment.getRiskLevel());
            vo.setAssessmentRiskLevelLabel(RiskLevel.fromCode(assessment.getRiskLevel())
                    .map(RiskLevel::getLabel).orElse(null));
        }
        if (alert != null) {
            vo.setAlertId(alert.getId());
            vo.setAlertStatus(alert.getStatus());
        }
        return vo;
    }
}
