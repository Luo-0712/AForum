package com.example.aforum.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aforum.content.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    @Update("UPDATE comment SET like_count = #{count} WHERE id = #{commentId}")
    int updateLikeCount(@Param("commentId") Long commentId, @Param("count") int count);
}
