package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.dto.ConsultationCreateRequest;
import com.cdvtc.demo.dto.ConsultationMessageRequest;
import com.cdvtc.demo.dto.ConsultationQueryRequest;
import com.cdvtc.demo.entity.Consultation;
import com.cdvtc.demo.entity.ConsultationMessage;
import com.cdvtc.demo.entity.HealthProfile;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.enums.ConsultationCategory;
import com.cdvtc.demo.enums.ConsultationStatus;
import com.cdvtc.demo.enums.RoleEnum;
import com.cdvtc.demo.mapper.ConsultationMapper;
import com.cdvtc.demo.mapper.ConsultationMessageMapper;
import com.cdvtc.demo.mapper.UserMapper;
import com.cdvtc.demo.vo.ConsultationDetailVO;
import com.cdvtc.demo.vo.ConsultationMessageVO;
import com.cdvtc.demo.vo.ConsultationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 在线健康咨询。
 *   - 归属与权限全部以登录身份为准，不信任前端传入的 residentId / senderRole；
 *   - 状态转换集中在此层校验，非法转换抛 409；
 *   - 消息追加式保存，一次咨询保留完整对话历史，不覆盖；
 *   - 医生处置范围复用 doctor_resident 授权关系。
 * 说明：在线咨询是健康管理服务，不是急诊通道，回复内容不构成正式诊断。
 */
@Service
@RequiredArgsConstructor
public class ConsultationService extends ServiceImpl<ConsultationMapper, Consultation> {

    private final ConsultationMessageMapper messageMapper;
    private final HealthProfileService healthProfileService;
    private final DoctorResidentService doctorResidentService;
    private final UserMapper userMapper;

    // ============================ 居民端 ============================

    @Transactional
    public ConsultationVO create(Long residentId, ConsultationCreateRequest request) {
        ConsultationCategory category = ConsultationCategory.fromCode(request.getCategory())
                .orElseThrow(() -> new BusinessException(400, "咨询分类取值不正确"));

        Consultation c = new Consultation();
        c.setResidentId(residentId);
        c.setDoctorId(null);
        c.setTitle(request.getTitle().trim());
        c.setCategory(category.name());
        c.setDescription(request.getDescription().trim());
        c.setStatus(ConsultationStatus.PENDING.name());
        save(c);
        return assemble(List.of(c)).get(0);
    }

    public IPage<ConsultationVO> pageForResident(Long residentId, ConsultationQueryRequest query) {
        LambdaQueryWrapper<Consultation> wrapper = new LambdaQueryWrapper<Consultation>()
                .eq(Consultation::getResidentId, residentId);
        applyCommonFilters(wrapper, query);
        wrapper.orderByDesc(Consultation::getCreateTime).orderByDesc(Consultation::getId);
        return toVoPage(page(new Page<>(normalizePage(query.getPage()), normalizeSize(query.getSize())), wrapper));
    }

    public ConsultationDetailVO getForResident(Long residentId, Long id) {
        Consultation c = requireConsultation(id);
        if (!Objects.equals(c.getResidentId(), residentId)) {
            // 不属于本人，统一按不存在处理，避免泄露他人咨询
            throw new BusinessException(404, "咨询不存在");
        }
        return detail(c);
    }

    /** 居民补充说明（待受理、处理中可发）。residentId 为档案 id 用于归属校验，senderId 为登录用户 id 用于落库。 */
    @Transactional
    public ConsultationMessageVO addResidentMessage(Long residentId, Long senderId, Long id, ConsultationMessageRequest request) {
        Consultation c = requireConsultation(id);
        if (!Objects.equals(c.getResidentId(), residentId)) {
            throw new BusinessException(404, "咨询不存在");
        }
        ConsultationStatus status = currentStatus(c);
        if (!status.residentCanMessage()) {
            throw new BusinessException(409, "咨询已" + status.getLabel() + "，无法继续发送消息");
        }
        return appendMessage(c, senderId, RoleEnum.RESIDENT, request.getContent());
    }

    /** 居民取消咨询：仅待受理可直接取消。 */
    @Transactional
    public ConsultationVO cancel(Long residentId, Long id) {
        Consultation c = requireConsultation(id);
        if (!Objects.equals(c.getResidentId(), residentId)) {
            throw new BusinessException(404, "咨询不存在");
        }
        ConsultationStatus status = currentStatus(c);
        if (status != ConsultationStatus.PENDING) {
            throw new BusinessException(409, "咨询已" + status.getLabel() + "，无法取消");
        }
        c.setStatus(ConsultationStatus.CLOSED.name());
        c.setClosedAt(LocalDateTime.now());
        updateById(c);
        return assemble(List.of(c)).get(0);
    }

    // ============================ 医生端 ============================

    /** 待受理（范围内、未分配）与我处理的咨询。 */
    public IPage<ConsultationVO> pageForDoctor(Long doctorId, ConsultationQueryRequest query) {
        List<Long> scoped = doctorResidentService.listResidentIdsOfDoctor(doctorId);
        int p = normalizePage(query.getPage());
        int s = normalizeSize(query.getSize());
        if (scoped.isEmpty()) {
            return new Page<>(p, s, 0);
        }
        LambdaQueryWrapper<Consultation> wrapper = new LambdaQueryWrapper<Consultation>()
                .in(Consultation::getResidentId, scoped)
                .and(w -> w.eq(Consultation::getDoctorId, doctorId)
                        .or(o -> o.isNull(Consultation::getDoctorId)
                                .eq(Consultation::getStatus, ConsultationStatus.PENDING.name())));
        applyCommonFilters(wrapper, query);
        wrapper.orderByDesc(Consultation::getCreateTime).orderByDesc(Consultation::getId);
        return toVoPage(page(new Page<>(p, s), wrapper));
    }

    public ConsultationDetailVO getForDoctor(Long doctorId, Long id) {
        Consultation c = requireConsultation(id);
        assertDoctorScope(doctorId, c);
        return detail(c);
    }

    /** 受理：仅待受理、且尚无医生的咨询可被受理。 */
    @Transactional
    public ConsultationVO accept(Long doctorId, Long id) {
        Consultation c = requireConsultation(id);
        assertDoctorScope(doctorId, c);
        ConsultationStatus status = currentStatus(c);
        if (c.getDoctorId() != null) {
            throw new BusinessException(409, "该咨询已被受理");
        }
        if (status != ConsultationStatus.PENDING) {
            throw new BusinessException(409, "咨询已" + status.getLabel() + "，无法受理");
        }
        c.setDoctorId(doctorId);
        c.setStatus(ConsultationStatus.PROCESSING.name());
        c.setAcceptedAt(LocalDateTime.now());
        updateById(c);
        return assemble(List.of(c)).get(0);
    }

    /** 医生回复：仅受理医生本人、且处理中。 */
    @Transactional
    public ConsultationMessageVO addDoctorMessage(Long doctorId, Long id, ConsultationMessageRequest request) {
        Consultation c = requireConsultation(id);
        assertDoctorScope(doctorId, c);
        if (!Objects.equals(c.getDoctorId(), doctorId)) {
            throw new BusinessException(403, "请先受理该咨询后再回复");
        }
        ConsultationStatus status = currentStatus(c);
        if (status != ConsultationStatus.PROCESSING) {
            throw new BusinessException(409, "咨询已" + status.getLabel() + "，无法回复");
        }
        return appendMessage(c, doctorId, RoleEnum.DOCTOR, request.getContent());
    }

    /** 解决：受理医生本人、处理中 → 已解决。 */
    @Transactional
    public ConsultationVO resolve(Long doctorId, Long id) {
        Consultation c = requireConsultation(id);
        assertDoctorScope(doctorId, c);
        assertAssignedDoctor(c, doctorId);
        ConsultationStatus status = currentStatus(c);
        if (status != ConsultationStatus.PROCESSING) {
            throw new BusinessException(409, "咨询已" + status.getLabel() + "，无法标记解决");
        }
        c.setStatus(ConsultationStatus.RESOLVED.name());
        c.setResolvedAt(LocalDateTime.now());
        updateById(c);
        return assemble(List.of(c)).get(0);
    }

    /** 关闭：受理医生本人、处理中或已解决 → 已关闭。 */
    @Transactional
    public ConsultationVO close(Long doctorId, Long id) {
        Consultation c = requireConsultation(id);
        assertDoctorScope(doctorId, c);
        assertAssignedDoctor(c, doctorId);
        ConsultationStatus status = currentStatus(c);
        if (status == ConsultationStatus.CLOSED) {
            throw new BusinessException(409, "咨询已关闭，请勿重复操作");
        }
        if (status == ConsultationStatus.PENDING) {
            throw new BusinessException(409, "待受理的咨询不能直接关闭，请先受理");
        }
        c.setStatus(ConsultationStatus.CLOSED.name());
        c.setClosedAt(LocalDateTime.now());
        updateById(c);
        return assemble(List.of(c)).get(0);
    }

    // ============================ 消息列表 ============================

    public List<ConsultationMessageVO> messagesForResident(Long residentId, Long id) {
        Consultation c = requireConsultation(id);
        if (!Objects.equals(c.getResidentId(), residentId)) {
            throw new BusinessException(404, "咨询不存在");
        }
        return loadMessages(c.getId());
    }

    public List<ConsultationMessageVO> messagesForDoctor(Long doctorId, Long id) {
        Consultation c = requireConsultation(id);
        assertDoctorScope(doctorId, c);
        return loadMessages(c.getId());
    }

    // ============================ 内部 ============================

    private void applyCommonFilters(LambdaQueryWrapper<Consultation> wrapper, ConsultationQueryRequest query) {
        if (StringUtils.hasText(query.getStatus())) {
            ConsultationStatus status = ConsultationStatus.fromCode(query.getStatus())
                    .orElseThrow(() -> new BusinessException(400, "咨询状态取值不正确"));
            wrapper.eq(Consultation::getStatus, status.name());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(Consultation::getTitle, kw)
                    .or().like(Consultation::getDescription, kw));
        }
    }

    private Consultation requireConsultation(Long id) {
        Consultation c = getById(id);
        if (c == null) {
            throw new BusinessException(404, "咨询不存在");
        }
        return c;
    }

    private ConsultationStatus currentStatus(Consultation c) {
        return ConsultationStatus.fromCode(c.getStatus())
                .orElseThrow(() -> new BusinessException(500, "咨询状态异常"));
    }

    private void assertDoctorScope(Long doctorId, Consultation c) {
        if (!doctorResidentService.isResidentInDoctorScope(doctorId, c.getResidentId())) {
            throw new BusinessException(403, "该居民不在您的管理范围内");
        }
    }

    private void assertAssignedDoctor(Consultation c, Long doctorId) {
        if (!Objects.equals(c.getDoctorId(), doctorId)) {
            throw new BusinessException(403, "该咨询不由您负责");
        }
    }

    private ConsultationMessageVO appendMessage(Consultation c, Long senderId, RoleEnum role, String rawContent) {
        String content = rawContent == null ? null : rawContent.trim();
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(400, "消息内容不能为空");
        }
        // 防重复提交：与最近一条同人同内容的消息视为重复
        ConsultationMessage last = messageMapper.selectOne(new LambdaQueryWrapper<ConsultationMessage>()
                .eq(ConsultationMessage::getConsultationId, c.getId())
                .orderByDesc(ConsultationMessage::getId)
                .last("LIMIT 1"));
        if (last != null && Objects.equals(last.getSenderId(), senderId)
                && content.equals(last.getContent())) {
            throw new BusinessException(409, "请勿重复提交相同内容");
        }

        ConsultationMessage msg = new ConsultationMessage();
        msg.setConsultationId(c.getId());
        msg.setSenderId(senderId);
        msg.setSenderRole(role.name());
        msg.setContent(content);
        messageMapper.insert(msg);

        // 触达回复时刷新咨询更新时间，便于医生工作台按活跃度排序
        c.setUpdateTime(LocalDateTime.now());
        updateById(c);
        return toMessageVO(msg, role);
    }

    private ConsultationMessageVO toMessageVO(ConsultationMessage m, RoleEnum role) {
        ConsultationMessageVO vo = new ConsultationMessageVO();
        vo.setId(m.getId());
        vo.setConsultationId(m.getConsultationId());
        vo.setSenderId(m.getSenderId());
        vo.setSenderRole(m.getSenderRole());
        vo.setSenderRoleLabel(role.getLabel());
        vo.setContent(m.getContent());
        vo.setCreateTime(m.getCreateTime());
        User sender = userMapper.selectById(m.getSenderId());
        vo.setSenderName(sender == null ? null : sender.displayName());
        return vo;
    }

    private List<ConsultationMessageVO> loadMessages(Long consultationId) {
        List<ConsultationMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<ConsultationMessage>()
                        .eq(ConsultationMessage::getConsultationId, consultationId)
                        .orderByAsc(ConsultationMessage::getCreateTime)
                        .orderByAsc(ConsultationMessage::getId));
        if (messages.isEmpty()) {
            return List.of();
        }
        Set<Long> senderIds = messages.stream().map(ConsultationMessage::getSenderId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> senders = senderIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(senderIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        return messages.stream().map(m -> {
            ConsultationMessageVO vo = new ConsultationMessageVO();
            vo.setId(m.getId());
            vo.setConsultationId(m.getConsultationId());
            vo.setSenderId(m.getSenderId());
            vo.setSenderRole(m.getSenderRole());
            vo.setSenderRoleLabel(RoleEnum.of(m.getSenderRole()).getLabel());
            vo.setContent(m.getContent());
            vo.setCreateTime(m.getCreateTime());
            User sender = senders.get(m.getSenderId());
            vo.setSenderName(sender == null ? null : sender.displayName());
            return vo;
        }).toList();
    }

    private ConsultationDetailVO detail(Consultation c) {
        ConsultationDetailVO detail = new ConsultationDetailVO();
        detail.setConsultation(assemble(List.of(c)).get(0));
        detail.setMessages(loadMessages(c.getId()));
        return detail;
    }

    private IPage<ConsultationVO> toVoPage(IPage<Consultation> page) {
        Page<ConsultationVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(assemble(page.getRecords()));
        return voPage;
    }

    private List<ConsultationVO> assemble(List<Consultation> consultations) {
        if (consultations == null || consultations.isEmpty()) {
            return List.of();
        }
        List<Long> ids = consultations.stream().map(Consultation::getId).toList();
        Set<Long> residentIds = consultations.stream().map(Consultation::getResidentId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> doctorIds = consultations.stream().map(Consultation::getDoctorId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, HealthProfile> profiles = residentIds.isEmpty() ? Map.of()
                : healthProfileService.listByIds(residentIds).stream()
                .collect(Collectors.toMap(HealthProfile::getId, p -> p, (a, b) -> a));
        Map<Long, User> doctors = doctorIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(doctorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        Map<Long, List<ConsultationMessage>> msgMap = messageMapper.selectList(
                        new LambdaQueryWrapper<ConsultationMessage>()
                                .in(ConsultationMessage::getConsultationId, ids)
                                .orderByAsc(ConsultationMessage::getCreateTime)
                                .orderByAsc(ConsultationMessage::getId))
                .stream().collect(Collectors.groupingBy(ConsultationMessage::getConsultationId));

        return consultations.stream().map(c -> {
            ConsultationVO vo = new ConsultationVO();
            vo.setId(c.getId());
            vo.setResidentId(c.getResidentId());
            HealthProfile profile = profiles.get(c.getResidentId());
            if (profile != null) {
                vo.setResidentNo(profile.getResidentNo());
                vo.setResidentName(StringUtils.hasText(profile.getRealName())
                        ? profile.getRealName() : profile.getResidentNo());
            }
            vo.setDoctorId(c.getDoctorId());
            User doctor = c.getDoctorId() == null ? null : doctors.get(c.getDoctorId());
            vo.setDoctorName(doctor == null ? null : doctor.displayName());
            vo.setTitle(c.getTitle());
            vo.setCategory(c.getCategory());
            vo.setCategoryLabel(ConsultationCategory.labelOf(c.getCategory()));
            vo.setDescription(c.getDescription());
            vo.setStatus(c.getStatus());
            ConsultationStatus status = ConsultationStatus.fromCode(c.getStatus()).orElse(null);
            vo.setStatusLabel(status == null ? c.getStatus() : status.getLabel());
            vo.setAcceptedAt(c.getAcceptedAt());
            vo.setResolvedAt(c.getResolvedAt());
            vo.setClosedAt(c.getClosedAt());
            List<ConsultationMessage> msgs = msgMap.getOrDefault(c.getId(), List.of());
            vo.setMessageCount(msgs.size());
            vo.setLastMessageAt(msgs.isEmpty() ? null : msgs.get(msgs.size() - 1).getCreateTime());
            vo.setCanCancel(status == ConsultationStatus.PENDING);
            vo.setResidentCanMessage(status != null && status.residentCanMessage());
            vo.setCreateTime(c.getCreateTime());
            vo.setUpdateTime(c.getUpdateTime());
            return vo;
        }).toList();
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        int s = size == null ? 10 : size;
        return s < 1 || s > 100 ? 10 : s;
    }
}
