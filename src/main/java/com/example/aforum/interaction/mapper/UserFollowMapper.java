package com.example.aforum.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aforum.interaction.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {

    @Select("SELECT * FROM user_follow WHERE user_id = #{userId} AND follow_user_id = #{followUserId} LIMIT 1")
    UserFollow selectAny(@Param("userId") Long userId, @Param("followUserId") Long followUserId);

    @Select("SELECT COUNT(*) FROM user_follow WHERE follow_user_id = #{userId} AND deleted = 0")
    int countFollowers(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM user_follow WHERE user_id = #{userId} AND deleted = 0")
    int countFollowing(@Param("userId") Long userId);

    @Update("UPDATE user_follow SET deleted = 0, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int restore(@Param("id") Long id);
}
