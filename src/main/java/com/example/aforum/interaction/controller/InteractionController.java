package com.example.aforum.interaction.controller;

import com.example.aforum.common.result.Result;
import com.example.aforum.common.util.SecurityUtils;
import com.example.aforum.interaction.service.InteractionService;
import com.example.aforum.interaction.vo.FollowStateVO;
import com.example.aforum.interaction.vo.InteractionStateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    @PostMapping("/posts/{id}/like")
    public Result<InteractionStateVO> likePost(@PathVariable Long id) {
        return Result.success(interactionService.likePost(SecurityUtils.getCurrentUserId(), id));
    }

    @DeleteMapping("/posts/{id}/like")
    public Result<InteractionStateVO> unlikePost(@PathVariable Long id) {
        return Result.success(interactionService.unlikePost(SecurityUtils.getCurrentUserId(), id));
    }

    @PostMapping("/posts/{id}/favorite")
    public Result<InteractionStateVO> favoritePost(@PathVariable Long id) {
        return Result.success(interactionService.favoritePost(SecurityUtils.getCurrentUserId(), id));
    }

    @DeleteMapping("/posts/{id}/favorite")
    public Result<InteractionStateVO> unfavoritePost(@PathVariable Long id) {
        return Result.success(interactionService.unfavoritePost(SecurityUtils.getCurrentUserId(), id));
    }

    @PostMapping("/comments/{id}/like")
    public Result<InteractionStateVO> likeComment(@PathVariable Long id) {
        return Result.success(interactionService.likeComment(SecurityUtils.getCurrentUserId(), id));
    }

    @DeleteMapping("/comments/{id}/like")
    public Result<InteractionStateVO> unlikeComment(@PathVariable Long id) {
        return Result.success(interactionService.unlikeComment(SecurityUtils.getCurrentUserId(), id));
    }

    @PostMapping("/users/{id}/follow")
    public Result<FollowStateVO> followUser(@PathVariable Long id) {
        return Result.success(interactionService.followUser(SecurityUtils.getCurrentUserId(), id));
    }

    @DeleteMapping("/users/{id}/follow")
    public Result<FollowStateVO> unfollowUser(@PathVariable Long id) {
        return Result.success(interactionService.unfollowUser(SecurityUtils.getCurrentUserId(), id));
    }
}
