package com.cdvtc.demo.service;

import com.cdvtc.demo.entity.HealthIndicatorRecord;
import com.cdvtc.demo.enums.RiskLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 风险评估规则测试。
 * 阈值取自 02_seed_risk_rule.sql（引用公开临床指南切点），
 * 本测试仅验证"程序能否正确按阈值判定"，不验证医学结论。
 */
@SpringBootTest
class RiskAssessmentServiceTest {

    @Autowired
    private RiskAssessmentService riskAssessmentService;

    private HealthIndicatorRecord bp(double systolic, double diastolic) {
        HealthIndicatorRecord r = new HealthIndicatorRecord();
        r.setIndicatorType("BLOOD_PRESSURE");
        r.setNumericValue(BigDecimal.valueOf(systolic));
        r.setSecondaryValue(BigDecimal.valueOf(diastolic));
        r.setUnit("mmHg");
        r.setMeasuredAt(LocalDateTime.now());
        return r;
    }

    private HealthIndicatorRecord glucose(double value, String context) {
        HealthIndicatorRecord r = new HealthIndicatorRecord();
        r.setIndicatorType("BLOOD_GLUCOSE");
        r.setNumericValue(BigDecimal.valueOf(value));
        r.setUnit("mmol/L");
        r.setMeasurementContext(context);
        r.setMeasuredAt(LocalDateTime.now());
        return r;
    }

    private HealthIndicatorRecord single(String type, double value, String unit) {
        HealthIndicatorRecord r = new HealthIndicatorRecord();
        r.setIndicatorType(type);
        r.setNumericValue(BigDecimal.valueOf(value));
        r.setUnit(unit);
        r.setMeasuredAt(LocalDateTime.now());
        return r;
    }

    @Test
    void 正常血压应为低风险且不触发预警() {
        RiskAssessmentService.Outcome outcome = riskAssessmentService.evaluate(bp(120, 80));
        assertEquals(RiskLevel.LOW, outcome.getRiskLevel());
        assertEquals(false, outcome.getRiskLevel().shouldAlert());
    }

    @Test
    void 血压165_105应为高风险() {
        RiskAssessmentService.Outcome outcome = riskAssessmentService.evaluate(bp(165, 105));
        assertEquals(RiskLevel.HIGH, outcome.getRiskLevel());
        assertEquals(true, outcome.getRiskLevel().shouldAlert());
    }

    @Test
    void 血压185_115应为严重风险() {
        assertEquals(RiskLevel.CRITICAL, riskAssessmentService.evaluate(bp(185, 115)).getRiskLevel());
    }

    @Test
    void 收缩压正常但舒张压偏高应按舒张压规则判为中风险() {
        // 120/95：收缩压正常，舒张压达到 1 级高血压切点
        assertEquals(RiskLevel.MEDIUM, riskAssessmentService.evaluate(bp(120, 95)).getRiskLevel());
    }

    @Test
    void 空腹血糖7_2应为高风险() {
        assertEquals(RiskLevel.HIGH,
                riskAssessmentService.evaluate(glucose(7.2, "FASTING")).getRiskLevel());
    }

    @Test
    void 空腹血糖6_3应为中风险() {
        assertEquals(RiskLevel.MEDIUM,
                riskAssessmentService.evaluate(glucose(6.3, "FASTING")).getRiskLevel());
    }

    @Test
    void 餐后血糖8_0应为中风险() {
        assertEquals(RiskLevel.MEDIUM,
                riskAssessmentService.evaluate(glucose(8.0, "POSTPRANDIAL")).getRiskLevel());
    }

    @Test
    void 血糖场景不同阈值应区分_空腹6_3中风险而餐后6_3无风险() {
        // 6.3 在空腹属空腹血糖受损，在餐后属正常范围
        assertEquals(RiskLevel.MEDIUM,
                riskAssessmentService.evaluate(glucose(6.3, "FASTING")).getRiskLevel());
        assertEquals(RiskLevel.LOW,
                riskAssessmentService.evaluate(glucose(6.3, "POSTPRANDIAL")).getRiskLevel());
    }

    @Test
    void 体重缺上下文应无可判断并不产生预警() {
        RiskAssessmentService.Outcome outcome = riskAssessmentService.evaluate(single("WEIGHT", 70, "kg"));
        assertEquals(RiskLevel.LOW, outcome.getRiskLevel());
        assertEquals(0, outcome.getMatchedRules().size());
    }

    @Test
    void 心率130应为高风险_心率45应为中风险() {
        assertEquals(RiskLevel.HIGH,
                riskAssessmentService.evaluate(single("HEART_RATE", 130, "bpm")).getRiskLevel());
        assertEquals(RiskLevel.MEDIUM,
                riskAssessmentService.evaluate(single("HEART_RATE", 45, "bpm")).getRiskLevel());
    }

    @Test
    void 评估结论应包含可解释的命中规则() {
        RiskAssessmentService.Outcome outcome = riskAssessmentService.evaluate(bp(165, 105));
        // 165/105 同时命中收缩压与舒张压的多条规则
        assertEquals(true, outcome.getMatchedRules().size() >= 4);
        assertEquals(true, outcome.getSummary().contains("收缩压"));
        assertEquals(false, outcome.getMatchedRules().isEmpty());
    }
}
