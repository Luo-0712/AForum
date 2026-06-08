package com.example.aforum.search.controller;

import com.example.aforum.common.result.Result;
import com.example.aforum.search.service.PostSearchIndexService;
import com.example.aforum.search.vo.SearchHealthVO;
import com.example.aforum.search.vo.SearchReindexVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin/search", "/admin/search"})
@RequiredArgsConstructor
public class AdminSearchController {

    private final PostSearchIndexService postSearchIndexService;

    @GetMapping("/health")
    @PreAuthorize("hasAuthority('search:reindex')")
    public Result<SearchHealthVO> health() {
        return Result.success(postSearchIndexService.health());
    }

    @PostMapping("/posts/reindex")
    @PreAuthorize("hasAuthority('search:reindex')")
    public Result<SearchReindexVO> reindexPosts() {
        return Result.success(postSearchIndexService.reindexAllPublishedPosts());
    }
}
