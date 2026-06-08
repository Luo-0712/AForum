package com.example.aforum.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aforum.common.constant.PostStatus;
import com.example.aforum.common.exception.BusinessException;
import com.example.aforum.common.request.PageRequest;
import com.example.aforum.common.result.ErrorCode;
import com.example.aforum.common.result.PageResult;
import com.example.aforum.content.dto.CreateCommentDTO;
import com.example.aforum.content.dto.CreatePostDTO;
import com.example.aforum.content.dto.PostQueryDTO;
import com.example.aforum.content.dto.UpdatePostDTO;
import com.example.aforum.content.entity.Board;
import com.example.aforum.content.entity.Category;
import com.example.aforum.content.entity.Comment;
import com.example.aforum.content.entity.ContentReview;
import com.example.aforum.content.entity.Post;
import com.example.aforum.content.mapper.BoardMapper;
import com.example.aforum.content.mapper.CategoryMapper;
import com.example.aforum.content.mapper.CommentMapper;
import com.example.aforum.content.mapper.ContentReviewMapper;
import com.example.aforum.content.mapper.PostMapper;
import com.example.aforum.content.service.AuditDecision;
import com.example.aforum.content.service.ContentAuditService;
import com.example.aforum.content.service.PostService;
import com.example.aforum.content.vo.CommentVO;
import com.example.aforum.content.vo.PostDetailVO;
import com.example.aforum.content.vo.PostSummaryVO;
import com.example.aforum.interaction.entity.PostFavorite;
import com.example.aforum.interaction.mapper.PostFavoriteMapper;
import com.example.aforum.interaction.service.InteractionService;
import com.example.aforum.message.service.NotificationService;
import com.example.aforum.search.event.PostSearchIndexEvent;
import com.example.aforum.user.entity.User;
import com.example.aforum.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private static final int MAX_PAGE_SIZE = 50;

    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final BoardMapper boardMapper;
    private final ContentReviewMapper contentReviewMapper;
    private final ContentAuditService contentAuditService;
    private final PostFavoriteMapper postFavoriteMapper;
    private final InteractionService interactionService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PageResult<PostSummaryVO> listPosts(Long currentUserId, PostQueryDTO query) {
        long pageNum = sanitizePageNum(query.getPageNum());
        long pageSize = sanitizePageSize(query.getPageSize());
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, PostStatus.PUBLISHED.getCode());
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(Post::getTitle, query.getKeyword())
                    .or()
                    .like(Post::getContent, query.getKeyword()));
        }
        if (query.getUserId() != null) {
            wrapper.eq(Post::getUserId, query.getUserId());
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(Post::getCategoryId, query.getCategoryId());
        }
        if (query.getBoardId() != null) {
            wrapper.eq(Post::getBoardId, query.getBoardId());
        }
        applyListSort(wrapper, query.getSort());
        Page<Post> page = postMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<PostSummaryVO> records = page.getRecords().stream()
                .map(post -> toSummaryVO(post, currentUserId))
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    @Override
    @Transactional
    public PostDetailVO getPostDetail(Long currentUserId, Long id, boolean increaseView) {
        Post post = getVisiblePost(id);
        if (increaseView && PostStatus.PUBLISHED.getCode().equals(post.getStatus())) {
            post.setViewCount(defaultInt(post.getViewCount()) + 1);
            postMapper.updateById(post);
        }
        return toDetailVO(post, latestReview(post.getId()), suggestedTags(post.getTitle(), post.getContent()), currentUserId);
    }

    @Override
    @Transactional
    public PostDetailVO createPost(Long userId, CreatePostDTO dto) {
        requireUser(userId);
        Post post = new Post();
        post.setTitle(dto.getTitle().trim());
        post.setContent(dto.getContent().trim());
        post.setSummary(buildSummary(dto.getContent()));
        post.setUserId(userId);
        post.setCategoryId(resolveCategoryId(dto.getCategoryId()));
        post.setBoardId(resolveBoardId(dto.getBoardId()));
        post.setStatus(PostStatus.REVIEWING.getCode());
        post.setTop(false);
        post.setEssence(false);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setFavoriteCount(0);
        postMapper.insert(post);

        AuditDecision decision = contentAuditService.review("post", post.getId(), post.getTitle(), post.getContent());
        post.setStatus(decision.approved() ? PostStatus.PUBLISHED.getCode() : PostStatus.REJECTED.getCode());
        postMapper.updateById(post);
        if (decision.approved()) {
            increaseBoardPostCount(post.getBoardId());
        }
        publishIndexEvent(post.getId(), decision.approved() ? PostSearchIndexEvent.Action.UPSERT : PostSearchIndexEvent.Action.DELETE);
        notificationService.notifyAudit(userId, "post", post.getId(), decision.approved(), decision.reason());
        return toDetailVO(post, latestReview(post.getId()), decision.suggestedTags(), userId);
    }

    @Override
    @Transactional
    public PostDetailVO updatePost(Long userId, Long id, UpdatePostDTO dto) {
        requireUser(userId);
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        if (!canManage(userId, post)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权编辑该帖子");
        }
        Long oldBoardId = post.getBoardId();
        post.setTitle(dto.getTitle().trim());
        post.setContent(dto.getContent().trim());
        post.setSummary(buildSummary(dto.getContent()));
        post.setCategoryId(resolveCategoryId(dto.getCategoryId()));
        post.setBoardId(resolveBoardId(dto.getBoardId()));
        post.setStatus(PostStatus.REVIEWING.getCode());
        postMapper.updateById(post);

        AuditDecision decision = contentAuditService.review("post", post.getId(), post.getTitle(), post.getContent());
        post.setStatus(decision.approved() ? PostStatus.PUBLISHED.getCode() : PostStatus.REJECTED.getCode());
        postMapper.updateById(post);
        if (decision.approved() && (oldBoardId == null || !oldBoardId.equals(post.getBoardId()))) {
            increaseBoardPostCount(post.getBoardId());
        }
        publishIndexEvent(post.getId(), decision.approved() ? PostSearchIndexEvent.Action.UPSERT : PostSearchIndexEvent.Action.DELETE);
        notificationService.notifyAudit(userId, "post", post.getId(), decision.approved(), decision.reason());
        return toDetailVO(post, latestReview(post.getId()), decision.suggestedTags(), userId);
    }

    @Override
    @Transactional
    public void deletePost(Long userId, Long id) {
        requireUser(userId);
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        if (!canManage(userId, post)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除该帖子");
        }
        postMapper.deleteById(id);
        publishIndexEvent(id, PostSearchIndexEvent.Action.DELETE);
    }

    @Override
    public List<CommentVO> listComments(Long currentUserId, Long postId) {
        Post post = getVisiblePost(postId);
        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, post.getId())
                .eq(Comment::getStatus, 0)
                .orderByAsc(Comment::getCreatedAt));
        return comments.stream().map(comment -> toCommentVO(comment, currentUserId)).toList();
    }

    @Override
    @Transactional
    public CommentVO createComment(Long userId, Long postId, CreateCommentDTO dto) {
        requireUser(userId);
        Post post = getVisiblePost(postId);
        Comment comment = new Comment();
        comment.setPostId(post.getId());
        comment.setUserId(userId);
        comment.setContent(dto.getContent().trim());
        comment.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        comment.setReplyUserId(dto.getReplyUserId());
        comment.setLikeCount(0);
        comment.setStatus(0);
        commentMapper.insert(comment);

        AuditDecision decision = contentAuditService.review("comment", comment.getId(), null, comment.getContent());
        if (decision.approved()) {
            post.setCommentCount(defaultInt(post.getCommentCount()) + 1);
            post.setLastCommentAt(LocalDateTime.now());
            postMapper.updateById(post);
            publishIndexEvent(post.getId(), PostSearchIndexEvent.Action.UPSERT);
            sendCommentNotification(userId, post, comment);
        } else {
            comment.setStatus(1);
            commentMapper.updateById(comment);
            notificationService.notifyAudit(userId, "comment", comment.getId(), false, decision.reason());
        }
        return toCommentVO(comment, userId);
    }

    @Override
    public PageResult<PostSummaryVO> listFavoritePosts(Long currentUserId, Long userId, PageRequest request) {
        requireUser(userId);
        long pageNum = sanitizePageNum(request.getPageNum());
        long pageSize = sanitizePageSize(request.getPageSize());
        Page<PostFavorite> page = postFavoriteMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<PostFavorite>()
                        .eq(PostFavorite::getUserId, userId)
                        .orderByDesc(PostFavorite::getCreatedAt));
        List<PostSummaryVO> records = page.getRecords().stream()
                .map(favorite -> postMapper.selectById(favorite.getPostId()))
                .filter(post -> post != null && PostStatus.PUBLISHED.getCode().equals(post.getStatus()))
                .map(post -> toSummaryVO(post, currentUserId))
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    private Post getVisiblePost(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null || !PostStatus.PUBLISHED.getCode().equals(post.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在或尚未发布");
        }
        return post;
    }

    private boolean canManage(Long userId, Post post) {
        return post.getUserId().equals(userId) || isAdmin(userId);
    }

    private boolean isAdmin(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && user.getUserType() != null && user.getUserType() >= 2;
    }

    private void requireUser(Long userId) {
        if (userId == null || userMapper.selectById(userId) == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private Long resolveCategoryId(Long categoryId) {
        if (categoryId != null && categoryMapper.selectById(categoryId) != null) {
            return categoryId;
        }
        Category category = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, 0)
                .orderByAsc(Category::getSort)
                .last("LIMIT 1"));
        return category == null ? null : category.getId();
    }

    private Long resolveBoardId(Long boardId) {
        if (boardId != null && boardMapper.selectById(boardId) != null) {
            return boardId;
        }
        Board board = boardMapper.selectOne(new LambdaQueryWrapper<Board>()
                .eq(Board::getStatus, 0)
                .orderByAsc(Board::getSort)
                .last("LIMIT 1"));
        return board == null ? null : board.getId();
    }

    private void increaseBoardPostCount(Long boardId) {
        if (boardId == null) {
            return;
        }
        Board board = boardMapper.selectById(boardId);
        if (board != null) {
            board.setPostCount(defaultInt(board.getPostCount()) + 1);
            boardMapper.updateById(board);
        }
    }

    private ContentReview latestReview(Long postId) {
        return contentReviewMapper.selectOne(new LambdaQueryWrapper<ContentReview>()
                .eq(ContentReview::getContentType, "post")
                .eq(ContentReview::getContentId, postId)
                .orderByDesc(ContentReview::getCreatedAt)
                .last("LIMIT 1"));
    }

    private void sendCommentNotification(Long userId, Post post, Comment comment) {
        if (comment.getReplyUserId() != null) {
            notificationService.notifyReply(userId, comment.getReplyUserId(), post.getId(), comment.getId());
            if (!comment.getReplyUserId().equals(post.getUserId())) {
                notificationService.notifyComment(userId, post.getUserId(), post.getId(), comment.getId());
            }
            return;
        }
        if (comment.getParentId() != null && comment.getParentId() > 0) {
            Comment parent = commentMapper.selectById(comment.getParentId());
            if (parent != null) {
                notificationService.notifyReply(userId, parent.getUserId(), post.getId(), comment.getId());
                if (!parent.getUserId().equals(post.getUserId())) {
                    notificationService.notifyComment(userId, post.getUserId(), post.getId(), comment.getId());
                }
                return;
            }
        }
        notificationService.notifyComment(userId, post.getUserId(), post.getId(), comment.getId());
    }

    private PostSummaryVO toSummaryVO(Post post, Long currentUserId) {
        ContentReview review = latestReview(post.getId());
        return PostSummaryVO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .summary(post.getSummary())
                .coverUrl(post.getCoverImage())
                .userId(post.getUserId())
                .authorName(userName(post.getUserId()))
                .categoryId(post.getCategoryId())
                .categoryName(categoryName(post.getCategoryId()))
                .boardId(post.getBoardId())
                .boardName(boardName(post.getBoardId()))
                .status(post.getStatus())
                .auditReason(review == null ? null : review.getReason())
                .viewCount(defaultInt(post.getViewCount()))
                .likeCount(defaultInt(post.getLikeCount()))
                .commentCount(defaultInt(post.getCommentCount()))
                .favoriteCount(defaultInt(post.getFavoriteCount()))
                .liked(interactionService.isPostLiked(currentUserId, post.getId()))
                .favorited(interactionService.isPostFavorited(currentUserId, post.getId()))
                .createdAt(post.getCreatedAt())
                .lastCommentAt(post.getLastCommentAt())
                .build();
    }

    private PostDetailVO toDetailVO(Post post, ContentReview review, List<String> suggestedTags, Long currentUserId) {
        return PostDetailVO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .summary(post.getSummary())
                .userId(post.getUserId())
                .authorName(userName(post.getUserId()))
                .categoryId(post.getCategoryId())
                .categoryName(categoryName(post.getCategoryId()))
                .boardId(post.getBoardId())
                .boardName(boardName(post.getBoardId()))
                .status(post.getStatus())
                .auditReason(review == null ? null : review.getReason())
                .suggestedTags(suggestedTags)
                .viewCount(defaultInt(post.getViewCount()))
                .likeCount(defaultInt(post.getLikeCount()))
                .commentCount(defaultInt(post.getCommentCount()))
                .favoriteCount(defaultInt(post.getFavoriteCount()))
                .liked(interactionService.isPostLiked(currentUserId, post.getId()))
                .favorited(interactionService.isPostFavorited(currentUserId, post.getId()))
                .top(Boolean.TRUE.equals(post.getTop()))
                .essence(Boolean.TRUE.equals(post.getEssence()))
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    private CommentVO toCommentVO(Comment comment, Long currentUserId) {
        return CommentVO.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .postId(comment.getPostId())
                .userId(comment.getUserId())
                .authorName(userName(comment.getUserId()))
                .parentId(comment.getParentId())
                .replyUserId(comment.getReplyUserId())
                .likeCount(defaultInt(comment.getLikeCount()))
                .liked(interactionService.isCommentLiked(currentUserId, comment.getId()))
                .status(comment.getStatus())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    private String userName(Long userId) {
        if (userId == null) {
            return "匿名用户";
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return "已注销用户";
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private String categoryName(Long categoryId) {
        Category category = categoryId == null ? null : categoryMapper.selectById(categoryId);
        return category == null ? null : category.getName();
    }

    private String boardName(Long boardId) {
        Board board = boardId == null ? null : boardMapper.selectById(boardId);
        return board == null ? null : board.getName();
    }

    private String buildSummary(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String text = content.replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return text.length() <= 140 ? text : text.substring(0, 140) + "...";
    }

    private List<String> suggestedTags(String title, String content) {
        List<String> tags = new ArrayList<>();
        String text = ((title == null ? "" : title) + " " + (content == null ? "" : content)).toLowerCase();
        if (text.contains("java") || text.contains("spring")) {
            tags.add("Java");
        }
        if (text.contains("react") || text.contains("前端")) {
            tags.add("前端");
        }
        if (text.contains("ai") || text.contains("智能") || text.contains("agent")) {
            tags.add("AI");
        }
        return tags;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private void applyListSort(LambdaQueryWrapper<Post> wrapper, String sort) {
        if ("hot".equals(sort)) {
            wrapper.orderByDesc(Post::getTop)
                    .orderByDesc(Post::getCommentCount)
                    .orderByDesc(Post::getLikeCount)
                    .orderByDesc(Post::getViewCount)
                    .orderByDesc(Post::getCreatedAt);
        } else if ("latest".equals(sort)) {
            wrapper.orderByDesc(Post::getTop)
                    .orderByDesc(Post::getCreatedAt);
        } else {
            wrapper.orderByDesc(Post::getTop)
                    .orderByDesc(Post::getCreatedAt);
        }
    }

    private void publishIndexEvent(Long postId, PostSearchIndexEvent.Action action) {
        eventPublisher.publishEvent(new PostSearchIndexEvent(postId, action));
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
