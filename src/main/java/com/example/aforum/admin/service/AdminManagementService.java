package com.example.aforum.admin.service;

import com.example.aforum.admin.dto.AdminBoardDTO;
import com.example.aforum.admin.dto.AdminCategoryDTO;
import com.example.aforum.admin.dto.AdminPostQueryDTO;
import com.example.aforum.admin.dto.AdminUserQueryDTO;
import com.example.aforum.admin.dto.RejectPostDTO;
import com.example.aforum.admin.dto.UpdatePostFlagsDTO;
import com.example.aforum.admin.dto.UpdateUserStatusDTO;
import com.example.aforum.admin.vo.AdminBoardVO;
import com.example.aforum.admin.vo.AdminCategoryVO;
import com.example.aforum.admin.vo.AdminDashboardVO;
import com.example.aforum.admin.vo.AdminPostVO;
import com.example.aforum.admin.vo.AdminTrendPointVO;
import com.example.aforum.admin.vo.AdminUserVO;
import com.example.aforum.common.result.PageResult;

import java.util.List;

public interface AdminManagementService {

    AdminDashboardVO overview();

    List<AdminTrendPointVO> trends(int days);

    List<AdminBoardVO> listBoards();

    AdminBoardVO createBoard(AdminBoardDTO dto);

    AdminBoardVO updateBoard(Long id, AdminBoardDTO dto);

    void deleteBoard(Long id);

    List<AdminCategoryVO> listCategories();

    AdminCategoryVO createCategory(AdminCategoryDTO dto);

    AdminCategoryVO updateCategory(Long id, AdminCategoryDTO dto);

    void deleteCategory(Long id);

    PageResult<AdminUserVO> listUsers(AdminUserQueryDTO query);

    AdminUserVO updateUserStatus(Long id, UpdateUserStatusDTO dto);

    PageResult<AdminPostVO> listPosts(AdminPostQueryDTO query);

    PageResult<AdminPostVO> listPendingPosts(AdminPostQueryDTO query);

    AdminPostVO approvePost(Long id);

    AdminPostVO rejectPost(Long id, RejectPostDTO dto);

    AdminPostVO updatePostFlags(Long id, UpdatePostFlagsDTO dto);

    void deletePost(Long id);
}
