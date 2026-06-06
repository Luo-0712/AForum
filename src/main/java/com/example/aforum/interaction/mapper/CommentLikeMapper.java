package com.example.aforum.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aforum.interaction.entity.CommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {

    @Select("SELECT * FROM comment_like WHERE comment_id = #{commentId} AND user_id = #{userId} LIMIT 1")
    CommentLike selectAny(@Param("commentId") Long commentId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM comment_like WHERE comment_id = #{commentId} AND deleted = 0")
    int countActiveByCommentId(@Param("commentId") Long commentId);

    @Update("UPDATE comment_like SET deleted = 0, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int restore(@Param("id") Long id);
}
