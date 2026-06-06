package com.example.aforum.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aforum.interaction.entity.PostFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PostFavoriteMapper extends BaseMapper<PostFavorite> {

    @Select("SELECT * FROM post_favorite WHERE post_id = #{postId} AND user_id = #{userId} LIMIT 1")
    PostFavorite selectAny(@Param("postId") Long postId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM post_favorite WHERE post_id = #{postId} AND deleted = 0")
    int countActiveByPostId(@Param("postId") Long postId);

    @Select("SELECT COUNT(*) FROM post_favorite WHERE user_id = #{userId} AND deleted = 0")
    int countActiveByUserId(@Param("userId") Long userId);

    @Update("UPDATE post_favorite SET deleted = 0, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int restore(@Param("id") Long id);
}
