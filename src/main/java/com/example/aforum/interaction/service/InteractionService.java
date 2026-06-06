package com.example.aforum.interaction.service;

import com.example.aforum.interaction.vo.FollowStateVO;
import com.example.aforum.interaction.vo.InteractionStateVO;

public interface InteractionService {

    InteractionStateVO likePost(Long userId, Long postId);

    InteractionStateVO unlikePost(Long userId, Long postId);

    InteractionStateVO favoritePost(Long userId, Long postId);

    InteractionStateVO unfavoritePost(Long userId, Long postId);

    InteractionStateVO likeComment(Long userId, Long commentId);

    InteractionStateVO unlikeComment(Long userId, Long commentId);

    FollowStateVO followUser(Long userId, Long followUserId);

    FollowStateVO unfollowUser(Long userId, Long followUserId);

    boolean isPostLiked(Long userId, Long postId);

    boolean isPostFavorited(Long userId, Long postId);

    boolean isCommentLiked(Long userId, Long commentId);

    boolean isFollowing(Long userId, Long followUserId);

    int countPostLikes(Long postId);

    int countPostFavorites(Long postId);

    int countUserFavorites(Long userId);

    int countUserFollowers(Long userId);

    int countUserFollowing(Long userId);
}
