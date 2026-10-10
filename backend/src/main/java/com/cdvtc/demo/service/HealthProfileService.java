package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.dto.HealthProfileUpdateRequest;
import com.cdvtc.demo.dto.ResidentQueryRequest;
import com.cdvtc.demo.entity.HealthProfile;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.enums.Gender;
import com.cdvtc.demo.mapper.HealthProfileMapper;
import com.cdvtc.demo.mapper.UserMapper;
import com.cdvtc.demo.vo.HealthProfileVO;
import com.cdvtc.demo.vo.ResidentBriefVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HealthProfileService extends ServiceImpl<HealthProfileMapper, HealthProfile> {

    private static final DateTimeFormatter NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final UserMapper userMapper;
    private final CommunityService communityService;

    public HealthProfile getByUserId(Long userId) {
        return lambdaQuery().eq(HealthProfile::getUserId, userId).one();
    }

    public HealthProfile getByIdRequired(Long id) {
        HealthProfile profile = getById(id);
        if (profile == null) {
            throw new BusinessException(404, "居民档案不存在");
        }
        return profile;
    }

    /** 读取居民登录信息（用户名/电话），供医生端详情展示。 */
    public User loadUser(Long userId) {
        return userId == null ? null : userMapper.selectById(userId);
    }

    /**
     * 获取当前用户的居民档案，不存在时自动创建一条最小档案，
     * 使居民首次录入指标无需额外建档步骤。
     */
    @Transactional
    public HealthProfile getOrCreate(User user) {
        HealthProfile existing = getByUserId(user.getId());
        if (existing != null) {
            return existing;
        }
        HealthProfile profile = new HealthProfile();
        profile.setUserId(user.getId());
        profile.setResidentNo(generateResidentNo());
        profile.setRealName(user.getUsername());
        profile.setGender(Gender.UNKNOWN.name());
        try {
            save(profile);
        } catch (DuplicateKeyException e) {
            return getByUserId(user.getId());
        }
        return profile;
    }

    /** 居民显式建档：已存在档案时返回 409。 */
    @Transactional
    public HealthProfileVO createProfile(User user, HealthProfileUpdateRequest request) {
        if (getByUserId(user.getId()) != null) {
            throw new BusinessException(409, "健康档案已存在，请直接编辑");
        }
        HealthProfile profile = new HealthProfile();
        profile.setUserId(user.getId());
        profile.setResidentNo(generateResidentNo());
        profile.setRealName(StringUtils.hasText(request.getRealName())
                ? request.getRealName().trim() : user.getUsername());
        profile.setGender(request.getGender() != null
                ? Gender.fromCode(request.getGender())
                    .orElseThrow(() -> new BusinessException(400, "性别取值不正确")).name()
                : Gender.UNKNOWN.name());
        applyEditableFields(profile, request);
        try {
            save(profile);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(409, "健康档案已存在，请直接编辑");
        }
        return toVO(profile, user);
    }

    @Transactional
    public HealthProfileVO updateProfile(User user, HealthProfileUpdateRequest request) {
        HealthProfile profile = getOrCreate(user);
        if (StringUtils.hasText(request.getRealName())) {
            profile.setRealName(request.getRealName().trim());
        }
        if (StringUtils.hasText(request.getGender())) {
            Gender gender = Gender.fromCode(request.getGender())
                    .orElseThrow(() -> new BusinessException(400, "性别取值不正确"));
            profile.setGender(gender.name());
        }
        applyEditableFields(profile, request);
        updateById(profile);
        return toVO(profile, user);
    }

    private void applyEditableFields(HealthProfile profile, HealthProfileUpdateRequest request) {
        if (request.getBirthDate() != null) {
            if (request.getBirthDate().isAfter(LocalDate.now())) {
                throw new BusinessException(400, "出生日期不能晚于今天");
            }
            profile.setBirthDate(request.getBirthDate());
        }
        if (request.getHeight() != null) {
            profile.setHeight(request.getHeight());
        }
        if (request.getPastHistory() != null) {
            profile.setPastHistory(trimOrNull(request.getPastHistory()));
        }
        if (request.getFamilyHistory() != null) {
            profile.setFamilyHistory(trimOrNull(request.getFamilyHistory()));
        }
        if (request.getAllergyHistory() != null) {
            profile.setAllergyHistory(trimOrNull(request.getAllergyHistory()));
        }
        if (request.getHealthRemark() != null) {
            profile.setHealthRemark(trimOrNull(request.getHealthRemark()));
        }
        if (request.getCommunityId() != null) {
            communityService.requireExists(request.getCommunityId());
            profile.setCommunityId(request.getCommunityId());
        }
    }

    private String trimOrNull(String value) {
        String v = value == null ? null : value.trim();
        return StringUtils.hasText(v) ? v : null;
    }

    // ============================ 组装 ============================

    public HealthProfileVO toVO(HealthProfile profile, User user) {
        String communityName = communityService.nameOf(profile.getCommunityId());
        String verifiedDoctorName = null;
        if (profile.getVerifiedDoctorId() != null) {
            User doctor = userMapper.selectById(profile.getVerifiedDoctorId());
            verifiedDoctorName = doctor == null ? null : doctor.displayName();
        }
        return toVO(profile, user, communityName, verifiedDoctorName);
    }

    private HealthProfileVO toVO(HealthProfile profile, User user, String communityName, String verifiedDoctorName) {
        HealthProfileVO vo = new HealthProfileVO();
        vo.setId(profile.getId());
        vo.setUserId(profile.getUserId());
        vo.setResidentNo(profile.getResidentNo());
        vo.setRealName(profile.getRealName());
        vo.setGender(profile.getGender());
        vo.setGenderLabel(Gender.labelOf(profile.getGender()));
        vo.setBirthDate(profile.getBirthDate());
        vo.setHeight(profile.getHeight());
        vo.setPastHistory(profile.getPastHistory());
        vo.setFamilyHistory(profile.getFamilyHistory());
        vo.setAllergyHistory(profile.getAllergyHistory());
        vo.setHealthRemark(profile.getHealthRemark());
        vo.setVerified(profile.getVerified() != null && profile.getVerified() == 1);
        vo.setVerifiedDoctorId(profile.getVerifiedDoctorId());
        vo.setVerifiedDoctorName(verifiedDoctorName);
        vo.setVerifiedAt(profile.getVerifiedAt());
        vo.setCommunityId(profile.getCommunityId());
        vo.setCommunityName(communityName);
        vo.setCreateTime(profile.getCreateTime());
        vo.setUpdateTime(profile.getUpdateTime());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setPhone(user.getPhone());
        }
        return vo;
    }

    public ResidentBriefVO toBrief(HealthProfile profile, User user) {
        ResidentBriefVO vo = new ResidentBriefVO();
        vo.setId(profile.getId());
        vo.setResidentNo(profile.getResidentNo());
        vo.setRealName(StringUtils.hasText(profile.getRealName())
                ? profile.getRealName()
                : (user != null ? user.getUsername() : null));
        vo.setGender(profile.getGender());
        vo.setGenderLabel(Gender.labelOf(profile.getGender()));
        vo.setBirthDate(profile.getBirthDate());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setPhone(user.getPhone());
        }
        return vo;
    }

    /** 医生端展示其授权范围内的居民简要信息。 */
    public List<ResidentBriefVO> listBriefByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<HealthProfile> profiles = listByIds(ids);
        Map<Long, User> users = loadUsers(profiles);
        return profiles.stream()
                .map(p -> toBrief(p, users.get(p.getUserId())))
                .collect(Collectors.toList());
    }

    /** 医生端分页检索授权范围内的居民。 */
    public IPage<HealthProfileVO> pageResidents(List<Long> scopedResidentIds, ResidentQueryRequest query) {
        int page = normalizePage(query.getPage());
        int size = normalizeSize(query.getSize());
        if (scopedResidentIds == null || scopedResidentIds.isEmpty()) {
            return new Page<>(page, size, 0);
        }
        LambdaQueryWrapper<HealthProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(HealthProfile::getId, scopedResidentIds);
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(q -> q.like(HealthProfile::getResidentNo, kw)
                    .or().like(HealthProfile::getRealName, kw));
        }
        if (query.getCommunityId() != null) {
            wrapper.eq(HealthProfile::getCommunityId, query.getCommunityId());
        }
        wrapper.orderByDesc(HealthProfile::getUpdateTime).orderByDesc(HealthProfile::getId);

        IPage<HealthProfile> profilePage = page(new Page<>(page, size), wrapper);
        List<HealthProfile> records = profilePage.getRecords();
        Map<Long, User> users = loadUsers(records);
        Map<Long, String> communityNames = communityService.nameMap(
                records.stream().map(HealthProfile::getCommunityId).filter(Objects::nonNull).toList());
        Map<Long, String> verifiedNames = loadVerifiedDoctorNames(records);

        List<HealthProfileVO> voList = records.stream()
                .map(p -> toVO(p, users.get(p.getUserId()),
                        communityNames.get(p.getCommunityId()),
                        p.getVerifiedDoctorId() == null ? null : verifiedNames.get(p.getVerifiedDoctorId())))
                .toList();
        Page<HealthProfileVO> voPage = new Page<>(profilePage.getCurrent(), profilePage.getSize(), profilePage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    private Map<Long, User> loadUsers(List<HealthProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(
                        profiles.stream().map(HealthProfile::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, String> loadVerifiedDoctorNames(List<HealthProfile> profiles) {
        List<Long> ids = profiles.stream().map(HealthProfile::getVerifiedDoctorId)
                .filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        int s = size == null ? 10 : size;
        return s < 1 || s > 100 ? 10 : s;
    }

    private String generateResidentNo() {
        return "R" + LocalDate.now().format(NO_DATE)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }
}
