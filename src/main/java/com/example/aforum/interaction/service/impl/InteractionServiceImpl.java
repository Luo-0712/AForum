package com.example.aforum.interaction.service.impl;

import com.example.aforum.common.constant.PostStatus;
import com.example.aforum.common.exception.BusinessException;
import com.example.aforum.common.result.ErrorCode;
import com.example.aforum.content.entity.Comment;
import com.example.aforum.content.entity.Post;
import com.example.aforum.content.mapper.CommentMapper;
import com.example.aforum.content.mapper.PostMapper;
import com.example.aforum.interaction.entity.CommentLike;
import com.example.aforum.interaction.entity.PostFavorite;
import com.example.aforum.interaction.entity.PostLike;
import com.example.aforum.interaction.entity.UserFollow;
import com.example.aforum.interaction.mapper.CommentLikeMapper;
import com.example.aforum.interaction.mapper.PostFavoriteMapper;
import com.example.aforum.interaction.mapper.PostLikeMapper;
import com.example.aforum.interaction.mapper.UserFollowMapper;
import com.example.aforum.interaction.service.InteractionService;
import com.example.aforum.interaction.vo.FollowStateVO;
import com.example.aforum.interaction.vo.InteractionStateVO;
import com.example.aforum.user.entity.User;
import com.example.aforum.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InteractionServiceImpl implements InteractionService {

    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final PostLikeMapper postLikeMapper;
    private final PostFavoriteMapper postFavoriteMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final UserFollowMapper userFollowMapper;

    @Override
    @Transactional
    public InteractionStateVO likePost(Long userId, Long postId) {
        requireUser(userId);
        requireVisiblePost(postId);
        PostLike like = postLikeMapper.selectAny(postId, userId);
        if (like == null) {
            PostLike next = new PostLike();
            next.setPostId(postId);
            next.setUserId(userId);
            next.setDeleted(0);
            postLikeMapper.insert(next);
        } else if (!isActive(like)) {
            postLikeMapper.restore(like.getId());
        }
        int count = syncPostLikeCount(postId);
        return new InteractionStateVO(true, count);
    }

    @Override
    @Transactional
    public InteractionStateVO unlikePost(Long userId, Long postId) {
        requireUser(userId);
        requireVisiblePost(postId);
        PostLike like = postLikeMapper.selectAny(postId, userId);
        if (isActive(like)) {
            postLikeMapper.deleteById(like.getId());
        }
        int count = syncPostLikeCount(postId);
        return new InteractionStateVO(false, count);
    }

    @Override
    @Transactional
    public InteractionStateVO favoritePost(Long userId, Long postId) {
        requireUser(userId);
        requireVisiblePost(postId);
        PostFavorite favorite = postFavoriteMapper.selectAny(postId, userId);
        if (favorite == null) {
            PostFavorite next = new PostFavorite();
            next.setPostId(postId);
            next.setUserId(userId);
            next.setDeleted(0);
            postFavoriteMapper.insert(next);
        } else if (!isActive(favorite)) {
            postFavoriteMapper.restore(favorite.getId());
        }
        int count = syncPostFavoriteCount(postId);
        return new InteractionStateVO(true, count);
    }

    @Override
    @Transactional
    public InteractionStateVO unfavoritePost(Long userId, Long postId) {
        requireUser(userId);
        requireVisiblePost(postId);
        PostFavorite favorite = postFavoriteMapper.selectAny(postId, userId);
        if (isActive(favorite)) {
            postFavoriteMapper.deleteById(favorite.getId());
        }
        int count = syncPostFavoriteCount(postId);
        return new InteractionStateVO(false, count);
    }

    @Override
    @Transactional
    public InteractionStateVO likeComment(Long userId, Long commentId) {
        requireUser(userId);
        requireVisibleComment(commentId);
        CommentLike like = commentLikeMapper.selectAny(commentId, userId);
        if (like == null) {
            CommentLike next = new CommentLike();
            next.setCommentId(commentId);
            next.setUserId(userId);
            next.setDeleted(0);
            commentLikeMapper.insert(next);
        } else if (!isActive(like)) {
            commentLikeMapper.restore(like.getId());
        }
        int count = syncCommentLikeCount(commentId);
        return new InteractionStateVO(true, count);
    }

    @Override
    @Transactional
    public InteractionStateVO unlikeComment(Long userId, Long commentId) {
        requireUser(userId);
        requireVisibleComment(commentId);
        CommentLike like = commentLikeMapper.selectAny(commentId, userId);
        if (isActive(like)) {
            commentLikeMapper.deleteById(like.getId());
        }
        int count = syncCommentLikeCount(commentId);
        return new InteractionStateVO(false, count);
    }

    @Override
    @Transactional
    public FollowStateVO followUser(Long userId, Long followUserId) {
        requireUser(userId);
        requireUser(followUserId);
        if (userId.equals(followUserId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Cannot follow yourself");
        }
        UserFollow follow = userFollowMapper.selectAny(userId, followUserId);
        if (follow == null) {
            UserFollow next = new UserFollow();
            next.setUserId(userId);
            next.setFollowUserId(followUserId);
            next.setDeleted(0);
            userFollowMapper.insert(next);
        } else if (!isActive(follow)) {
            userFollowMapper.restore(follow.getId());
        }
        return followState(userId, followUserId, true);
    }

    @Override
    @Transactional
    public FollowStateVO unfollowUser(Long userId, Long followUserId) {
        requireUser(userId);
        requireUser(followUserId);
        UserFollow follow = userFollowMapper.selectAny(userId, followUserId);
        if (isActive(follow)) {
            userFollowMapper.deleteById(follow.getId());
        }
        return followState(userId, followUserId, false);
    }

    @Override
    public boolean isPostLiked(Long userId, Long postId) {
        if (userId == null || postId == null) {
            return false;
        }
        return isActive(postLikeMapper.selectAny(postId, userId));
    }

    @Override
    public boolean isPostFavorited(Long userId, Long postId) {
        if (userId == null || postId == null) {
            return false;
        }
        return isActive(postFavoriteMapper.selectAny(postId, userId));
    }

    @Override
    public boolean isCommentLiked(Long userId, Long commentId) {
        if (userId == null || commentId == null) {
            return false;
        }
        return isActive(commentLikeMapper.selectAny(commentId, userId));
    }

    @Override
    public boolean isFollowing(Long userId, Long followUserId) {
        if (userId == null || followUserId == null) {
            return false;
        }
        return isActive(userFollowMapper.selectAny(userId, followUserId));
    }

    @Override
    public int countPostLikes(Long postId) {
        return postId == null ? 0 : postLikeMapper.countActiveByPostId(postId);
    }

    @Override
    public int countPostFavorites(Long postId) {
        return postId == null ? 0 : postFavoriteMapper.countActiveByPostId(postId);
    }

    @Override
    public int countUserFavorites(Long userId) {
        return userId == null ? 0 : postFavoriteMapper.countActiveByUserId(userId);
    }

    @Override
    public int countUserFollowers(Long userId) {
        return userId == null ? 0 : userFollowMapper.countFollowers(userId);
    }

    @Override
    public int countUserFollowing(Long userId) {
        return userId == null ? 0 : userFollowMapper.countFollowing(userId);
    }

    private int syncPostLikeCount(Long postId) {
        int count = postLikeMapper.countActiveByPostId(postId);
        postMapper.updateLikeCount(postId, count);
        return count;
    }

    private int syncPostFavoriteCount(Long postId) {
        int count = postFavoriteMapper.countActiveByPostId(postId);
        postMapper.updateFavoriteCount(postId, count);
        return count;
    }

    private int syncCommentLikeCount(Long commentId) {
        int count = commentLikeMapper.countActiveByCommentId(commentId);
        commentMapper.updateLikeCount(commentId, count);
        return count;
    }

    private FollowStateVO followState(Long userId, Long followUserId, boolean following) {
        return new FollowStateVO(
                following,
                userFollowMapper.countFollowers(followUserId),
                userFollowMapper.countFollowing(userId)
        );
    }

    private Post requireVisiblePost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || !PostStatus.PUBLISHED.getCode().equals(post.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Post not found");
        }
        return post;
    }

    private Comment requireVisibleComment(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() == null || comment.getStatus() != 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Comment not found");
        }
        requireVisiblePost(comment.getPostId());
        return comment;
    }

    private User requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "User not found");
        }
        return user;
    }

    private boolean isActive(PostLike like) {
        return like != null && Integer.valueOf(0).equals(like.getDeleted());
    }

    private boolean isActive(PostFavorite favorite) {
        return favorite != null && Integer.valueOf(0).equals(favorite.getDeleted());
    }

    private boolean isActive(CommentLike like) {
        return like != null && Integer.valueOf(0).equals(like.getDeleted());
    }

    private boolean isActive(UserFollow follow) {
        return follow != null && Integer.valueOf(0).equals(follow.getDeleted());
    }
}
