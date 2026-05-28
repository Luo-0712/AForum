package com.example.aforum.user.service;

import com.example.aforum.auth.vo.UserInfoVO;
import com.example.aforum.user.dto.UpdateProfileDTO;

public interface UserService {

    UserInfoVO getUserById(Long id);

    void updateProfile(Long userId, UpdateProfileDTO dto);
}
