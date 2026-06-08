package com.example.aforum.search.controller;

import com.example.aforum.common.result.PageResult;
import com.example.aforum.common.result.Result;
import com.example.aforum.common.util.SecurityUtils;
import com.example.aforum.search.dto.SearchPostQueryDTO;
import com.example.aforum.search.service.SearchOptionService;
import com.example.aforum.search.service.PostSearchService;
import com.example.aforum.search.vo.SearchOptionsVO;
import com.example.aforum.search.vo.SearchPostVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Validated
public class SearchController {

    private final PostSearchService postSearchService;
    private final SearchOptionService searchOptionService;

    @GetMapping
    public Result<PageResult<SearchPostVO>> searchPosts(@Valid SearchPostQueryDTO query) {
        return Result.success(postSearchService.searchPosts(SecurityUtils.getCurrentUserId(), query));
    }

    @GetMapping("/options")
    public Result<SearchOptionsVO> listOptions() {
        return Result.success(searchOptionService.listOptions());
    }
}
