package com.example.aforum.admin.controller;

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
import com.example.aforum.common.result.PageResult;
import com.example.aforum.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminManagementController {

    private final AdminManagementService adminManagementService;

    @GetMapping({"/dashboard", "/stats/overview"})
    @PreAuthorize("hasAuthority('admin:dashboard')")
    public Result<AdminDashboardVO> overview() {
        return Result.success(adminManagementService.overview());
    }

    @GetMapping("/stats/trends")
    @PreAuthorize("hasAnyAuthority('admin:dashboard', 'admin:stats:trends')")
    public Result<List<AdminTrendPointVO>> trends(@RequestParam(defaultValue = "7") int days) {
        return Result.success(adminManagementService.trends(days));
    }

    @GetMapping("/boards")
    @PreAuthorize("hasAuthority('board:list')")
    public Result<List<AdminBoardVO>> listBoards() {
        return Result.success(adminManagementService.listBoards());
    }

    @PostMapping("/boards")
    @PreAuthorize("hasAuthority('board:add')")
    public Result<AdminBoardVO> createBoard(@Valid @RequestBody AdminBoardDTO dto) {
        return Result.success(adminManagementService.createBoard(dto));
    }

    @PutMapping("/boards/{id}")
    @PreAuthorize("hasAuthority('board:edit')")
    public Result<AdminBoardVO> updateBoard(@PathVariable Long id, @Valid @RequestBody AdminBoardDTO dto) {
        return Result.success(adminManagementService.updateBoard(id, dto));
    }

    @DeleteMapping("/boards/{id}")
    @PreAuthorize("hasAuthority('board:delete')")
    public Result<Void> deleteBoard(@PathVariable Long id) {
        adminManagementService.deleteBoard(id);
        return Result.success();
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('category:list')")
    public Result<List<AdminCategoryVO>> listCategories() {
        return Result.success(adminManagementService.listCategories());
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAuthority('category:add')")
    public Result<AdminCategoryVO> createCategory(@Valid @RequestBody AdminCategoryDTO dto) {
        return Result.success(adminManagementService.createCategory(dto));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('category:edit')")
    public Result<AdminCategoryVO> updateCategory(@PathVariable Long id, @Valid @RequestBody AdminCategoryDTO dto) {
        return Result.success(adminManagementService.updateCategory(id, dto));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('category:delete')")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        adminManagementService.deleteCategory(id);
        return Result.success();
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('user:list')")
    public Result<PageResult<AdminUserVO>> listUsers(AdminUserQueryDTO query) {
        return Result.success(adminManagementService.listUsers(query));
    }

    @PutMapping("/users/{id}/status")
    @PreAuthorize("hasAuthority('user:edit')")
    public Result<AdminUserVO> updateUserStatus(@PathVariable Long id, @Valid @RequestBody UpdateUserStatusDTO dto) {
        return Result.success(adminManagementService.updateUserStatus(id, dto));
    }

    @GetMapping("/posts")
    @PreAuthorize("hasAuthority('post:list')")
    public Result<PageResult<AdminPostVO>> listPosts(AdminPostQueryDTO query) {
        return Result.success(adminManagementService.listPosts(query));
    }

    @GetMapping("/audit/pending")
    @PreAuthorize("hasAuthority('post:review')")
    public Result<PageResult<AdminPostVO>> listPendingPosts(AdminPostQueryDTO query) {
        return Result.success(adminManagementService.listPendingPosts(query));
    }

    @PutMapping({"/audit/posts/{id}/approve", "/posts/{id}/approve"})
    @PreAuthorize("hasAuthority('post:review')")
    public Result<AdminPostVO> approvePost(@PathVariable Long id) {
        return Result.success(adminManagementService.approvePost(id));
    }

    @PutMapping({"/audit/posts/{id}/reject", "/posts/{id}/reject"})
    @PreAuthorize("hasAuthority('post:review')")
    public Result<AdminPostVO> rejectPost(@PathVariable Long id, @Valid @RequestBody(required = false) RejectPostDTO dto) {
        return Result.success(adminManagementService.rejectPost(id, dto == null ? new RejectPostDTO() : dto));
    }

    @PutMapping("/posts/{id}/flags")
    @PreAuthorize("hasAuthority('post:review')")
    public Result<AdminPostVO> updatePostFlags(@PathVariable Long id, @Valid @RequestBody UpdatePostFlagsDTO dto) {
        return Result.success(adminManagementService.updatePostFlags(id, dto));
    }

    @DeleteMapping("/posts/{id}")
    @PreAuthorize("hasAuthority('post:delete')")
    public Result<Void> deletePost(@PathVariable Long id) {
        adminManagementService.deletePost(id);
        return Result.success();
    }
}
