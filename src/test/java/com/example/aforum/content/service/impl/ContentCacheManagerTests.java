package com.example.aforum.content.service.impl;

import com.example.aforum.common.cache.CacheProperties;
import com.example.aforum.common.cache.RedisCacheService;
import com.example.aforum.common.constant.CacheConstants;
import com.example.aforum.common.result.PageResult;
import com.example.aforum.content.dto.PostQueryDTO;
import com.example.aforum.content.vo.PostSummaryVO;
import com.example.aforum.interaction.entity.PostFavorite;
import com.example.aforum.interaction.entity.PostLike;
import com.example.aforum.interaction.mapper.PostFavoriteMapper;
import com.example.aforum.interaction.mapper.PostLikeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentCacheManagerTests {

    @Mock
    private RedisCacheService cacheService;

    @Mock
    private PostLikeMapper postLikeMapper;

    @Mock
    private PostFavoriteMapper postFavoriteMapper;

    private CacheProperties properties;
    private ContentCacheManager cacheManager;

    @BeforeEach
    void setUp() {
        properties = new CacheProperties();
        cacheManager = new ContentCacheManager(cacheService, properties, postLikeMapper, postFavoriteMapper);
    }

    @Test
    void cachedListHitsDoNotInvokeDatabaseLoaderAndGetUserStateBackfilled() {
        PostQueryDTO query = new PostQueryDTO();
        query.setPageNum(1L);
        query.setPageSize(10L);
        query.setSort("hot");

        PostSummaryVO first = PostSummaryVO.builder().id(10L).liked(null).favorited(null).build();
        PostSummaryVO second = PostSummaryVO.builder().id(11L).liked(null).favorited(null).build();
        PageResult<PostSummaryVO> cached =
                new PageResult<>(List.of(first, second), 2L, 10L, 1L);

        when(cacheService.readGeneration(CacheConstants.POST_GENERATION_KEY)).thenReturn(1L);
        when(cacheService.buildHashedKey(eq(CacheConstants.HOT_POSTS_CACHE), eq(1L), eq(1L), eq(10L),
                eq("hot"), eq(0L), eq(0L), eq(0L))).thenReturn("cached-list-key");
        when(cacheService.get(eq("cached-list-key"), any()))
                .thenReturn(new RedisCacheService.CacheLookup<>(RedisCacheService.State.HIT, cached));
        when(postLikeMapper.selectList(any()))
                .thenReturn(List.of(like(11L)));
        when(postFavoriteMapper.selectList(any()))
                .thenReturn(List.of(favorite(10L)));

        PageResult<PostSummaryVO> result = cacheManager.getList(query, 7L, () -> {
            throw new IllegalStateException("cache hit must not load posts");
        });

        assertThat(result.getRecords())
                .satisfiesExactly(
                        record -> {
                            assertThat(record.getId()).isEqualTo(10L);
                            assertThat(record.getLiked()).isFalse();
                            assertThat(record.getFavorited()).isTrue();
                        },
                        record -> {
                            assertThat(record.getId()).isEqualTo(11L);
                            assertThat(record.getLiked()).isTrue();
                            assertThat(record.getFavorited()).isFalse();
                        });
    }

    @Test
    void cacheMissLoadsDatabaseResultAndRemovesSharedPersonalStateBeforeStorage() {
        PostQueryDTO query = new PostQueryDTO();
        query.setPageNum(1L);
        query.setPageSize(10L);

        PostSummaryVO summary = PostSummaryVO.builder()
                .id(21L)
                .liked(Boolean.TRUE)
                .favorited(Boolean.FALSE)
                .build();
        PageResult<PostSummaryVO> loaded = new PageResult<>(List.of(summary), 1L, 10L, 1L);

        when(cacheService.readGeneration(CacheConstants.POST_GENERATION_KEY)).thenReturn(4L);
        when(cacheService.buildHashedKey(eq(CacheConstants.HOT_POSTS_CACHE), eq(4L), eq(1L),
                eq(10L), eq(""), eq(0L), eq(0L), eq(0L))).thenReturn("miss-key");
        when(cacheService.get(eq("miss-key"), any()))
                .thenReturn(new RedisCacheService.CacheLookup<>(RedisCacheService.State.MISS, null));

        PageResult<PostSummaryVO> result = cacheManager.getList(query, null, () -> loaded);

        assertThat(result).isSameAs(loaded);
        assertThat(result.getRecords().get(0).getLiked()).isNull();
        assertThat(result.getRecords().get(0).getFavorited()).isNull();
        verify(cacheService).putWithJitter(eq("miss-key"), same(loaded), anyLong());
    }

    @Test
    void redisGenerationFailureFallsThroughWithoutCaching() {
        when(cacheService.readGeneration(anyString())).thenReturn(-1L);
        PostSummaryVO summary = PostSummaryVO.builder().id(31L).liked(null).favorited(null).build();
        PageResult<PostSummaryVO> loaded = new PageResult<>(List.of(summary), 1L, 10L, 1L);

        PageResult<PostSummaryVO> result = cacheManager.getList(new PostQueryDTO(), null, () -> loaded);

        assertThat(result.getRecords()).containsExactly(summary);
        verify(cacheService, org.mockito.Mockito.never())
                .putWithJitter(anyString(), any(), anyLong());
    }

    private PostLike like(Long postId) {
        PostLike entity = new PostLike();
        entity.setPostId(postId);
        entity.setUserId(7L);
        entity.setDeleted(0);
        return entity;
    }

    private PostFavorite favorite(Long postId) {
        PostFavorite entity = new PostFavorite();
        entity.setPostId(postId);
        entity.setUserId(7L);
        entity.setDeleted(0);
        return entity;
    }
}
