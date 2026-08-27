package com.example.aforum.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aforum.common.cache.CacheProperties;
import com.example.aforum.common.cache.RedisCacheService;
import com.example.aforum.common.constant.CacheConstants;
import com.example.aforum.common.exception.BusinessException;
import com.example.aforum.common.result.ErrorCode;
import com.example.aforum.common.result.PageResult;
import com.example.aforum.content.dto.PostQueryDTO;
import com.example.aforum.interaction.entity.PostFavorite;
import com.example.aforum.interaction.entity.PostLike;
import com.example.aforum.interaction.mapper.PostFavoriteMapper;
import com.example.aforum.interaction.mapper.PostLikeMapper;
import com.example.aforum.content.vo.PostDetailVO;
import com.example.aforum.content.vo.PostSummaryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentCacheManager {

    private static final int MAX_CACHEABLE_PAGE = 5;

    private final RedisCacheService cacheService;
    private final CacheProperties cacheProperties;
    private final PostLikeMapper postLikeMapper;
    private final PostFavoriteMapper postFavoriteMapper;

    public PageResult<PostSummaryVO> getList(PostQueryDTO query,
                                             Long currentUserId,
                                             Supplier<PageResult<PostSummaryVO>> loader) {
        if (!isCacheable(query)) {
            return loader.get();
        }

        long generation = postGeneration();
        if (generation < 0) {
            return personalize(loader.get(), currentUserId);
        }

        String key = cacheService.buildHashedKey(
                CacheConstants.HOT_POSTS_CACHE,
                generation,
                sanitizeLong(query.getPageNum()),
                sanitizeLong(query.getPageSize()),
                StringUtils.hasText(query.getSort()) ? query.getSort() : "",
                sanitizeLong(query.getCategoryId()),
                sanitizeLong(query.getBoardId()),
                sanitizeLong(query.getUserId()));

        @SuppressWarnings({"unchecked", "rawtypes"})
        Class<PageResult<PostSummaryVO>> pageResultType = (Class) PageResult.class;
        RedisCacheService.CacheLookup<PageResult<PostSummaryVO>> lookup =
                cacheService.get(key, pageResultType);
        if (lookup.hit()) {
            return personalize(lookup.value(), currentUserId);
        }
        if (lookup.negativeHit()) {
            return new PageResult<>(List.of(), 0L, sanitizeLong(query.getPageSize()), sanitizeLong(query.getPageNum()));
        }

        PageResult<PostSummaryVO> loaded = loader.get();
        clearPersonalState(loaded);
        cacheService.putWithJitter(
                key,
                loaded,
                cacheProperties.getPostListTtlSeconds());
        return personalize(loaded, currentUserId);
    }

    public PostDetailVO getDetail(Long currentUserId, Long postId, Supplier<PostDetailVO> loader) {
        long generation = postGeneration();
        if (generation >= 0) {
            String key = detailKey(generation, postId);
            RedisCacheService.CacheLookup<PostDetailVO> lookup =
                    cacheService.get(key, PostDetailVO.class);
            if (lookup.hit()) {
                return personalizeDetail(lookup.value(), currentUserId);
            }
            if (lookup.negativeHit()) {
                throw notFound();
            }
            try {
                PostDetailVO detail = loader.get();
                cacheService.putWithJitter(key, detail, cacheProperties.getPostDetailTtlSeconds());
                return personalizeDetail(detail, currentUserId);
            } catch (BusinessException ex) {
                if (ErrorCode.NOT_FOUND.getCode().equals(ex.getCode())) {
                    cacheService.putNegativeWithJitter(key, cacheProperties.getNegativeTtlSeconds());
                }
                throw ex;
            }
        }
        return personalizeDetail(loader.get(), currentUserId);
    }

    private PostDetailVO personalizeDetail(PostDetailVO detail, Long currentUserId) {
        if (detail == null || detail.getId() == null || currentUserId == null) {
            return detail;
        }

        Long postId = detail.getId();
        detail.setLiked(!postLikeMapper.selectList(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId)
                .eq(PostLike::getUserId, currentUserId)
                .eq(PostLike::getDeleted, 0)
                .last("LIMIT 1"))
                .isEmpty());
        detail.setFavorited(!postFavoriteMapper.selectList(new LambdaQueryWrapper<PostFavorite>()
                .eq(PostFavorite::getPostId, postId)
                .eq(PostFavorite::getUserId, currentUserId)
                .eq(PostFavorite::getDeleted, 0)
                .last("LIMIT 1"))
                .isEmpty());
        return detail;
    }

    private String detailKey(long generation, Long postId) {
        return cacheService.buildDirectKey(
                CacheConstants.POST_CACHE_PREFIX + ":" + generation + ":detail",
                postId);
    }

    public void bumpPostGeneration() {
        if (!cacheService.isEnabled()) {
            return;
        }
        cacheService.incrementAfterCommit(() -> incrementSafely());
    }

    public void evictSearchOptions() {
        if (!cacheService.isEnabled()) {
            return;
        }
        cacheService.incrementAfterCommit(() ->
                cacheService.delete(cacheService.buildDirectKey(CacheConstants.SEARCH_OPTIONS_CACHE_KEY, "v1")));
    }

    private void incrementSafely() {
        try {
            cacheService.incrementGeneration(CacheConstants.POST_GENERATION_KEY);
        } catch (Exception ex) {
            log.warn("Failed to bump Redis post generation", ex);
        }
    }

    private long postGeneration() {
        return cacheService.readGeneration(CacheConstants.POST_GENERATION_KEY);
    }

    private PageResult<PostSummaryVO> personalize(PageResult<PostSummaryVO> page, Long currentUserId) {
        List<PostSummaryVO> records = page.getRecords();
        if (currentUserId == null || records == null || records.isEmpty()) {
            return page;
        }

        List<Long> postIds = records.stream()
                .map(PostSummaryVO::getId)
                .filter(id -> id != null)
                .toList();
        if (postIds.isEmpty()) {
            return page;
        }

        Set<Long> likedIds = activePostIds(postLikeMapper.selectList(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getUserId, currentUserId)
                .eq(PostLike::getDeleted, 0)
                .in(PostLike::getPostId, postIds)), PostLike::getPostId);
        Set<Long> favoritedIds = activePostIds(postFavoriteMapper.selectList(new LambdaQueryWrapper<PostFavorite>()
                .eq(PostFavorite::getUserId, currentUserId)
                .eq(PostFavorite::getDeleted, 0)
                .in(PostFavorite::getPostId, postIds)), PostFavorite::getPostId);

        records.forEach(record -> {
            record.setLiked(likedIds.contains(record.getId()));
            record.setFavorited(favoritedIds.contains(record.getId()));
        });
        return page;
    }

    private <T> Set<Long> activePostIds(List<T> entities, Function<T, Long> postIdGetter) {
        Set<Long> ids = new HashSet<>();
        for (T entity : entities) {
            Long postId = postIdGetter.apply(entity);
            if (postId != null) {
                ids.add(postId);
            }
        }
        return ids;
    }

    private void clearPersonalState(PageResult<PostSummaryVO> page) {
        if (page.getRecords() == null) {
            return;
        }
        page.getRecords().forEach(record -> {
            record.setLiked(null);
            record.setFavorited(null);
        });
    }

    private boolean isCacheable(PostQueryDTO query) {
        return query != null
                && !StringUtils.hasText(query.getKeyword())
                && sanitizeLong(query.getPageNum()) <= MAX_CACHEABLE_PAGE;
    }

    private long sanitizeLong(Long value) {
        return value == null ? 0L : value;
    }

    private BusinessException notFound() {
        return new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在或尚未发布");
    }
}
