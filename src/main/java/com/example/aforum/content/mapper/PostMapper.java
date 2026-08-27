package com.example.aforum.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aforum.content.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    @Update("UPDATE post SET like_count = #{count} WHERE id = #{postId}")
    int updateLikeCount(@Param("postId") Long postId, @Param("count") int count);

    @Update("UPDATE post SET favorite_count = #{count} WHERE id = #{postId}")
    int updateFavoriteCount(@Param("postId") Long postId, @Param("count") int count);

    @Update("UPDATE post SET view_count = view_count + 1 WHERE id = #{postId} AND status = 1 AND deleted = 0")
    int incrementPublishedViewCount(@Param("postId") Long postId);
}
