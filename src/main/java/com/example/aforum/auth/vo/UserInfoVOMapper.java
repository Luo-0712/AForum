package com.example.aforum.auth.vo;

import com.example.aforum.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserInfoVOMapper {

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "postCount", ignore = true)
    @Mapping(target = "favoriteCount", ignore = true)
    @Mapping(target = "followingCount", ignore = true)
    @Mapping(target = "followerCount", ignore = true)
    @Mapping(target = "followedByCurrentUser", ignore = true)
    UserInfoVO toVO(User user);
}
