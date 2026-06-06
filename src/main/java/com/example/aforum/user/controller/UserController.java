package com.example.aforum.user.controller;

import com.example.aforum.auth.service.AuthService;
import com.example.aforum.auth.vo.UserInfoVO;
import com.example.aforum.common.request.PageRequest;
import com.example.aforum.common.result.PageResult;
import com.example.aforum.common.result.Result;
import com.example.aforum.common.util.SecurityUtils;
import com.example.aforum.content.service.PostService;
import com.example.aforum.content.vo.PostSummaryVO;
import com.example.aforum.user.dto.UpdateProfileDTO;
import com.example.aforum.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final PostService postService;

    @GetMapping("/me")
    public Result<UserInfoVO> getCurrentUser() {
        return Result.success(authService.getCurrentUserInfo());
    }

    @PutMapping("/me")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        userService.updateProfile(userId, dto);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<UserInfoVO> getUserById(@PathVariable Long id) {
        return Result.success(userService.getUserById(SecurityUtils.getCurrentUserId(), id));
    }

    @GetMapping("/{id}/favorites")
    public Result<PageResult<PostSummaryVO>> listFavorites(@PathVariable Long id, PageRequest request) {
        return Result.success(postService.listFavoritePosts(SecurityUtils.getCurrentUserId(), id, request));
    }

    @GetMapping("/{id}/following")
    public Result<PageResult<UserInfoVO>> listFollowing(@PathVariable Long id, PageRequest request) {
        return Result.success(userService.listFollowing(SecurityUtils.getCurrentUserId(), id, request));
    }

    @GetMapping("/{id}/followers")
    public Result<PageResult<UserInfoVO>> listFollowers(@PathVariable Long id, PageRequest request) {
        return Result.success(userService.listFollowers(SecurityUtils.getCurrentUserId(), id, request));
    }
}
