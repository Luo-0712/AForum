package com.example.aforum.search.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aforum.common.cache.CacheProperties;
import com.example.aforum.common.cache.RedisCacheService;
import com.example.aforum.common.constant.CacheConstants;
import com.example.aforum.content.entity.Board;
import com.example.aforum.content.entity.Category;
import com.example.aforum.content.mapper.BoardMapper;
import com.example.aforum.content.mapper.CategoryMapper;
import com.example.aforum.search.service.SearchOptionService;
import com.example.aforum.search.vo.SearchOptionVO;
import com.example.aforum.search.vo.SearchOptionsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchOptionServiceImpl implements SearchOptionService {

    private final CategoryMapper categoryMapper;
    private final BoardMapper boardMapper;
    private final RedisCacheService cacheService;
    private final CacheProperties cacheProperties;

    @Override
    public SearchOptionsVO listOptions() {
        if (!cacheService.isEnabled()) {
            return loadOptions();
        }

        String key = cacheService.buildDirectKey(CacheConstants.SEARCH_OPTIONS_CACHE_KEY, "v1");
        RedisCacheService.CacheLookup<SearchOptionsVO> lookup =
                cacheService.get(key, SearchOptionsVO.class);
        if (lookup.hit()) {
            return lookup.value();
        }
        if (lookup.negativeHit()) {
            return SearchOptionsVO.builder().categories(List.of()).boards(List.of()).build();
        }

        SearchOptionsVO options = loadOptions();
        cacheService.putWithJitter(key, options, cacheProperties.getSearchOptionsTtlSeconds());
        return options;
    }

    private SearchOptionsVO loadOptions() {
        return SearchOptionsVO.builder()
                .categories(categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                                .eq(Category::getStatus, 0)
                                .orderByAsc(Category::getSort))
                        .stream()
                        .map(category -> SearchOptionVO.builder()
                                .id(category.getId())
                                .name(category.getName())
                                .build())
                        .toList())
                .boards(boardMapper.selectList(new LambdaQueryWrapper<Board>()
                                .eq(Board::getStatus, 0)
                                .orderByAsc(Board::getSort))
                        .stream()
                        .map(board -> SearchOptionVO.builder()
                                .id(board.getId())
                                .name(board.getName())
                                .build())
                        .toList())
                .build();
    }
}
