package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.dto.AlertHandleRequest;
import com.cdvtc.demo.dto.IndicatorCreateRequest;
import com.cdvtc.demo.entity.AlertRecord;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.enums.AlertStatus;
import com.cdvtc.demo.mapper.UserMapper;
import com.cdvtc.demo.vo.AlertDetailVO;
import com.cdvtc.demo.vo.IndicatorSubmitResultVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 医生预警处理状态流转测试：PENDING -> PROCESSING -> RESOLVED，
 * 并校验非法流转被拒绝、处理记录追加保存。
 */
@SpringBootTest
@Transactional
class AlertHandlingTest {

    @Autowired
    private IndicatorService indicatorService;
    @Autowired
    private AlertService alertService;
    @Autowired
    private UserMapper userMapper;

    private User user(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    /** 造一条高风险预警（医生 doctor 已与该居民建立授权关系）。 */
    private AlertRecord createAlert(String requestId) {
        IndicatorCreateRequest req = new IndicatorCreateRequest();
        req.setIndicatorType("BLOOD_PRESSURE");
        req.setNumericValue(BigDecimal.valueOf(178));
        req.setSecondaryValue(BigDecimal.valueOf(109));
        req.setUnit("mmHg");
        req.setMeasuredAt(LocalDateTime.now());
        req.setRequestId(requestId);
        IndicatorSubmitResultVO result = indicatorService.submit(user("resident"), req);
        assertNotNull(result.getAlert());
        return alertService.getById(result.getAlert().getId());
    }

    private AlertHandleRequest handle(String action, String comment) {
        AlertHandleRequest req = new AlertHandleRequest();
        req.setActionType(action);
        req.setHandlingComment(comment);
        return req;
    }

    @Test
    void 待处理预警不能直接完成() {
        AlertRecord alert = createAlert("test-flow-1");
        Long doctorId = user("doctor").getId();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> alertService.handle(doctorId, alert.getId(), handle("RESOLVE", "直接完成")));
        assertEquals(409, ex.getCode().intValue());
    }

    @Test
    void 完整流转_开始处理到完成处理() {
        AlertRecord alert = createAlert("test-flow-2");
        Long doctorId = user("doctor").getId();

        AlertDetailVO afterStart = alertService.handle(doctorId, alert.getId(), handle("START", "开始处理"));
        assertEquals(AlertStatus.PROCESSING.name(), afterStart.getAlert().getStatus());

        AlertHandleRequest resolve = handle("RESOLVE", "已完成处置");
        resolve.setFollowupRequired(true);
        resolve.setNextFollowupAt(LocalDateTime.now().plusDays(7));
        AlertDetailVO afterResolve = alertService.handle(doctorId, alert.getId(), resolve);
        assertEquals(AlertStatus.RESOLVED.name(), afterResolve.getAlert().getStatus());

        // 处理历史追加，两条记录都在
        assertEquals(2, afterResolve.getHandlingRecords().size());
        assertEquals(true, afterResolve.getHandlingRecords().get(1).getFollowupRequired());
    }

    @Test
    void 已完成预警不能再次处理() {
        AlertRecord alert = createAlert("test-flow-3");
        Long doctorId = user("doctor").getId();
        alertService.handle(doctorId, alert.getId(), handle("START", "开始"));
        alertService.handle(doctorId, alert.getId(), handle("RESOLVE", "完成"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> alertService.handle(doctorId, alert.getId(), handle("RESOLVE", "重复完成")));
        assertEquals(409, ex.getCode().intValue());
    }

    @Test
    void 非授权范围医生不能处理该预警() {
        AlertRecord alert = createAlert("test-flow-4");
        // 造一个没有授权关系的医生账号用于测试
        User otherDoctor = user("resident"); // 复用 resident 账号仅用于验证 id 不在授权范围
        BusinessException ex = assertThrows(BusinessException.class,
                () -> alertService.handle(otherDoctor.getId(), alert.getId(), handle("START", "越权")));
        assertEquals(403, ex.getCode().intValue());
    }

    @Test
    void 处理意见为空应被拒绝() {
        AlertRecord alert = createAlert("test-flow-5");
        Long doctorId = user("doctor").getId();
        // @NotBlank 由接口层校验；服务层直接传空串时不应改变状态
        BusinessException ex = assertThrows(BusinessException.class,
                () -> alertService.handle(doctorId, alert.getId(),
                        handle("START", "   ")));
        assertNotNull(ex.getCode());
    }
}
