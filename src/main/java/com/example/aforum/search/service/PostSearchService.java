package com.example.aforum.search.service;

import com.example.aforum.common.result.PageResult;
import com.example.aforum.search.dto.SearchPostQueryDTO;
import com.example.aforum.search.vo.SearchPostVO;

public interface PostSearchService {

    PageResult<SearchPostVO> searchPosts(Long currentUserId, SearchPostQueryDTO query);
}
