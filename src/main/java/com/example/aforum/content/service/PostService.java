package com.example.aforum.content.service;

import com.example.aforum.common.result.PageResult;
import com.example.aforum.common.request.PageRequest;
import com.example.aforum.content.dto.CreateCommentDTO;
import com.example.aforum.content.dto.CreatePostDTO;
import com.example.aforum.content.dto.PostQueryDTO;
import com.example.aforum.content.dto.UpdatePostDTO;
import com.example.aforum.content.vo.CommentVO;
import com.example.aforum.content.vo.PostDetailVO;
import com.example.aforum.content.vo.PostSummaryVO;

import java.util.List;

public interface PostService {

    PageResult<PostSummaryVO> listPosts(Long currentUserId, PostQueryDTO query);

    PostDetailVO getPostDetail(Long currentUserId, Long id, boolean increaseView);

    PostDetailVO createPost(Long userId, CreatePostDTO dto);

    PostDetailVO updatePost(Long userId, Long id, UpdatePostDTO dto);

    void deletePost(Long userId, Long id);

    List<CommentVO> listComments(Long currentUserId, Long postId);

    CommentVO createComment(Long userId, Long postId, CreateCommentDTO dto);

    PageResult<PostSummaryVO> listFavoritePosts(Long currentUserId, Long userId, PageRequest request);
}
