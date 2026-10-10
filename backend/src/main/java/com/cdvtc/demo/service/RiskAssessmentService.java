package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.entity.AlertRecord;
import com.cdvtc.demo.entity.HealthIndicatorRecord;
import com.cdvtc.demo.entity.RiskAssessment;
import com.cdvtc.demo.entity.RiskRule;
import com.cdvtc.demo.enums.IndicatorType;
import com.cdvtc.demo.enums.MeasurementContext;
import com.cdvtc.demo.enums.RiskLevel;
import com.cdvtc.demo.mapper.RiskAssessmentMapper;
import com.cdvtc.demo.vo.AssessmentVO;
import com.cdvtc.demo.vo.MatchedRuleVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 风险评估：后端规则驱动，规则命中结果可解释（返回命中的规则、阈值与原因）。
 * 不做疾病诊断，不预测发病概率。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskAssessmentService extends ServiceImpl<RiskAssessmentMapper, RiskAssessment> {

    private static final DateTimeFormatter CODE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String DEFAULT_RULE_VERSION = "v1";

    private final RiskRuleService riskRuleService;
    private final ObjectMapper objectMapper;

    /** 评估结论（未持久化）。 */
    @Getter
    public static class Outcome {
        private final RiskLevel riskLevel;
        private final List<MatchedRuleVO> matchedRules;
        private final String summary;
        private final String ruleVersion;
        private final String triggerDetailsJson;

        Outcome(RiskLevel riskLevel, List<MatchedRuleVO> matchedRules, String summary,
                String ruleVersion, String triggerDetailsJson) {
            this.riskLevel = riskLevel;
            this.matchedRules = matchedRules;
            this.summary = summary;
            this.ruleVersion = ruleVersion;
            this.triggerDetailsJson = triggerDetailsJson;
        }
    }

    public Outcome evaluate(HealthIndicatorRecord record) {
        IndicatorType type = IndicatorType.fromCode(record.getIndicatorType())
                .orElseThrow(() -> new BusinessException(400, "不支持的指标类型: " + record.getIndicatorType()));
        MeasurementContext context = MeasurementContext.fromCode(record.getMeasurementContext()).orElse(null);

        // 规则有效期按"评估时刻"判断：使用当前生效的规则版本评估，历史评估保留当时的快照
        List<RiskRule> rules = riskRuleService.listActiveRules(type, context, LocalDateTime.now());

        List<MatchedRuleVO> matched = new ArrayList<>();
        for (RiskRule rule : rules) {
            BigDecimal actual = resolveActualValue(rule, record);
            if (actual == null) {
                continue;
            }
            if (compare(actual, rule.getOperator(), rule.getThresholdValue())) {
                matched.add(toMatchedRuleVO(rule, actual, type));
            }
        }

        RiskLevel riskLevel = matched.stream()
                .map(m -> RiskLevel.fromCode(m.getRiskLevel()).orElse(RiskLevel.LOW))
                .reduce(RiskLevel.LOW, RiskLevel::max);

        MatchedRuleVO topRule = matched.stream()
                .max((a, b) -> Integer.compare(
                        RiskLevel.fromCode(a.getRiskLevel()).orElse(RiskLevel.LOW).getPriority(),
                        RiskLevel.fromCode(b.getRiskLevel()).orElse(RiskLevel.LOW).getPriority()))
                .orElse(null);

        String ruleVersion = topRule != null ? topRule.getRuleVersion()
                : rules.stream().findFirst().map(RiskRule::getRuleVersion).orElse(DEFAULT_RULE_VERSION);

        String summary = buildSummary(type, record, matched, topRule, rules.isEmpty(), riskLevel);

        String triggerJson;
        try {
            triggerJson = objectMapper.writeValueAsString(matched);
        } catch (Exception e) {
            log.error("序列化触发规则快照失败", e);
            throw new BusinessException(500, "风险评估失败，请稍后重试");
        }

        return new Outcome(riskLevel, matched, summary, ruleVersion, triggerJson);
    }

    public RiskAssessment persist(HealthIndicatorRecord record, Outcome outcome) {
        RiskAssessment assessment = new RiskAssessment();
        assessment.setResidentId(record.getResidentId());
        assessment.setIndicatorRecordId(record.getId());
        assessment.setAssessmentCode(generateCode("RA"));
        assessment.setRiskLevel(outcome.getRiskLevel().name());
        assessment.setAssessmentSummary(outcome.getSummary());
        assessment.setTriggerDetails(outcome.getTriggerDetailsJson());
        assessment.setRuleVersion(outcome.getRuleVersion());
        assessment.setAssessedAt(LocalDateTime.now());
        save(assessment);
        return assessment;
    }

    public RiskAssessment findByIndicatorRecordId(Long indicatorRecordId) {
        return lambdaQuery()
                .eq(RiskAssessment::getIndicatorRecordId, indicatorRecordId)
                .one();
    }

    public AssessmentVO toVO(RiskAssessment assessment, HealthIndicatorRecord record, AlertRecord alert) {
        AssessmentVO vo = new AssessmentVO();
        vo.setId(assessment.getId());
        vo.setAssessmentCode(assessment.getAssessmentCode());
        vo.setIndicatorRecordId(assessment.getIndicatorRecordId());
        vo.setResidentId(assessment.getResidentId());
        vo.setRiskLevel(assessment.getRiskLevel());
        vo.setRiskLevelLabel(labelOfRiskLevel(assessment.getRiskLevel()));
        vo.setAssessmentSummary(assessment.getAssessmentSummary());
        vo.setRuleVersion(assessment.getRuleVersion());
        vo.setAssessedAt(assessment.getAssessedAt());
        vo.setMatchedRules(parseTriggerDetails(assessment.getTriggerDetails()));

        if (record != null) {
            IndicatorType type = IndicatorType.fromCode(record.getIndicatorType()).orElse(null);
            vo.setIndicatorType(record.getIndicatorType());
            vo.setIndicatorTypeLabel(type != null ? type.getLabel() : record.getIndicatorType());
        }
        if (alert != null) {
            vo.setAlertId(alert.getId());
            vo.setAlertNo(alert.getAlertNo());
            vo.setAlertStatus(alert.getStatus());
        }
        return vo;
    }

    public List<MatchedRuleVO> parseTriggerDetails(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<MatchedRuleVO>>() {
            });
        } catch (Exception e) {
            log.warn("解析触发规则快照失败: {}", json);
            return new ArrayList<>();
        }
    }

    public static String labelOfRiskLevel(String code) {
        return RiskLevel.fromCode(code).map(RiskLevel::getLabel).orElse("未知");
    }

    private BigDecimal resolveActualValue(RiskRule rule, HealthIndicatorRecord record) {
        if ("SECONDARY".equalsIgnoreCase(rule.getValueField())) {
            return record.getSecondaryValue();
        }
        return record.getNumericValue();
    }

    private boolean compare(BigDecimal actual, String operator, BigDecimal threshold) {
        int cmp = actual.compareTo(threshold);
        return switch (operator) {
            case ">=" -> cmp >= 0;
            case ">" -> cmp > 0;
            case "<=" -> cmp <= 0;
            case "<" -> cmp < 0;
            case "=", "==" -> cmp == 0;
            default -> throw new BusinessException(500, "不支持的规则操作符: " + operator);
        };
    }

    private MatchedRuleVO toMatchedRuleVO(RiskRule rule, BigDecimal actual, IndicatorType type) {
        MatchedRuleVO vo = new MatchedRuleVO();
        vo.setRuleCode(rule.getRuleCode());
        vo.setRuleName(rule.getRuleName());
        vo.setValueField(rule.getValueField());
        vo.setValueFieldLabel(valueFieldLabel(type, rule.getValueField()));
        vo.setOperator(rule.getOperator());
        vo.setThresholdValue(rule.getThresholdValue());
        vo.setActualValue(actual);
        vo.setRiskLevel(rule.getRiskLevel());
        vo.setRiskLevelLabel(labelOfRiskLevel(rule.getRiskLevel()));
        vo.setMeasurementContext(rule.getMeasurementContext());
        vo.setDescription(rule.getDescription());
        vo.setRuleVersion(rule.getRuleVersion());
        vo.setAlertType(rule.getAlertType());
        return vo;
    }

    private String valueFieldLabel(IndicatorType type, String valueField) {
        if (type == IndicatorType.BLOOD_PRESSURE) {
            return "SECONDARY".equalsIgnoreCase(valueField) ? "舒张压" : "收缩压";
        }
        return "测量值";
    }

    private String buildSummary(IndicatorType type, HealthIndicatorRecord record,
                                List<MatchedRuleVO> matched, MatchedRuleVO topRule,
                                boolean noRuleConfigured, RiskLevel riskLevel) {
        if (matched.isEmpty()) {
            if (noRuleConfigured) {
                return type.getLabel() + "暂无可用的评估规则，本次不做风险判断、不产生预警；"
                        + "（如体重需结合身高计算 BMI 等上下文才能评估，请补充资料或咨询医生）";
            }
            return "本次" + type.getLabel() + "测量未触发预警阈值，未产生预警。"
                    + "未触发预警不等于完全健康，请继续保持定期测量。";
        }
        return "本次" + type.getLabel() + "测量命中 " + matched.size() + " 条规则，最高风险等级为"
                + riskLevel.getLabel() + "。主要依据：" + topRule.getRuleName()
                + "（" + topRule.getValueFieldLabel() + " " + stripTrailingZeros(topRule.getActualValue())
                + " " + record.getUnit() + " " + topRule.getOperator() + " "
                + stripTrailingZeros(topRule.getThresholdValue()) + " " + record.getUnit() + "）。"
                + "本提示基于规则阈值，需由医生结合病史判断，不构成疾病诊断。";
    }

    private String stripTrailingZeros(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String generateCode(String prefix) {
        return prefix + LocalDateTime.now().format(CODE_FMT)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
