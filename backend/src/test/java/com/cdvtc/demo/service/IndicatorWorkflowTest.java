package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.dto.IndicatorCreateRequest;
import com.cdvtc.demo.entity.AlertRecord;
import com.cdvtc.demo.entity.RiskAssessment;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.enums.AlertStatus;
import com.cdvtc.demo.mapper.AlertRecordMapper;
import com.cdvtc.demo.mapper.RiskAssessmentMapper;
import com.cdvtc.demo.mapper.UserMapper;
import com.cdvtc.demo.vo.IndicatorSubmitResultVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 指标录入闭环测试：保存 -> 评估 -> 预警，以及幂等与参数校验。
 * 使用真实 MySQL，测试事务内完成后回滚，不污染数据。
 */
@SpringBootTest
@Transactional
class IndicatorWorkflowTest {

    @Autowired
    private IndicatorService indicatorService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RiskAssessmentMapper riskAssessmentMapper;
    @Autowired
    private AlertRecordMapper alertRecordMapper;

    private User resident() {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "resident"));
    }

    private IndicatorCreateRequest bpRequest(String requestId, double systolic, double diastolic) {
        IndicatorCreateRequest req = new IndicatorCreateRequest();
        req.setIndicatorType("BLOOD_PRESSURE");
        req.setNumericValue(BigDecimal.valueOf(systolic));
        req.setSecondaryValue(BigDecimal.valueOf(diastolic));
        req.setUnit("mmHg");
        req.setMeasuredAt(LocalDateTime.now());
        req.setRequestId(requestId);
        return req;
    }

    @Test
    void 正常血压保存成功但不产生预警() {
        IndicatorSubmitResultVO result = indicatorService.submit(resident(), bpRequest("test-normal-1", 120, 80));
        assertTrue(result.getRecordSaved());
        assertTrue(result.getAssessmentCompleted());
        assertFalse(result.getAlertCreated());
        assertNull(result.getAlert());
    }

    @Test
    void 异常血压应生成评估与预警() {
        IndicatorSubmitResultVO result = indicatorService.submit(resident(), bpRequest("test-high-1", 165, 105));
        assertTrue(result.getAlertCreated());
        assertNotNull(result.getAlert());
        assertEquals("HIGH", result.getAlert().getRiskLevel());
        assertEquals(AlertStatus.PENDING.name(), result.getAlert().getStatus());
        // 评估与预警均已落库
        assertNotNull(riskAssessmentMapper.selectById(result.getAssessment().getId()));
        assertNotNull(alertRecordMapper.selectById(result.getAlert().getId()));
    }

    @Test
    void 重复提交同一请求应幂等_不重复评估和预警() {
        IndicatorCreateRequest req = bpRequest("test-idem-1", 170, 108);
        IndicatorSubmitResultVO first = indicatorService.submit(resident(), req);
        IndicatorSubmitResultVO second = indicatorService.submit(resident(), req);

        assertEquals(first.getRecord().getId(), second.getRecord().getId());
        assertEquals(first.getAlert().getId(), second.getAlert().getId());

        long assessmentCount = riskAssessmentMapper.selectCount(new LambdaQueryWrapper<RiskAssessment>()
                .eq(RiskAssessment::getIndicatorRecordId, first.getRecord().getId()));
        long alertCount = alertRecordMapper.selectCount(new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getIndicatorRecordId, first.getRecord().getId()));
        assertEquals(1, assessmentCount);
        assertEquals(1, alertCount);
    }

    @Test
    void 单位错误应被拒绝() {
        IndicatorCreateRequest req = bpRequest("test-bad-unit-1", 130, 85);
        req.setUnit("mmol/L");
        assertThrows(BusinessException.class, () -> indicatorService.submit(resident(), req));
    }

    @Test
    void 收缩压小于舒张压应被拒绝() {
        assertThrows(BusinessException.class,
                () -> indicatorService.submit(resident(), bpRequest("test-bad-order-1", 80, 120)));
    }

    @Test
    void 明显不合理的数值应被拒绝() {
        assertThrows(BusinessException.class,
                () -> indicatorService.submit(resident(), bpRequest("test-bad-range-1", 500, 100)));
    }

    @Test
    void 血糖缺少测量场景应被拒绝() {
        IndicatorCreateRequest req = new IndicatorCreateRequest();
        req.setIndicatorType("BLOOD_GLUCOSE");
        req.setNumericValue(BigDecimal.valueOf(6.0));
        req.setUnit("mmol/L");
        req.setMeasuredAt(LocalDateTime.now());
        req.setRequestId("test-bad-ctx-1");
        assertThrows(BusinessException.class, () -> indicatorService.submit(resident(), req));
    }
}
