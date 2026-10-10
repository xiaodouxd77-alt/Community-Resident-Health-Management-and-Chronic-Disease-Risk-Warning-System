package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.dto.ResidentGroupCreateRequest;
import com.cdvtc.demo.dto.ResidentGroupMemberAddRequest;
import com.cdvtc.demo.dto.ResidentGroupQueryRequest;
import com.cdvtc.demo.dto.ResidentGroupUpdateRequest;
import com.cdvtc.demo.entity.HealthProfile;
import com.cdvtc.demo.entity.ResidentGroup;
import com.cdvtc.demo.entity.ResidentGroupMember;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.enums.Gender;
import com.cdvtc.demo.enums.GroupStatus;
import com.cdvtc.demo.mapper.ResidentGroupMapper;
import com.cdvtc.demo.mapper.ResidentGroupMemberMapper;
import com.cdvtc.demo.mapper.UserMapper;
import com.cdvtc.demo.vo.ResidentGroupMemberVO;
import com.cdvtc.demo.vo.ResidentGroupVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResidentGroupService extends ServiceImpl<ResidentGroupMapper, ResidentGroup> {

    private static final DateTimeFormatter CODE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ResidentGroupMemberMapper memberMapper;
    private final CommunityService communityService;
    private final HealthProfileService healthProfileService;
    private final DoctorResidentService doctorResidentService;
    private final UserMapper userMapper;

    // ============================ 分组 CRUD ============================

    public IPage<ResidentGroupVO> pageForDoctor(Long doctorId, ResidentGroupQueryRequest query) {
        int page = normalizePage(query.getPage());
        int size = normalizeSize(query.getSize());

        LambdaQueryWrapper<ResidentGroup> wrapper = new LambdaQueryWrapper<>();
        // 可见范围：本人负责或本人创建的分组
        wrapper.and(w -> w.eq(ResidentGroup::getManagerDoctorId, doctorId)
                .or().eq(ResidentGroup::getCreatedBy, doctorId));

        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(ResidentGroup::getGroupName, kw)
                    .or().like(ResidentGroup::getGroupCode, kw));
        }
        if (query.getCommunityId() != null) {
            wrapper.eq(ResidentGroup::getCommunityId, query.getCommunityId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            GroupStatus status = GroupStatus.fromCode(query.getStatus())
                    .orElseThrow(() -> new BusinessException(400, "分组状态取值不正确"));
            wrapper.eq(ResidentGroup::getStatus, status.name());
        }
        if (query.getManagerDoctorId() != null) {
            wrapper.eq(ResidentGroup::getManagerDoctorId, query.getManagerDoctorId());
        }
        wrapper.orderByDesc(ResidentGroup::getCreateTime).orderByDesc(ResidentGroup::getId);

        IPage<ResidentGroup> groupPage = page(new Page<>(page, size), wrapper);
        List<ResidentGroup> records = groupPage.getRecords();
        Map<Long, Long> counts = memberCounts(records.stream().map(ResidentGroup::getId).toList());
        Map<Long, String> communityNames = communityService.nameMap(
                records.stream().map(ResidentGroup::getCommunityId).filter(Objects::nonNull).toList());
        Map<Long, String> userNames = loadUserNames(collectUserIds(records));

        List<ResidentGroupVO> voList = records.stream()
                .map(g -> toVO(g, counts.getOrDefault(g.getId(), 0L), communityNames, userNames))
                .toList();
        Page<ResidentGroupVO> voPage = new Page<>(groupPage.getCurrent(), groupPage.getSize(), groupPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    public ResidentGroupVO getForDoctor(Long doctorId, Long groupId) {
        ResidentGroup group = getByIdRequired(groupId);
        assertManaged(doctorId, group);
        Map<Long, Long> counts = memberCounts(List.of(group.getId()));
        Map<Long, String> communityNames = communityService.nameMap(
                group.getCommunityId() == null ? List.of() : List.of(group.getCommunityId()));
        Map<Long, String> userNames = loadUserNames(collectUserIds(List.of(group)));
        return toVO(group, counts.getOrDefault(group.getId(), 0L), communityNames, userNames);
    }

    @Transactional
    public ResidentGroupVO create(Long doctorId, ResidentGroupCreateRequest request) {
        ResidentGroup group = new ResidentGroup();
        group.setGroupName(request.getGroupName().trim());
        group.setGroupCode(StringUtils.hasText(request.getGroupCode())
                ? request.getGroupCode().trim() : generateGroupCode());
        group.setDescription(trimOrNull(request.getDescription()));
        if (request.getCommunityId() != null) {
            communityService.requireExists(request.getCommunityId());
            group.setCommunityId(request.getCommunityId());
        }
        group.setManagerDoctorId(request.getManagerDoctorId() != null ? request.getManagerDoctorId() : doctorId);
        group.setCreatedBy(doctorId);
        group.setStatus(GroupStatus.ACTIVE.name());
        try {
            save(group);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new BusinessException(409, "分组编码已存在");
        }
        return getForDoctor(doctorId, group.getId());
    }

    @Transactional
    public ResidentGroupVO update(Long doctorId, Long groupId, ResidentGroupUpdateRequest request) {
        ResidentGroup group = getByIdRequired(groupId);
        assertManaged(doctorId, group);

        if (StringUtils.hasText(request.getGroupName())) {
            group.setGroupName(request.getGroupName().trim());
        }
        if (request.getDescription() != null) {
            group.setDescription(trimOrNull(request.getDescription()));
        }
        if (request.getCommunityId() != null) {
            communityService.requireExists(request.getCommunityId());
            group.setCommunityId(request.getCommunityId());
        }
        if (request.getManagerDoctorId() != null) {
            group.setManagerDoctorId(request.getManagerDoctorId());
        }
        if (StringUtils.hasText(request.getStatus())) {
            GroupStatus status = GroupStatus.fromCode(request.getStatus())
                    .orElseThrow(() -> new BusinessException(400, "分组状态取值不正确"));
            group.setStatus(status.name());
        }
        updateById(group);
        return getForDoctor(doctorId, groupId);
    }

    // ============================ 成员管理 ============================

    public IPage<ResidentGroupMemberVO> pageMembers(Long doctorId, Long groupId, String keyword,
                                                    Integer pageNo, Integer sizeNo) {
        ResidentGroup group = getByIdRequired(groupId);
        assertManaged(doctorId, group);

        int page = normalizePage(pageNo);
        int size = normalizeSize(sizeNo);
        LambdaQueryWrapper<ResidentGroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResidentGroupMember::getGroupId, groupId);

        if (StringUtils.hasText(keyword)) {
            List<Long> matchedResidentIds = healthProfileService.lambdaQuery()
                    .and(q -> q.like(HealthProfile::getResidentNo, keyword.trim())
                            .or().like(HealthProfile::getRealName, keyword.trim()))
                    .list().stream().map(HealthProfile::getId).toList();
            if (matchedResidentIds.isEmpty()) {
                return new Page<>(page, size, 0);
            }
            wrapper.in(ResidentGroupMember::getResidentId, matchedResidentIds);
        }
        wrapper.orderByDesc(ResidentGroupMember::getCreateTime).orderByDesc(ResidentGroupMember::getId);

        IPage<ResidentGroupMember> memberPage = memberMapper.selectPage(new Page<>(page, size), wrapper);
        List<ResidentGroupMember> records = memberPage.getRecords();
        Map<Long, HealthProfile> profiles = loadProfiles(records);
        Map<Long, String> communityNames = communityService.nameMap(
                profiles.values().stream().map(HealthProfile::getCommunityId).filter(Objects::nonNull).toList());
        Map<Long, String> userNames = loadUserNames(
                records.stream().map(ResidentGroupMember::getAddedBy).filter(Objects::nonNull).distinct().toList());

        List<ResidentGroupMemberVO> voList = records.stream()
                .map(m -> toMemberVO(m, profiles.get(m.getResidentId()), communityNames, userNames))
                .toList();
        Page<ResidentGroupMemberVO> voPage = new Page<>(memberPage.getCurrent(), memberPage.getSize(), memberPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    /** 批量添加成员，返回 {added, skipped}。 */
    @Transactional
    public Map<String, Object> addMembers(Long doctorId, Long groupId, ResidentGroupMemberAddRequest request) {
        ResidentGroup group = getByIdRequired(groupId);
        assertManaged(doctorId, group);

        List<Long> residentIds = request.getResidentIds().stream().distinct().toList();
        // 逐个体校验访问权限与社区归属
        for (Long residentId : residentIds) {
            if (!doctorResidentService.isResidentInDoctorScope(doctorId, residentId)) {
                throw new BusinessException(403, "存在不在您管理范围内的居民，无法添加");
            }
        }
        Map<Long, HealthProfile> profiles = healthProfileService.listByIds(residentIds).stream()
                .collect(Collectors.toMap(HealthProfile::getId, Function.identity(), (a, b) -> a));
        if (group.getCommunityId() != null) {
            for (Long residentId : residentIds) {
                HealthProfile p = profiles.get(residentId);
                if (p != null && p.getCommunityId() != null
                        && !Objects.equals(p.getCommunityId(), group.getCommunityId())) {
                    throw new BusinessException(400, "居民[" + p.getResidentNo()
                            + "]所属社区与分组社区不一致，无法加入");
                }
            }
        }

        String remark = trimOrNull(request.getRemark());
        int added = 0;
        int skipped = 0;
        for (Long residentId : residentIds) {
            boolean exists = memberMapper.exists(new LambdaQueryWrapper<ResidentGroupMember>()
                    .eq(ResidentGroupMember::getGroupId, groupId)
                    .eq(ResidentGroupMember::getResidentId, residentId));
            if (exists) {
                skipped++;
                continue;
            }
            int restored = memberMapper.restoreMember(groupId, residentId, doctorId, remark);
            if (restored > 0) {
                added++;
                continue;
            }
            ResidentGroupMember member = new ResidentGroupMember();
            member.setGroupId(groupId);
            member.setResidentId(residentId);
            member.setAddedBy(doctorId);
            member.setRemark(remark);
            try {
                memberMapper.insert(member);
                added++;
            } catch (org.springframework.dao.DuplicateKeyException e) {
                skipped++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("added", added);
        result.put("skipped", skipped);
        return result;
    }

    @Transactional
    public void removeMember(Long doctorId, Long groupId, Long residentId) {
        ResidentGroup group = getByIdRequired(groupId);
        assertManaged(doctorId, group);
        boolean removed = memberMapper.delete(new LambdaQueryWrapper<ResidentGroupMember>()
                .eq(ResidentGroupMember::getGroupId, groupId)
                .eq(ResidentGroupMember::getResidentId, residentId)) > 0;
        if (!removed) {
            throw new BusinessException(404, "该居民不在此分组中");
        }
    }

    /** 某居民所属的分组（居民详情页与居民本人查看）。 */
    public List<ResidentGroupVO> listGroupsOfResident(Long residentId) {
        List<Long> groupIds = memberMapper.selectList(new LambdaQueryWrapper<ResidentGroupMember>()
                        .eq(ResidentGroupMember::getResidentId, residentId))
                .stream().map(ResidentGroupMember::getGroupId).distinct().toList();
        if (groupIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<ResidentGroup> groups = listByIds(groupIds);
        Map<Long, Long> counts = memberCounts(groupIds);
        Map<Long, String> communityNames = communityService.nameMap(
                groups.stream().map(ResidentGroup::getCommunityId).filter(Objects::nonNull).toList());
        Map<Long, String> userNames = loadUserNames(collectUserIds(groups));
        return groups.stream()
                .map(g -> toVO(g, counts.getOrDefault(g.getId(), 0L), communityNames, userNames))
                .toList();
    }

    // ============================ 组装 ============================

    private Map<Long, Long> memberCounts(List<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ResidentGroupMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<ResidentGroupMember>().in(ResidentGroupMember::getGroupId, groupIds));
        return members.stream().collect(Collectors.groupingBy(ResidentGroupMember::getGroupId, Collectors.counting()));
    }

    private Set<Long> collectUserIds(List<ResidentGroup> groups) {
        Set<Long> ids = new HashSet<>();
        for (ResidentGroup g : groups) {
            if (g.getManagerDoctorId() != null) ids.add(g.getManagerDoctorId());
            if (g.getCreatedBy() != null) ids.add(g.getCreatedBy());
        }
        return ids;
    }

    private Map<Long, HealthProfile> loadProfiles(List<ResidentGroupMember> members) {
        List<Long> ids = members.stream().map(ResidentGroupMember::getResidentId).distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return healthProfileService.listByIds(ids).stream()
                .collect(Collectors.toMap(HealthProfile::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, String> loadUserNames(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(ids.stream().filter(Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, User::displayName, (a, b) -> a));
    }

    private ResidentGroupVO toVO(ResidentGroup g, Long memberCount,
                                 Map<Long, String> communityNames, Map<Long, String> userNames) {
        ResidentGroupVO vo = new ResidentGroupVO();
        vo.setId(g.getId());
        vo.setGroupName(g.getGroupName());
        vo.setGroupCode(g.getGroupCode());
        vo.setDescription(g.getDescription());
        vo.setCommunityId(g.getCommunityId());
        vo.setCommunityName(communityNames.get(g.getCommunityId()));
        vo.setManagerDoctorId(g.getManagerDoctorId());
        vo.setManagerDoctorName(g.getManagerDoctorId() == null ? null : userNames.get(g.getManagerDoctorId()));
        vo.setCreatedBy(g.getCreatedBy());
        vo.setCreatorName(g.getCreatedBy() == null ? null : userNames.get(g.getCreatedBy()));
        vo.setStatus(g.getStatus());
        vo.setStatusLabel(GroupStatus.labelOf(g.getStatus()));
        vo.setMemberCount(memberCount);
        vo.setCreateTime(g.getCreateTime());
        vo.setUpdateTime(g.getUpdateTime());
        return vo;
    }

    private ResidentGroupMemberVO toMemberVO(ResidentGroupMember m, HealthProfile p,
                                             Map<Long, String> communityNames, Map<Long, String> userNames) {
        ResidentGroupMemberVO vo = new ResidentGroupMemberVO();
        vo.setId(m.getId());
        vo.setGroupId(m.getGroupId());
        vo.setResidentId(m.getResidentId());
        if (p != null) {
            vo.setResidentNo(p.getResidentNo());
            vo.setResidentName(StringUtils.hasText(p.getRealName()) ? p.getRealName() : p.getResidentNo());
            vo.setGender(p.getGender());
            vo.setGenderLabel(Gender.labelOf(p.getGender()));
            vo.setCommunityName(communityNames.get(p.getCommunityId()));
        }
        vo.setRemark(m.getRemark());
        vo.setAddedBy(m.getAddedBy());
        vo.setAddedByName(m.getAddedBy() == null ? null : userNames.get(m.getAddedBy()));
        vo.setAddedAt(m.getCreateTime());
        return vo;
    }

    // ============================ 工具 ============================

    private ResidentGroup getByIdRequired(Long id) {
        ResidentGroup group = getById(id);
        if (group == null) {
            throw new BusinessException(404, "分组不存在");
        }
        return group;
    }

    private void assertManaged(Long doctorId, ResidentGroup group) {
        boolean manager = Objects.equals(group.getManagerDoctorId(), doctorId);
        boolean creator = Objects.equals(group.getCreatedBy(), doctorId);
        if (!manager && !creator) {
            throw new BusinessException(403, "该分组不在您的管理范围内");
        }
    }

    private String generateGroupCode() {
        return "G" + LocalDateTime.now().format(CODE_FMT)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
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
