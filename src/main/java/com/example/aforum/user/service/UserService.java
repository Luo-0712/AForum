package com.example.aforum.user.service;

import com.example.aforum.auth.vo.UserInfoVO;
import com.example.aforum.common.request.PageRequest;
import com.example.aforum.common.result.PageResult;
import com.example.aforum.user.dto.UpdateProfileDTO;

public interface UserService {

    UserInfoVO getUserById(Long currentUserId, Long id);

    void updateProfile(Long userId, UpdateProfileDTO dto);

    PageResult<UserInfoVO> listFollowing(Long currentUserId, Long userId, PageRequest request);

    PageResult<UserInfoVO> listFollowers(Long currentUserId, Long userId, PageRequest request);
}
