package com.example.aforum.search.service.impl;

import com.example.aforum.common.result.PageResult;
import com.example.aforum.search.dto.SearchPostQueryDTO;
import com.example.aforum.search.service.PostSearchService;
import com.example.aforum.search.vo.SearchPostVO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "false", matchIfMissing = false)
public class MysqlOnlyPostSearchService implements PostSearchService {

    private final MysqlPostSearchFallback mysqlPostSearchFallback;

    @Override
    public PageResult<SearchPostVO> searchPosts(Long currentUserId, SearchPostQueryDTO query) {
        return mysqlPostSearchFallback.search(currentUserId, query);
    }
}
