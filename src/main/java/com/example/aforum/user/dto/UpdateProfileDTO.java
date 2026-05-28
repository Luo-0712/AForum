package com.example.aforum.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileDTO {

    @Size(max = 50, message = "昵称最长50个字符")
    private String nickname;

    @Size(max = 500, message = "个人简介最长500个字符")
    private String bio;

    @Size(max = 255, message = "头像URL最长255个字符")
    private String avatar;
}
