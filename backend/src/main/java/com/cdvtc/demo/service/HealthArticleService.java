package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.dto.ArticleCreateRequest;
import com.cdvtc.demo.dto.ArticleQueryRequest;
import com.cdvtc.demo.dto.ArticleUpdateRequest;
import com.cdvtc.demo.entity.HealthArticle;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.enums.ArticleCategory;
import com.cdvtc.demo.enums.PublishStatus;
import com.cdvtc.demo.mapper.HealthArticleMapper;
import com.cdvtc.demo.mapper.UserMapper;
import com.cdvtc.demo.vo.ArticleVO;
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
 * 健康知识文章。管理员维护（草稿/发布/下架），居民只读已发布。
 * 正文按纯文本/受控 Markdown 处理，前端以文本方式渲染，避免 XSS。
 */
@Service
@RequiredArgsConstructor
public class HealthArticleService extends ServiceImpl<HealthArticleMapper, HealthArticle> {

    private final UserMapper userMapper;

    // ============================ 管理端 ============================

    @Transactional
    public ArticleVO create(Long authorId, ArticleCreateRequest request) {
        ArticleCategory category = ArticleCategory.fromCode(request.getCategory())
                .orElseThrow(() -> new BusinessException(400, "文章分类取值不正确"));
        HealthArticle article = new HealthArticle();
        article.setTitle(request.getTitle().trim());
        article.setSummary(trimOrNull(request.getSummary()));
        article.setContent(request.getContent().trim());
        article.setCategory(category.name());
        article.setCoverUrl(trimOrNull(request.getCoverUrl()));
        article.setAuthorId(authorId);
        article.setPublishStatus(PublishStatus.DRAFT.name());
        save(article);
        return assemble(List.of(article)).get(0);
    }

    @Transactional
    public ArticleVO update(Long id, ArticleUpdateRequest request) {
        HealthArticle article = requireArticle(id);
        ArticleCategory category = ArticleCategory.fromCode(request.getCategory())
                .orElseThrow(() -> new BusinessException(400, "文章分类取值不正确"));
        article.setTitle(request.getTitle().trim());
        article.setSummary(trimOrNull(request.getSummary()));
        article.setContent(request.getContent().trim());
        article.setCategory(category.name());
        article.setCoverUrl(trimOrNull(request.getCoverUrl()));
        // 编辑不修改发布状态与发布时间，仅刷新更新时间（由自动填充处理）
        updateById(article);
        return assemble(List.of(article)).get(0);
    }

    public IPage<ArticleVO> pageForAdmin(ArticleQueryRequest query) {
        LambdaQueryWrapper<HealthArticle> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(HealthArticle::getTitle, query.getKeyword().trim());
        }
        if (StringUtils.hasText(query.getCategory())) {
            ArticleCategory category = ArticleCategory.fromCode(query.getCategory())
                    .orElseThrow(() -> new BusinessException(400, "文章分类取值不正确"));
            wrapper.eq(HealthArticle::getCategory, category.name());
        }
        if (StringUtils.hasText(query.getStatus())) {
            PublishStatus status = PublishStatus.fromCode(query.getStatus())
                    .orElseThrow(() -> new BusinessException(400, "发布状态取值不正确"));
            wrapper.eq(HealthArticle::getPublishStatus, status.name());
        }
        wrapper.orderByDesc(HealthArticle::getUpdateTime).orderByDesc(HealthArticle::getId);
        return toVoPage(page(new Page<>(normalizePage(query.getPage()), normalizeSize(query.getSize())), wrapper));
    }

    public ArticleVO getForAdmin(Long id) {
        return assemble(List.of(requireArticle(id))).get(0);
    }

    @Transactional
    public ArticleVO publish(Long id) {
        HealthArticle article = requireArticle(id);
        if (PublishStatus.PUBLISHED.name().equals(article.getPublishStatus())) {
            throw new BusinessException(409, "文章已是发布状态，请勿重复发布");
        }
        article.setPublishStatus(PublishStatus.PUBLISHED.name());
        article.setPublishedAt(LocalDateTime.now());
        updateById(article);
        return assemble(List.of(article)).get(0);
    }

    @Transactional
    public ArticleVO offline(Long id) {
        HealthArticle article = requireArticle(id);
        if (!PublishStatus.PUBLISHED.name().equals(article.getPublishStatus())) {
            throw new BusinessException(409, "仅已发布的文章可以下架");
        }
        article.setPublishStatus(PublishStatus.OFFLINE.name());
        updateById(article);
        return assemble(List.of(article)).get(0);
    }

    // ============================ 居民端 ============================

    /** 已发布文章，按发布时间倒序。 */
    public IPage<ArticleVO> pagePublished(ArticleQueryRequest query) {
        LambdaQueryWrapper<HealthArticle> wrapper = new LambdaQueryWrapper<HealthArticle>()
                .eq(HealthArticle::getPublishStatus, PublishStatus.PUBLISHED.name());
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(HealthArticle::getTitle, query.getKeyword().trim());
        }
        if (StringUtils.hasText(query.getCategory())) {
            ArticleCategory category = ArticleCategory.fromCode(query.getCategory())
                    .orElseThrow(() -> new BusinessException(400, "文章分类取值不正确"));
            wrapper.eq(HealthArticle::getCategory, category.name());
        }
        wrapper.orderByDesc(HealthArticle::getPublishedAt).orderByDesc(HealthArticle::getId);
        return toVoPage(page(new Page<>(normalizePage(query.getPage()), normalizeSize(query.getSize())), wrapper));
    }

    public ArticleVO getPublished(Long id) {
        HealthArticle article = getById(id);
        if (article == null || !PublishStatus.PUBLISHED.name().equals(article.getPublishStatus())) {
            // 草稿/已下架对居民统一按不存在处理
            throw new BusinessException(404, "文章不存在或已下架");
        }
        return assemble(List.of(article)).get(0);
    }

    // ============================ 内部 ============================

    private HealthArticle requireArticle(Long id) {
        HealthArticle article = getById(id);
        if (article == null) {
            throw new BusinessException(404, "文章不存在");
        }
        return article;
    }

    private IPage<ArticleVO> toVoPage(IPage<HealthArticle> page) {
        Page<ArticleVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(assemble(page.getRecords()));
        return voPage;
    }

    private List<ArticleVO> assemble(List<HealthArticle> articles) {
        if (articles == null || articles.isEmpty()) {
            return List.of();
        }
        Set<Long> authorIds = articles.stream().map(HealthArticle::getAuthorId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> authors = authorIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        return articles.stream().map(a -> {
            ArticleVO vo = new ArticleVO();
            vo.setId(a.getId());
            vo.setTitle(a.getTitle());
            vo.setSummary(a.getSummary());
            vo.setContent(a.getContent());
            vo.setCategory(a.getCategory());
            vo.setCategoryLabel(ArticleCategory.labelOf(a.getCategory()));
            vo.setCoverUrl(a.getCoverUrl());
            vo.setAuthorId(a.getAuthorId());
            User author = a.getAuthorId() == null ? null : authors.get(a.getAuthorId());
            vo.setAuthorName(author == null ? null : author.displayName());
            vo.setPublishStatus(a.getPublishStatus());
            vo.setPublishStatusLabel(PublishStatus.labelOf(a.getPublishStatus()));
            vo.setPublishedAt(a.getPublishedAt());
            vo.setCreateTime(a.getCreateTime());
            vo.setUpdateTime(a.getUpdateTime());
            return vo;
        }).toList();
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
