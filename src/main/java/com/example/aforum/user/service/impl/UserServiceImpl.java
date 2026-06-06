package com.example.aforum.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aforum.auth.vo.UserInfoVO;
import com.example.aforum.auth.vo.UserInfoVOMapper;
import com.example.aforum.common.constant.PostStatus;
import com.example.aforum.common.exception.BusinessException;
import com.example.aforum.common.request.PageRequest;
import com.example.aforum.common.result.ErrorCode;
import com.example.aforum.common.result.PageResult;
import com.example.aforum.content.entity.Post;
import com.example.aforum.content.mapper.PostMapper;
import com.example.aforum.interaction.entity.UserFollow;
import com.example.aforum.interaction.mapper.UserFollowMapper;
import com.example.aforum.interaction.service.InteractionService;
import com.example.aforum.user.dto.UpdateProfileDTO;
import com.example.aforum.user.entity.User;
import com.example.aforum.user.mapper.UserMapper;
import com.example.aforum.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int MAX_PAGE_SIZE = 50;

    private final UserMapper userMapper;
    private final UserInfoVOMapper userInfoVOMapper;
    private final PostMapper postMapper;
    private final UserFollowMapper userFollowMapper;
    private final InteractionService interactionService;

    @Override
    public UserInfoVO getUserById(Long currentUserId, Long id) {
        return toProfileVO(requireUser(id), currentUserId);
    }

    @Override
    public void updateProfile(Long userId, UpdateProfileDTO dto) {
        User user = requireUser(userId);
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

    @Override
    public PageResult<UserInfoVO> listFollowing(Long currentUserId, Long userId, PageRequest request) {
        requireUser(userId);
        long pageNum = sanitizePageNum(request.getPageNum());
        long pageSize = sanitizePageSize(request.getPageSize());
        Page<UserFollow> page = userFollowMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getUserId, userId)
                        .orderByDesc(UserFollow::getCreatedAt));
        return new PageResult<>(
                page.getRecords().stream()
                        .map(follow -> userMapper.selectById(follow.getFollowUserId()))
                        .filter(user -> user != null)
                        .map(user -> toProfileVO(user, currentUserId))
                        .toList(),
                page.getTotal(),
                page.getSize(),
                page.getCurrent()
        );
    }

    @Override
    public PageResult<UserInfoVO> listFollowers(Long currentUserId, Long userId, PageRequest request) {
        requireUser(userId);
        long pageNum = sanitizePageNum(request.getPageNum());
        long pageSize = sanitizePageSize(request.getPageSize());
        Page<UserFollow> page = userFollowMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowUserId, userId)
                        .orderByDesc(UserFollow::getCreatedAt));
        return new PageResult<>(
                page.getRecords().stream()
                        .map(follow -> userMapper.selectById(follow.getUserId()))
                        .filter(user -> user != null)
                        .map(user -> toProfileVO(user, currentUserId))
                        .toList(),
                page.getTotal(),
                page.getSize(),
                page.getCurrent()
        );
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "User not found");
        }
        return user;
    }

    private UserInfoVO toProfileVO(User user, Long currentUserId) {
        UserInfoVO vo = userInfoVOMapper.toVO(user);
        Long userId = user.getId();
        vo.setPostCount(Math.toIntExact(postMapper.selectCount(new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId)
                .eq(Post::getStatus, PostStatus.PUBLISHED.getCode()))));
        vo.setFavoriteCount(interactionService.countUserFavorites(userId));
        vo.setFollowingCount(interactionService.countUserFollowing(userId));
        vo.setFollowerCount(interactionService.countUserFollowers(userId));
        vo.setFollowedByCurrentUser(interactionService.isFollowing(currentUserId, userId));
        return vo;
    }

    private long sanitizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    private long sanitizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
