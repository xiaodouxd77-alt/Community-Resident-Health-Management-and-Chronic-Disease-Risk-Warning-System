package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.dto.NoticeCreateRequest;
import com.cdvtc.demo.dto.NoticeQueryRequest;
import com.cdvtc.demo.dto.NoticeUpdateRequest;
import com.cdvtc.demo.entity.SystemNotice;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.enums.PublishStatus;
import com.cdvtc.demo.mapper.SystemNoticeMapper;
import com.cdvtc.demo.mapper.UserMapper;
import com.cdvtc.demo.vo.NoticeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 社区公告。管理员维护（草稿/发布/下架），居民只读已发布且处于有效期内的公告。
 */
@Service
@RequiredArgsConstructor
public class NoticeService extends ServiceImpl<SystemNoticeMapper, SystemNotice> {

    private final UserMapper userMapper;

    // ============================ 管理端 ============================

    @Transactional
    public NoticeVO create(Long publisherId, NoticeCreateRequest request) {
        validateWindow(request.getEffectiveFrom(), request.getEffectiveUntil());
        SystemNotice notice = new SystemNotice();
        notice.setTitle(request.getTitle().trim());
        notice.setContent(request.getContent().trim());
        notice.setPublisherId(publisherId);
        notice.setPublishStatus(PublishStatus.DRAFT.name());
        notice.setEffectiveFrom(request.getEffectiveFrom());
        notice.setEffectiveUntil(request.getEffectiveUntil());
        notice.setIsTop(toFlag(request.getIsTop()));
        save(notice);
        return assemble(List.of(notice)).get(0);
    }

    @Transactional
    public NoticeVO update(Long id, NoticeUpdateRequest request) {
        SystemNotice notice = requireNotice(id);
        validateWindow(request.getEffectiveFrom(), request.getEffectiveUntil());
        notice.setTitle(request.getTitle().trim());
        notice.setContent(request.getContent().trim());
        notice.setEffectiveFrom(request.getEffectiveFrom());
        notice.setEffectiveUntil(request.getEffectiveUntil());
        notice.setIsTop(toFlag(request.getIsTop()));
        // 编辑不修改发布状态与发布时间
        updateById(notice);
        return assemble(List.of(notice)).get(0);
    }

    public IPage<NoticeVO> pageForAdmin(NoticeQueryRequest query) {
        LambdaQueryWrapper<SystemNotice> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(SystemNotice::getTitle, query.getKeyword().trim());
        }
        if (StringUtils.hasText(query.getStatus())) {
            PublishStatus status = PublishStatus.fromCode(query.getStatus())
                    .orElseThrow(() -> new BusinessException(400, "发布状态取值不正确"));
            wrapper.eq(SystemNotice::getPublishStatus, status.name());
        }
        wrapper.orderByDesc(SystemNotice::getIsTop)
                .orderByDesc(SystemNotice::getUpdateTime)
                .orderByDesc(SystemNotice::getId);
        return toVoPage(page(new Page<>(normalizePage(query.getPage()), normalizeSize(query.getSize())), wrapper));
    }

    public NoticeVO getForAdmin(Long id) {
        return assemble(List.of(requireNotice(id))).get(0);
    }

    @Transactional
    public NoticeVO publish(Long id) {
        SystemNotice notice = requireNotice(id);
        if (PublishStatus.PUBLISHED.name().equals(notice.getPublishStatus())) {
            throw new BusinessException(409, "公告已是发布状态，请勿重复发布");
        }
        notice.setPublishStatus(PublishStatus.PUBLISHED.name());
        notice.setPublishedAt(LocalDateTime.now());
        updateById(notice);
        return assemble(List.of(notice)).get(0);
    }

    @Transactional
    public NoticeVO offline(Long id) {
        SystemNotice notice = requireNotice(id);
        if (!PublishStatus.PUBLISHED.name().equals(notice.getPublishStatus())) {
            throw new BusinessException(409, "仅已发布的公告可以下架");
        }
        notice.setPublishStatus(PublishStatus.OFFLINE.name());
        updateById(notice);
        return assemble(List.of(notice)).get(0);
    }

    // ============================ 居民端 ============================

    /** 当前有效（已发布且在有效期内）的公告，置顶优先、发布时间倒序。 */
    public IPage<NoticeVO> pageEffective(NoticeQueryRequest query) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<SystemNotice> wrapper = effectiveWrapper(now);
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(SystemNotice::getTitle, query.getKeyword().trim());
        }
        wrapper.orderByDesc(SystemNotice::getIsTop)
                .orderByDesc(SystemNotice::getPublishedAt)
                .orderByDesc(SystemNotice::getId);
        return toVoPage(page(new Page<>(normalizePage(query.getPage()), normalizeSize(query.getSize())), wrapper));
    }

    public NoticeVO getEffective(Long id) {
        SystemNotice notice = getById(id);
        if (notice == null || !isEffective(notice, LocalDateTime.now())) {
            // 非发布状态或不在有效期，统一按不存在处理
            throw new BusinessException(404, "公告不存在或已失效");
        }
        return assemble(List.of(notice)).get(0);
    }

    // ============================ 内部 ============================

    private LambdaQueryWrapper<SystemNotice> effectiveWrapper(LocalDateTime now) {
        return new LambdaQueryWrapper<SystemNotice>()
                .eq(SystemNotice::getPublishStatus, PublishStatus.PUBLISHED.name())
                .and(w -> w.isNull(SystemNotice::getEffectiveFrom)
                        .or().le(SystemNotice::getEffectiveFrom, now))
                .and(w -> w.isNull(SystemNotice::getEffectiveUntil)
                        .or().ge(SystemNotice::getEffectiveUntil, now));
    }

    private boolean isEffective(SystemNotice notice, LocalDateTime now) {
        if (!PublishStatus.PUBLISHED.name().equals(notice.getPublishStatus())) {
            return false;
        }
        if (notice.getEffectiveFrom() != null && notice.getEffectiveFrom().isAfter(now)) {
            return false;
        }
        return notice.getEffectiveUntil() == null || !notice.getEffectiveUntil().isBefore(now);
    }

    private void validateWindow(LocalDateTime from, LocalDateTime until) {
        if (from != null && until != null && from.isAfter(until)) {
            throw new BusinessException(400, "生效开始时间不能晚于生效结束时间");
        }
    }

    private SystemNotice requireNotice(Long id) {
        SystemNotice notice = getById(id);
        if (notice == null) {
            throw new BusinessException(404, "公告不存在");
        }
        return notice;
    }

    private Integer toFlag(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    private IPage<NoticeVO> toVoPage(IPage<SystemNotice> page) {
        Page<NoticeVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(assemble(page.getRecords()));
        return voPage;
    }

    private List<NoticeVO> assemble(List<SystemNotice> notices) {
        if (notices == null || notices.isEmpty()) {
            return List.of();
        }
        Set<Long> publisherIds = notices.stream().map(SystemNotice::getPublisherId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> publishers = publisherIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(publisherIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        LocalDateTime now = LocalDateTime.now();
        return notices.stream().map(n -> {
            NoticeVO vo = new NoticeVO();
            vo.setId(n.getId());
            vo.setTitle(n.getTitle());
            vo.setContent(n.getContent());
            vo.setPublisherId(n.getPublisherId());
            User publisher = n.getPublisherId() == null ? null : publishers.get(n.getPublisherId());
            vo.setPublisherName(publisher == null ? null : publisher.displayName());
            vo.setPublishStatus(n.getPublishStatus());
            vo.setPublishStatusLabel(PublishStatus.labelOf(n.getPublishStatus()));
            vo.setPublishedAt(n.getPublishedAt());
            vo.setEffectiveFrom(n.getEffectiveFrom());
            vo.setEffectiveUntil(n.getEffectiveUntil());
            vo.setIsTop(n.getIsTop() != null && n.getIsTop() == 1);
            vo.setExpired(n.getEffectiveUntil() != null && n.getEffectiveUntil().isBefore(now));
            vo.setCreateTime(n.getCreateTime());
            vo.setUpdateTime(n.getUpdateTime());
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
