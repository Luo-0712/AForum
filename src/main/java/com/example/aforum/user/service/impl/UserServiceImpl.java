package com.example.aforum.user.service.impl;

import com.example.aforum.auth.vo.UserInfoVO;
import com.example.aforum.auth.vo.UserInfoVOMapper;
import com.example.aforum.common.exception.BusinessException;
import com.example.aforum.common.result.ErrorCode;
import com.example.aforum.user.dto.UpdateProfileDTO;
import com.example.aforum.user.entity.User;
import com.example.aforum.user.mapper.UserMapper;
import com.example.aforum.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserInfoVOMapper userInfoVOMapper;

    @Override
    public UserInfoVO getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return userInfoVOMapper.toVO(user);
    }

    @Override
    public void updateProfile(Long userId, UpdateProfileDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getBio() != null) {
            user.setBio(dto.getBio());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        userMapper.updateById(user);
    }
}
