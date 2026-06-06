package com.example.aforum.content.controller;

import com.example.aforum.common.result.PageResult;
import com.example.aforum.common.result.Result;
import com.example.aforum.common.util.SecurityUtils;
import com.example.aforum.content.dto.CreateCommentDTO;
import com.example.aforum.content.dto.CreatePostDTO;
import com.example.aforum.content.dto.PostQueryDTO;
import com.example.aforum.content.dto.UpdatePostDTO;
import com.example.aforum.content.service.PostService;
import com.example.aforum.content.vo.CommentVO;
import com.example.aforum.content.vo.PostDetailVO;
import com.example.aforum.content.vo.PostSummaryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public Result<PageResult<PostSummaryVO>> listPosts(PostQueryDTO query) {
        return Result.success(postService.listPosts(SecurityUtils.getCurrentUserId(), query));
    }

    @GetMapping("/{id}")
    public Result<PostDetailVO> getPostDetail(@PathVariable Long id) {
        return Result.success(postService.getPostDetail(SecurityUtils.getCurrentUserId(), id, true));
    }

    @PostMapping
    public Result<PostDetailVO> createPost(@Valid @RequestBody CreatePostDTO dto) {
        return Result.success(postService.createPost(SecurityUtils.getCurrentUserId(), dto));
    }

    @PutMapping("/{id}")
    public Result<PostDetailVO> updatePost(@PathVariable Long id, @Valid @RequestBody UpdatePostDTO dto) {
        return Result.success(postService.updatePost(SecurityUtils.getCurrentUserId(), id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(SecurityUtils.getCurrentUserId(), id);
        return Result.success();
    }

    @GetMapping("/{postId}/comments")
    public Result<List<CommentVO>> listComments(@PathVariable Long postId) {
        return Result.success(postService.listComments(SecurityUtils.getCurrentUserId(), postId));
    }

    @PostMapping("/{postId}/comments")
    public Result<CommentVO> createComment(@PathVariable Long postId, @Valid @RequestBody CreateCommentDTO dto) {
        return Result.success(postService.createComment(SecurityUtils.getCurrentUserId(), postId, dto));
    }
}
