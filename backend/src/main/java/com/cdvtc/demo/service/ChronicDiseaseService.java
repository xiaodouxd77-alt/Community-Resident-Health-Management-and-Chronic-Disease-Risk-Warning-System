package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.dto.*;
import com.cdvtc.demo.entity.ChronicDiseaseHistory;
import com.cdvtc.demo.entity.ChronicDiseaseRecord;
import com.cdvtc.demo.entity.HealthProfile;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.enums.*;
import com.cdvtc.demo.mapper.ChronicDiseaseHistoryMapper;
import com.cdvtc.demo.mapper.ChronicDiseaseRecordMapper;
import com.cdvtc.demo.mapper.UserMapper;
import com.cdvtc.demo.vo.ChronicDiseaseVO;
import com.cdvtc.demo.vo.ChronicHistoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChronicDiseaseService extends ServiceImpl<ChronicDiseaseRecordMapper, ChronicDiseaseRecord> {

    private final HealthProfileService healthProfileService;
    private final DoctorResidentService doctorResidentService;
    private final ChronicDiseaseHistoryMapper historyMapper;
    private final UserMapper userMapper;

    // ============================ 医生端 ============================

    public IPage<ChronicDiseaseVO> pageForDoctor(Long doctorId, ChronicDiseaseQueryRequest query) {
        int page = normalizePage(query.getPage());
        int size = normalizeSize(query.getSize());
        List<Long> scopedResidentIds = doctorResidentService.listResidentIdsOfDoctor(doctorId);
        if (scopedResidentIds.isEmpty()) {
            return new Page<>(page, size, 0);
        }

        LambdaQueryWrapper<ChronicDiseaseRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ChronicDiseaseRecord::getResidentId, scopedResidentIds);

        if (StringUtils.hasText(query.getDiseaseType())) {
            DiseaseType type = DiseaseType.fromCode(query.getDiseaseType())
                    .orElseThrow(() -> new BusinessException(400, "慢病类型取值不正确"));
            wrapper.eq(ChronicDiseaseRecord::getDiseaseType, type.name());
        }
        if (StringUtils.hasText(query.getManagementStatus())) {
            ManagementStatus status = ManagementStatus.fromCode(query.getManagementStatus())
                    .orElseThrow(() -> new BusinessException(400, "管理状态取值不正确"));
            wrapper.eq(ChronicDiseaseRecord::getManagementStatus, status.name());
        }
        if (StringUtils.hasText(query.getDiagnosisStatus())) {
            DiagnosisStatus status = DiagnosisStatus.fromCode(query.getDiagnosisStatus())
                    .orElseThrow(() -> new BusinessException(400, "资料状态取值不正确"));
            wrapper.eq(ChronicDiseaseRecord::getDiagnosisStatus, status.name());
        }
        if (query.getManagingDoctorId() != null) {
            wrapper.eq(ChronicDiseaseRecord::getManagingDoctorId, query.getManagingDoctorId());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            List<Long> matched = matchResidentIdsByKeyword(query.getKeyword(), scopedResidentIds);
            if (matched.isEmpty()) {
                return new Page<>(page, size, 0);
            }
            wrapper.in(ChronicDiseaseRecord::getResidentId, matched);
        }
        wrapper.orderByDesc(ChronicDiseaseRecord::getUpdateTime).orderByDesc(ChronicDiseaseRecord::getId);

        IPage<ChronicDiseaseRecord> recordPage = page(new Page<>(page, size), wrapper);
        Page<ChronicDiseaseVO> voPage = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        voPage.setRecords(toVOList(recordPage.getRecords()));
        return voPage;
    }

    @Transactional
    public ChronicDiseaseVO create(Long doctorId, ChronicDiseaseCreateRequest request) {
        HealthProfile profile = healthProfileService.getByIdRequired(request.getResidentId());
        assertDoctorScope(doctorId, profile.getId());

        DiseaseType type = DiseaseType.fromCode(request.getDiseaseType())
                .orElseThrow(() -> new BusinessException(400, "慢病类型取值不正确"));

        // 同一居民、同一慢病类型，不允许存在重复的有效管理档案
        boolean duplicated = lambdaQuery()
                .eq(ChronicDiseaseRecord::getResidentId, profile.getId())
                .eq(ChronicDiseaseRecord::getDiseaseType, type.name())
                .in(ChronicDiseaseRecord::getManagementStatus,
                        ManagementStatus.ACTIVE.name(), ManagementStatus.SUSPENDED.name())
                .exists();
        if (duplicated) {
            throw new BusinessException(409, "该居民已存在相同慢病类型的有效管理档案");
        }

        ChronicDiseaseRecord record = new ChronicDiseaseRecord();
        record.setResidentId(profile.getId());
        record.setDiseaseType(type.name());
        record.setDiagnosisStatus(DiagnosisStatus.PENDING_REVIEW.name());
        record.setManagementStatus(ManagementStatus.ACTIVE.name());
        record.setDiagnosisDate(request.getDiagnosisDate());
        record.setDiagnosisBasis(trimOrNull(request.getDiagnosisBasis()));
        record.setRemark(trimOrNull(request.getRemark()));
        record.setManagingDoctorId(doctorId);
        save(record);

        writeHistory(record, ChronicAction.CREATE, doctorId,
                "为居民建档：" + type.getLabel() + "，资料状态待核实");
        return toVO(record);
    }

    public ChronicDiseaseVO getForDoctor(Long doctorId, Long id) {
        ChronicDiseaseRecord record = getByIdRequired(id);
        assertDoctorScope(doctorId, record.getResidentId());
        return toVO(record);
    }

    @Transactional
    public ChronicDiseaseVO update(Long doctorId, Long id, ChronicDiseaseUpdateRequest request) {
        ChronicDiseaseRecord record = getByIdRequired(id);
        assertDoctorScope(doctorId, record.getResidentId());
        if (ManagementStatus.CLOSED.name().equals(record.getManagementStatus())) {
            throw new BusinessException(409, "已终止管理的档案不可修改");
        }
        if (request.getDiagnosisDate() != null) {
            record.setDiagnosisDate(request.getDiagnosisDate());
        }
        if (request.getDiagnosisBasis() != null) {
            record.setDiagnosisBasis(trimOrNull(request.getDiagnosisBasis()));
        }
        if (request.getRemark() != null) {
            record.setRemark(trimOrNull(request.getRemark()));
        }
        updateById(record);
        writeHistory(record, ChronicAction.UPDATE, doctorId, "更新档案管理信息");
        return toVO(record);
    }

    @Transactional
    public ChronicDiseaseVO review(Long doctorId, Long id, ChronicDiseaseReviewRequest request) {
        ChronicDiseaseRecord record = getByIdRequired(id);
        assertDoctorScope(doctorId, record.getResidentId());

        DiagnosisStatus target = DiagnosisStatus.fromCode(request.getDiagnosisStatus())
                .filter(s -> s != DiagnosisStatus.PENDING_REVIEW)
                .orElseThrow(() -> new BusinessException(400, "审核结果仅支持 已核实/未通过"));
        if (!DiagnosisStatus.PENDING_REVIEW.name().equals(record.getDiagnosisStatus())) {
            throw new BusinessException(409, "该档案资料已审核，无需重复审核");
        }

        record.setDiagnosisStatus(target.name());
        record.setReviewedBy(doctorId);
        record.setReviewedAt(LocalDateTime.now());
        updateById(record);
        writeHistory(record, ChronicAction.REVIEW, doctorId,
                "资料审核：" + target.getLabel()
                        + (StringUtils.hasText(request.getComment()) ? "；" + request.getComment().trim() : ""));
        return toVO(record);
    }

    @Transactional
    public ChronicDiseaseVO changeStatus(Long doctorId, Long id, ChronicDiseaseStatusRequest request) {
        ChronicDiseaseRecord record = getByIdRequired(id);
        assertDoctorScope(doctorId, record.getResidentId());

        ManagementStatus current = ManagementStatus.fromCode(record.getManagementStatus())
                .orElse(ManagementStatus.ACTIVE);
        ManagementStatus target = ManagementStatus.fromCode(request.getManagementStatus())
                .orElseThrow(() -> new BusinessException(400, "管理状态取值不正确"));

        if (current == ManagementStatus.CLOSED) {
            throw new BusinessException(409, "该档案已终止管理，状态不可再变更");
        }
        if (current == target) {
            throw new BusinessException(400, "管理状态未发生变化");
        }

        record.setManagementStatus(target.name());
        updateById(record);
        writeHistory(record, ChronicAction.STATUS_CHANGE, doctorId,
                "管理状态：" + current.getLabel() + " → " + target.getLabel()
                        + (StringUtils.hasText(request.getComment()) ? "；" + request.getComment().trim() : ""));
        return toVO(record);
    }

    // ============================ 居民端 ============================

    /** 某居民的全部慢病档案（居民本人或医生详情页使用）。 */
    public List<ChronicDiseaseVO> listByResident(Long residentId) {
        List<ChronicDiseaseRecord> records = lambdaQuery()
                .eq(ChronicDiseaseRecord::getResidentId, residentId)
                .orderByDesc(ChronicDiseaseRecord::getCreateTime)
                .orderByDesc(ChronicDiseaseRecord::getId)
                .list();
        return toVOList(records);
    }

    // ============================ 组装 ============================

    public List<ChronicHistoryVO> history(Long chronicId) {
        List<ChronicDiseaseHistory> histories = historyMapper.selectList(
                new LambdaQueryWrapper<ChronicDiseaseHistory>()
                        .eq(ChronicDiseaseHistory::getChronicId, chronicId)
                        .orderByDesc(ChronicDiseaseHistory::getCreateTime)
                        .orderByDesc(ChronicDiseaseHistory::getId));
        Map<Long, String> operators = loadUserNames(
                histories.stream().map(ChronicDiseaseHistory::getOperatorId).toList());
        return histories.stream().map(h -> {
            ChronicHistoryVO vo = new ChronicHistoryVO();
            vo.setId(h.getId());
            vo.setAction(h.getAction());
            vo.setActionLabel(ChronicAction.labelOf(h.getAction()));
            vo.setDetail(h.getDetail());
            vo.setOperatorId(h.getOperatorId());
            vo.setOperatorName(h.getOperatorId() == null ? null : operators.get(h.getOperatorId()));
            vo.setCreateTime(h.getCreateTime());
            return vo;
        }).toList();
    }

    public List<ChronicDiseaseVO> toVOList(List<ChronicDiseaseRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, HealthProfile> profiles = healthProfileService
                .listByIds(records.stream().map(ChronicDiseaseRecord::getResidentId).distinct().toList())
                .stream().collect(Collectors.toMap(HealthProfile::getId, Function.identity(), (a, b) -> a));
        Set<Long> userIds = new HashSet<>();
        records.forEach(r -> {
            if (r.getManagingDoctorId() != null) userIds.add(r.getManagingDoctorId());
            if (r.getReviewedBy() != null) userIds.add(r.getReviewedBy());
        });
        Map<Long, String> userNames = loadUserNames(new ArrayList<>(userIds));

        return records.stream().map(r -> {
            HealthProfile p = profiles.get(r.getResidentId());
            return toVO(r, p, userNames);
        }).toList();
    }

    private ChronicDiseaseVO toVO(ChronicDiseaseRecord record) {
        HealthProfile p = healthProfileService.getById(record.getResidentId());
        Set<Long> ids = new HashSet<>();
        if (record.getManagingDoctorId() != null) ids.add(record.getManagingDoctorId());
        if (record.getReviewedBy() != null) ids.add(record.getReviewedBy());
        Map<Long, String> userNames = loadUserNames(new ArrayList<>(ids));
        return toVO(record, p, userNames);
    }

    private ChronicDiseaseVO toVO(ChronicDiseaseRecord r, HealthProfile p, Map<Long, String> userNames) {
        ChronicDiseaseVO vo = new ChronicDiseaseVO();
        vo.setId(r.getId());
        vo.setResidentId(r.getResidentId());
        if (p != null) {
            vo.setResidentNo(p.getResidentNo());
            vo.setResidentName(StringUtils.hasText(p.getRealName()) ? p.getRealName() : p.getResidentNo());
        }
        vo.setDiseaseType(r.getDiseaseType());
        vo.setDiseaseTypeLabel(DiseaseType.labelOf(r.getDiseaseType()));
        vo.setDiagnosisStatus(r.getDiagnosisStatus());
        vo.setDiagnosisStatusLabel(DiagnosisStatus.labelOf(r.getDiagnosisStatus()));
        vo.setDiagnosisDate(r.getDiagnosisDate());
        vo.setDiagnosisBasis(r.getDiagnosisBasis());
        vo.setManagementStatus(r.getManagementStatus());
        vo.setManagementStatusLabel(ManagementStatus.labelOf(r.getManagementStatus()));
        vo.setManagingDoctorId(r.getManagingDoctorId());
        vo.setManagingDoctorName(r.getManagingDoctorId() == null ? null : userNames.get(r.getManagingDoctorId()));
        vo.setReviewedBy(r.getReviewedBy());
        vo.setReviewedByName(r.getReviewedBy() == null ? null : userNames.get(r.getReviewedBy()));
        vo.setReviewedAt(r.getReviewedAt());
        vo.setRemark(r.getRemark());
        vo.setCreateTime(r.getCreateTime());
        vo.setUpdateTime(r.getUpdateTime());
        return vo;
    }

    // ============================ 工具 ============================

    private void writeHistory(ChronicDiseaseRecord record, ChronicAction action, Long operatorId, String detail) {
        ChronicDiseaseHistory history = new ChronicDiseaseHistory();
        history.setChronicId(record.getId());
        history.setResidentId(record.getResidentId());
        history.setAction(action.name());
        history.setDetail(detail);
        history.setOperatorId(operatorId);
        historyMapper.insert(history);
    }

    private ChronicDiseaseRecord getByIdRequired(Long id) {
        ChronicDiseaseRecord record = getById(id);
        if (record == null) {
            throw new BusinessException(404, "慢病档案不存在");
        }
        return record;
    }

    private void assertDoctorScope(Long doctorId, Long residentId) {
        if (!doctorResidentService.isResidentInDoctorScope(doctorId, residentId)) {
            throw new BusinessException(403, "该居民不在您的管理范围内");
        }
    }

    private List<Long> matchResidentIdsByKeyword(String keyword, List<Long> scopedResidentIds) {
        return healthProfileService.lambdaQuery()
                .in(HealthProfile::getId, scopedResidentIds)
                .and(q -> q.like(HealthProfile::getResidentNo, keyword)
                        .or().like(HealthProfile::getRealName, keyword))
                .list()
                .stream().map(HealthProfile::getId).toList();
    }

    private Map<Long, String> loadUserNames(List<Long> ids) {
        List<Long> valid = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (valid.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(valid).stream()
                .collect(Collectors.toMap(User::getId, User::displayName, (a, b) -> a));
    }

    private String trimOrNull(String value) {
        String v = value == null ? null : value.trim();
        return StringUtils.hasText(v) ? v : null;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        int s = size == null ? 10 : size;
        return s < 1 || s > 100 ? 10 : s;
    }
}
