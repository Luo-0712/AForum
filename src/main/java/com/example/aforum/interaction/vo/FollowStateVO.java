package com.example.aforum.interaction.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FollowStateVO {

    private Boolean following;

    private Integer followerCount;

    private Integer followingCount;
}
