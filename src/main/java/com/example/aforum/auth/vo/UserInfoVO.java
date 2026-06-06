package com.example.aforum.auth.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserInfoVO {

    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatar;
    private String bio;
    private Integer userType;
    private Integer status;
    private Integer postCount;
    private Integer favoriteCount;
    private Integer followingCount;
    private Integer followerCount;
    private Boolean followedByCurrentUser;
    private List<String> roles;
    private List<String> permissions;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
