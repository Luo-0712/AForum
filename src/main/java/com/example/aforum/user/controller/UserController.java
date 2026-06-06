package com.example.aforum.user.controller;

import com.example.aforum.auth.service.AuthService;
import com.example.aforum.auth.vo.UserInfoVO;
import com.example.aforum.common.result.Result;
import com.example.aforum.common.util.SecurityUtils;
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
        return Result.success(userService.getUserById(id));
    }
}
