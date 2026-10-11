package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.dto.ConsultationCreateRequest;
import com.cdvtc.demo.dto.ConsultationMessageRequest;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.enums.ConsultationStatus;
import com.cdvtc.demo.mapper.UserMapper;
import com.cdvtc.demo.vo.ConsultationDetailVO;
import com.cdvtc.demo.vo.ConsultationVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 在线咨询状态流转与消息历史测试。
 * 覆盖：待受理->处理中->已解决/已关闭，非法流转被拒，消息追加保留，越权被拒。
 * 使用真实 MySQL，事务内完成回滚，不污染数据。
 */
@SpringBootTest
@Transactional
class ConsultationWorkflowTest {

    @Autowired
    private ConsultationService consultationService;
    @Autowired
    private HealthProfileService healthProfileService;
    @Autowired
    private UserMapper userMapper;

    private User user(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    private Long residentProfileId() {
        return healthProfileService.getByUserId(user("resident").getId()).getId();
    }

    private Long residentUserId() {
        return user("resident").getId();
    }

    private Long doctorId() {
        return user("doctor").getId();
    }

    private ConsultationVO create() {
        ConsultationCreateRequest req = new ConsultationCreateRequest();
        req.setTitle("血压偏高咨询");
        req.setCategory("HEALTH_INDICATOR");
        req.setDescription("最近血压偏高，想了解日常注意事项。");
        return consultationService.create(residentProfileId(), req);
    }

    private ConsultationMessageRequest msg(String content) {
        ConsultationMessageRequest m = new ConsultationMessageRequest();
        m.setContent(content);
        return m;
    }

    @Test
    void 创建后为待受理且无负责医生() {
        ConsultationVO vo = create();
        assertEquals(ConsultationStatus.PENDING.name(), vo.getStatus());
        assertNull(vo.getDoctorId());
        assertEquals(true, vo.getCanCancel());
        assertTrue(vo.getResidentCanMessage());
    }

    @Test
    void 受理后进入处理中() {
        ConsultationVO vo = create();
        ConsultationVO accepted = consultationService.accept(doctorId(), vo.getId());
        assertEquals(ConsultationStatus.PROCESSING.name(), accepted.getStatus());
        assertEquals(doctorId(), accepted.getDoctorId());
        assertNotNull(accepted.getAcceptedAt());
        assertFalse(accepted.getCanCancel());
    }

    @Test
    void 重复受理被拒绝() {
        ConsultationVO vo = create();
        consultationService.accept(doctorId(), vo.getId());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> consultationService.accept(doctorId(), vo.getId()));
        assertEquals(409, ex.getCode().intValue());
    }

    @Test
    void 未受理不能回复() {
        ConsultationVO vo = create();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> consultationService.addDoctorMessage(doctorId(), vo.getId(), msg("先回复")));
        assertTrue(ex.getCode() == 403 || ex.getCode() == 409);
    }

    @Test
    void 消息追加保留历史() {
        ConsultationVO vo = create();
        consultationService.accept(doctorId(), vo.getId());
        consultationService.addResidentMessage(residentProfileId(), residentUserId(), vo.getId(), msg("补充：早晨血压更高"));
        consultationService.addDoctorMessage(doctorId(), vo.getId(), msg("建议清淡饮食并监测"));
        consultationService.addDoctorMessage(doctorId(), vo.getId(), msg("如持续升高请及时就医"));

        ConsultationDetailVO detail = consultationService.getForDoctor(doctorId(), vo.getId());
        assertEquals(3, detail.getMessages().size());
        assertEquals("补充：早晨血压更高", detail.getMessages().get(0).getContent());
        assertEquals("DOCTOR", detail.getMessages().get(1).getSenderRole());
    }

    @Test
    void 重复相同内容被拒绝() {
        ConsultationVO vo = create();
        consultationService.accept(doctorId(), vo.getId());
        consultationService.addDoctorMessage(doctorId(), vo.getId(), msg("同样的回复"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> consultationService.addDoctorMessage(doctorId(), vo.getId(), msg("同样的回复")));
        assertEquals(409, ex.getCode().intValue());
    }

    @Test
    void 已解决后不能再回复或补充() {
        ConsultationVO vo = create();
        consultationService.accept(doctorId(), vo.getId());
        ConsultationVO resolved = consultationService.resolve(doctorId(), vo.getId());
        assertEquals(ConsultationStatus.RESOLVED.name(), resolved.getStatus());
        assertNotNull(resolved.getResolvedAt());

        assertEquals(409, assertThrows(BusinessException.class,
                () -> consultationService.addDoctorMessage(doctorId(), vo.getId(), msg("再补一句"))).getCode().intValue());
        assertEquals(409, assertThrows(BusinessException.class,
                () -> consultationService.addResidentMessage(residentProfileId(), residentUserId(), vo.getId(), msg("我也补"))).getCode().intValue());
    }

    @Test
    void 医生关闭后为已关闭且不能重复关闭() {
        ConsultationVO vo = create();
        consultationService.accept(doctorId(), vo.getId());
        ConsultationVO closed = consultationService.close(doctorId(), vo.getId());
        assertEquals(ConsultationStatus.CLOSED.name(), closed.getStatus());
        assertNotNull(closed.getClosedAt());
        assertEquals(409, assertThrows(BusinessException.class,
                () -> consultationService.close(doctorId(), vo.getId())).getCode().intValue());
    }

    @Test
    void 居民取消仅限待受理() {
        ConsultationVO vo = create();
        ConsultationVO cancelled = consultationService.cancel(residentProfileId(), vo.getId());
        assertEquals(ConsultationStatus.CLOSED.name(), cancelled.getStatus());

        ConsultationVO another = create();
        consultationService.accept(doctorId(), another.getId());
        assertEquals(409, assertThrows(BusinessException.class,
                () -> consultationService.cancel(residentProfileId(), another.getId())).getCode().intValue());
    }

    @Test
    void 居民不能访问他人咨询() {
        ConsultationVO vo = create();
        Long otherResidentId = residentProfileId() + 99999L;
        BusinessException ex = assertThrows(BusinessException.class,
                () -> consultationService.getForResident(otherResidentId, vo.getId()));
        assertEquals(404, ex.getCode().intValue());
    }

    @Test
    void 范围外医生不能处置该咨询() {
        ConsultationVO vo = create();
        // 用居民自己的 userId 冒充医生，必然不在授权范围内
        BusinessException ex = assertThrows(BusinessException.class,
                () -> consultationService.accept(residentUserId(), vo.getId()));
        assertEquals(403, ex.getCode().intValue());
    }

    @Test
    void 不存在的咨询返回404() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> consultationService.getForDoctor(doctorId(), 999999L));
        assertEquals(404, ex.getCode().intValue());
    }
}
