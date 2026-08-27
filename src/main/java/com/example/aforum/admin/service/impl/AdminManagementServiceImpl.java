package com.example.aforum.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aforum.admin.dto.AdminBoardDTO;
import com.example.aforum.admin.dto.AdminCategoryDTO;
import com.example.aforum.admin.dto.AdminPostQueryDTO;
import com.example.aforum.admin.dto.AdminUserQueryDTO;
import com.example.aforum.admin.dto.RejectPostDTO;
import com.example.aforum.admin.dto.UpdatePostFlagsDTO;
import com.example.aforum.admin.dto.UpdateUserStatusDTO;
import com.example.aforum.admin.service.AdminManagementService;
import com.example.aforum.admin.vo.AdminBoardVO;
import com.example.aforum.admin.vo.AdminCategoryVO;
import com.example.aforum.admin.vo.AdminDashboardVO;
import com.example.aforum.admin.vo.AdminPostVO;
import com.example.aforum.admin.vo.AdminTrendPointVO;
import com.example.aforum.admin.vo.AdminUserVO;
import com.example.aforum.auth.entity.Role;
import com.example.aforum.auth.entity.UserRole;
import com.example.aforum.auth.mapper.RoleMapper;
import com.example.aforum.auth.mapper.UserRoleMapper;
import com.example.aforum.auth.service.AuthorizationService;
import com.example.aforum.common.base.BaseEntity;
import com.example.aforum.content.service.impl.ContentCacheManager;
import com.example.aforum.common.constant.PostStatus;
import com.example.aforum.common.constant.UserStatus;
import com.example.aforum.common.exception.BusinessException;
import com.example.aforum.common.result.ErrorCode;
import com.example.aforum.common.result.PageResult;
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
import com.example.aforum.message.service.NotificationService;
import com.example.aforum.search.event.PostSearchIndexEvent;
import com.example.aforum.user.entity.User;
import com.example.aforum.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class AdminManagementServiceImpl implements AdminManagementService {

    private static final int ACTIVE = 0;
    private static final int MAX_PAGE_SIZE = 50;
    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final BoardMapper boardMapper;
    private final CategoryMapper categoryMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final ContentReviewMapper contentReviewMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final ContentCacheManager contentCacheManager;

    @Override
    public AdminDashboardVO overview() {
        return AdminDashboardVO.builder()
                .userCount(userMapper.selectCount(new LambdaQueryWrapper<User>()))
                .activeUserCount(userMapper.selectCount(new LambdaQueryWrapper<User>()
                        .eq(User::getStatus, UserStatus.ACTIVE.getCode())))
                .disabledUserCount(userMapper.selectCount(new LambdaQueryWrapper<User>()
                        .ne(User::getStatus, UserStatus.ACTIVE.getCode())))
                .postCount(postMapper.selectCount(new LambdaQueryWrapper<Post>()))
                .publishedPostCount(postMapper.selectCount(new LambdaQueryWrapper<Post>()
                        .eq(Post::getStatus, PostStatus.PUBLISHED.getCode())))
                .pendingPostCount(postMapper.selectCount(new LambdaQueryWrapper<Post>()
                        .eq(Post::getStatus, PostStatus.REVIEWING.getCode())))
                .rejectedPostCount(postMapper.selectCount(new LambdaQueryWrapper<Post>()
                        .eq(Post::getStatus, PostStatus.REJECTED.getCode())))
                .commentCount(commentMapper.selectCount(new LambdaQueryWrapper<Comment>()))
                .boardCount(boardMapper.selectCount(new LambdaQueryWrapper<Board>()))
                .categoryCount(categoryMapper.selectCount(new LambdaQueryWrapper<Category>()))
                .build();
    }

    @Override
    public List<AdminTrendPointVO> trends(int days) {
        int safeDays = Math.min(Math.max(days, 1), 30);
        LocalDate start = LocalDate.now().minusDays(safeDays - 1L);
        Map<LocalDate, Long> userCounts = countByDate(userMapper.selectList(new LambdaQueryWrapper<User>()
                .ge(User::getCreatedAt, start.atStartOfDay())));
        Map<LocalDate, Long> postCounts = countByDate(postMapper.selectList(new LambdaQueryWrapper<Post>()
                .ge(Post::getCreatedAt, start.atStartOfDay())));
        Map<LocalDate, Long> commentCounts = countByDate(commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .ge(Comment::getCreatedAt, start.atStartOfDay())));

        return IntStream.range(0, safeDays)
                .mapToObj(start::plusDays)
                .map(date -> AdminTrendPointVO.builder()
                        .date(date.toString())
                        .userCount(userCounts.getOrDefault(date, 0L))
                        .postCount(postCounts.getOrDefault(date, 0L))
                        .commentCount(commentCounts.getOrDefault(date, 0L))
                        .build())
                .toList();
    }

    @Override
    public List<AdminBoardVO> listBoards() {
        return boardMapper.selectList(new LambdaQueryWrapper<Board>()
                        .orderByAsc(Board::getSort)
                        .orderByAsc(Board::getId))
                .stream()
                .map(this::toBoardVO)
                .toList();
    }

    @Override
    @Transactional
    public AdminBoardVO createBoard(AdminBoardDTO dto) {
        ensureBoardCodeAvailable(dto.getCode(), null);
        Board board = new Board();
        applyBoard(board, dto);
        board.setPostCount(0);
        boardMapper.insert(board);
        contentCacheManager.evictSearchOptions();
        return toBoardVO(board);
    }

    @Override
    @Transactional
    public AdminBoardVO updateBoard(Long id, AdminBoardDTO dto) {
        Board board = requireBoard(id);
        ensureBoardCodeAvailable(dto.getCode(), id);
        applyBoard(board, dto);
        boardMapper.updateById(board);
        contentCacheManager.evictSearchOptions();
        return toBoardVO(board);
    }

    @Override
    @Transactional
    public void deleteBoard(Long id) {
        requireBoard(id);
        Long postCount = postMapper.selectCount(new LambdaQueryWrapper<Post>().eq(Post::getBoardId, id));
        if (postCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "board is used by posts");
        }
        boardMapper.deleteById(id);
        contentCacheManager.evictSearchOptions();
    }

    @Override
    public List<AdminCategoryVO> listCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSort)
                        .orderByAsc(Category::getId))
                .stream()
                .map(this::toCategoryVO)
                .toList();
    }

    @Override
    @Transactional
    public AdminCategoryVO createCategory(AdminCategoryDTO dto) {
        ensureCategoryCodeAvailable(dto.getCode(), null);
        validateParentCategory(dto.getParentId(), null);
        Category category = new Category();
        applyCategory(category, dto);
        categoryMapper.insert(category);
        contentCacheManager.evictSearchOptions();
        return toCategoryVO(category);
    }

    @Override
    @Transactional
    public AdminCategoryVO updateCategory(Long id, AdminCategoryDTO dto) {
        Category category = requireCategory(id);
        ensureCategoryCodeAvailable(dto.getCode(), id);
        validateParentCategory(dto.getParentId(), id);
        applyCategory(category, dto);
        categoryMapper.updateById(category);
        contentCacheManager.evictSearchOptions();
        return toCategoryVO(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        requireCategory(id);
        Long childCount = categoryMapper.selectCount(new LambdaQueryWrapper<Category>().eq(Category::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "category has children");
        }
        Long postCount = postMapper.selectCount(new LambdaQueryWrapper<Post>().eq(Post::getCategoryId, id));
        if (postCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "category is used by posts");
        }
        categoryMapper.deleteById(id);
        contentCacheManager.evictSearchOptions();
    }

    @Override
    public PageResult<AdminUserVO> listUsers(AdminUserQueryDTO query) {
        long pageNum = sanitizePageNum(query.getPageNum());
        long pageSize = sanitizePageSize(query.getPageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword)
                    .or()
                    .like(User::getEmail, keyword));
        }
        if (query.getStatus() != null) {
            wrapper.eq(User::getStatus, query.getStatus());
        }
        if (query.getUserType() != null) {
            wrapper.eq(User::getUserType, query.getUserType());
        }
        wrapper.orderByDesc(User::getCreatedAt).orderByDesc(User::getId);
        Page<User> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toUserVO).toList(),
                page.getTotal(), page.getSize(), page.getCurrent());
    }

    @Override
    @Transactional
    public AdminUserVO updateUserStatus(Long id, UpdateUserStatusDTO dto) {
        User user = requireUser(id);
        Integer oldStatus = user.getStatus();
        if (UserStatus.ACTIVE.getCode().equals(oldStatus)
                && !UserStatus.ACTIVE.getCode().equals(dto.getStatus())
                && authorizationService.hasRole(user.getId(), SUPER_ADMIN)
                && countActiveSuperAdminUsers() <= 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "cannot disable the last active super admin");
        }
        user.setStatus(dto.getStatus());
        userMapper.updateById(user);
        return toUserVO(user);
    }

    @Override
    public PageResult<AdminPostVO> listPosts(AdminPostQueryDTO query) {
        return listPostsByQuery(query, query.getStatus());
    }

    @Override
    public PageResult<AdminPostVO> listPendingPosts(AdminPostQueryDTO query) {
        return listPostsByQuery(query, PostStatus.REVIEWING.getCode());
    }

    @Override
    @Transactional
    public AdminPostVO approvePost(Long id) {
        Post post = requirePost(id);
        Integer oldStatus = post.getStatus();
        post.setStatus(PostStatus.PUBLISHED.getCode());
        postMapper.updateById(post);
        if (!PostStatus.PUBLISHED.getCode().equals(oldStatus)) {
            syncBoardPostCount(post.getBoardId());
        }
        saveManualReview(post.getId(), "approved", "low", BigDecimal.ZERO, "Approved by admin");
        notificationService.notifyAudit(post.getUserId(), "post", post.getId(), true, "Approved by admin");
        publishIndexEvent(post.getId(), PostSearchIndexEvent.Action.UPSERT);
        contentCacheManager.bumpPostGeneration();
        return toPostVO(post);
    }

    @Override
    @Transactional
    public AdminPostVO rejectPost(Long id, RejectPostDTO dto) {
        Post post = requirePost(id);
        Integer oldStatus = post.getStatus();
        post.setStatus(PostStatus.REJECTED.getCode());
        postMapper.updateById(post);
        if (PostStatus.PUBLISHED.getCode().equals(oldStatus)) {
            syncBoardPostCount(post.getBoardId());
        }
        String reason = StringUtils.hasText(dto.getReason()) ? dto.getReason().trim() : "Rejected by admin";
        saveManualReview(post.getId(), "rejected", "medium", BigDecimal.ONE, reason);
        notificationService.notifyAudit(post.getUserId(), "post", post.getId(), false, reason);
        publishIndexEvent(post.getId(), PostSearchIndexEvent.Action.DELETE);
        contentCacheManager.bumpPostGeneration();
        return toPostVO(post);
    }

    @Override
    @Transactional
    public AdminPostVO updatePostFlags(Long id, UpdatePostFlagsDTO dto) {
        Post post = requirePost(id);
        if (dto.getTop() != null) {
            post.setTop(dto.getTop());
        }
        if (dto.getEssence() != null) {
            post.setEssence(dto.getEssence());
        }
        postMapper.updateById(post);
        if (PostStatus.PUBLISHED.getCode().equals(post.getStatus())) {
            publishIndexEvent(post.getId(), PostSearchIndexEvent.Action.UPSERT);
        }
        contentCacheManager.bumpPostGeneration();
        return toPostVO(post);
    }

    @Override
    @Transactional
    public void deletePost(Long id) {
        Post post = requirePost(id);
        postMapper.deleteById(id);
        if (PostStatus.PUBLISHED.getCode().equals(post.getStatus())) {
            syncBoardPostCount(post.getBoardId());
        }
        publishIndexEvent(id, PostSearchIndexEvent.Action.DELETE);
        contentCacheManager.bumpPostGeneration();
    }

    private PageResult<AdminPostVO> listPostsByQuery(AdminPostQueryDTO query, Integer forcedStatus) {
        long pageNum = sanitizePageNum(query.getPageNum());
        long pageSize = sanitizePageSize(query.getPageSize());
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(Post::getTitle, keyword)
                    .or()
                    .like(Post::getSummary, keyword)
                    .or()
                    .like(Post::getContent, keyword));
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
        if (forcedStatus != null) {
            wrapper.eq(Post::getStatus, forcedStatus);
        }
        wrapper.orderByDesc(Post::getCreatedAt).orderByDesc(Post::getId);
        Page<Post> page = postMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toPostVO).toList(),
                page.getTotal(), page.getSize(), page.getCurrent());
    }

    private <T extends BaseEntity> Map<LocalDate, Long> countByDate(List<T> records) {
        return records.stream()
                .filter(record -> record.getCreatedAt() != null)
                .collect(Collectors.groupingBy(record -> record.getCreatedAt().toLocalDate(), Collectors.counting()));
    }

    private void applyBoard(Board board, AdminBoardDTO dto) {
        board.setName(dto.getName().trim());
        board.setCode(dto.getCode().trim());
        board.setDescription(trimToNull(dto.getDescription()));
        board.setIcon(trimToNull(dto.getIcon()));
        board.setSort(dto.getSort() == null ? 0 : dto.getSort());
        board.setStatus(dto.getStatus() == null ? ACTIVE : dto.getStatus());
    }

    private void applyCategory(Category category, AdminCategoryDTO dto) {
        category.setName(dto.getName().trim());
        category.setCode(dto.getCode().trim());
        category.setDescription(trimToNull(dto.getDescription()));
        category.setIcon(trimToNull(dto.getIcon()));
        category.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        category.setSort(dto.getSort() == null ? 0 : dto.getSort());
        category.setStatus(dto.getStatus() == null ? ACTIVE : dto.getStatus());
    }

    private Board requireBoard(Long id) {
        Board board = boardMapper.selectById(id);
        if (board == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "board not found");
        }
        return board;
    }

    private Category requireCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "category not found");
        }
        return category;
    }

    private User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found");
        }
        return user;
    }

    private Post requirePost(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "post not found");
        }
        return post;
    }

    private void ensureBoardCodeAvailable(String code, Long currentId) {
        Board existing = boardMapper.selectOne(new LambdaQueryWrapper<Board>().eq(Board::getCode, code.trim()));
        if (existing != null && !existing.getId().equals(currentId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "board code already exists");
        }
    }

    private void ensureCategoryCodeAvailable(String code, Long currentId) {
        Category existing = categoryMapper.selectOne(new LambdaQueryWrapper<Category>().eq(Category::getCode, code.trim()));
        if (existing != null && !existing.getId().equals(currentId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "category code already exists");
        }
    }

    private void validateParentCategory(Long parentId, Long currentId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        if (currentId != null && parentId.equals(currentId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "category cannot be its own parent");
        }
        if (categoryMapper.selectById(parentId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "parent category not found");
        }
    }

    private void saveManualReview(Long postId, String decision, String riskLevel, BigDecimal riskScore, String reason) {
        ContentReview review = new ContentReview();
        review.setContentType("post");
        review.setContentId(postId);
        review.setDecision(decision);
        review.setRiskLevel(riskLevel);
        review.setRiskScore(riskScore);
        review.setReason(reason);
        review.setSuggestion(null);
        review.setModelName("manual-admin");
        review.setPromptVersion("manual-v1");
        contentReviewMapper.insert(review);
    }

    private ContentReview latestReview(Long postId) {
        return contentReviewMapper.selectOne(new LambdaQueryWrapper<ContentReview>()
                .eq(ContentReview::getContentType, "post")
                .eq(ContentReview::getContentId, postId)
                .orderByDesc(ContentReview::getCreatedAt)
                .orderByDesc(ContentReview::getId)
                .last("LIMIT 1"));
    }

    private void syncBoardPostCount(Long boardId) {
        if (boardId == null) {
            return;
        }
        Board board = boardMapper.selectById(boardId);
        if (board == null) {
            return;
        }
        Long count = postMapper.selectCount(new LambdaQueryWrapper<Post>()
                .eq(Post::getBoardId, boardId)
                .eq(Post::getStatus, PostStatus.PUBLISHED.getCode()));
        board.setPostCount(count.intValue());
        boardMapper.updateById(board);
    }

    private long countActiveSuperAdminUsers() {
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, SUPER_ADMIN)
                .eq(Role::getStatus, ACTIVE));
        if (role == null) {
            return 0L;
        }
        List<UserRole> assignments = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getRoleId, role.getId()));
        return assignments.stream()
                .map(UserRole::getUserId)
                .distinct()
                .map(userMapper::selectById)
                .filter(user -> user != null && UserStatus.ACTIVE.getCode().equals(user.getStatus()))
                .count();
    }

    private AdminBoardVO toBoardVO(Board board) {
        return AdminBoardVO.builder()
                .id(board.getId())
                .name(board.getName())
                .code(board.getCode())
                .description(board.getDescription())
                .icon(board.getIcon())
                .sort(defaultInt(board.getSort()))
                .status(defaultInt(board.getStatus()))
                .postCount(defaultInt(board.getPostCount()))
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build();
    }

    private AdminCategoryVO toCategoryVO(Category category) {
        Long postCount = postMapper.selectCount(new LambdaQueryWrapper<Post>().eq(Post::getCategoryId, category.getId()));
        return AdminCategoryVO.builder()
                .id(category.getId())
                .name(category.getName())
                .code(category.getCode())
                .description(category.getDescription())
                .icon(category.getIcon())
                .parentId(category.getParentId())
                .sort(defaultInt(category.getSort()))
                .status(defaultInt(category.getStatus()))
                .postCount(postCount)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private AdminUserVO toUserVO(User user) {
        return AdminUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .userType(user.getUserType())
                .status(user.getStatus())
                .roles(authorizationService.getRoleCodesByUserId(user.getId()))
                .postCount(postMapper.selectCount(new LambdaQueryWrapper<Post>().eq(Post::getUserId, user.getId())))
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private AdminPostVO toPostVO(Post post) {
        ContentReview review = latestReview(post.getId());
        return AdminPostVO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .summary(post.getSummary())
                .content(post.getContent())
                .userId(post.getUserId())
                .authorName(userName(post.getUserId()))
                .categoryId(post.getCategoryId())
                .categoryName(categoryName(post.getCategoryId()))
                .boardId(post.getBoardId())
                .boardName(boardName(post.getBoardId()))
                .status(post.getStatus())
                .auditReason(review == null ? null : review.getReason())
                .top(Boolean.TRUE.equals(post.getTop()))
                .essence(Boolean.TRUE.equals(post.getEssence()))
                .viewCount(defaultInt(post.getViewCount()))
                .likeCount(defaultInt(post.getLikeCount()))
                .commentCount(defaultInt(post.getCommentCount()))
                .favoriteCount(defaultInt(post.getFavoriteCount()))
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    private String userName(Long userId) {
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            return null;
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

    private void publishIndexEvent(Long postId, PostSearchIndexEvent.Action action) {
        eventPublisher.publishEvent(new PostSearchIndexEvent(postId, action));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
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
