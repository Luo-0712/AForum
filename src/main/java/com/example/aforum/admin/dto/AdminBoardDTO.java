package com.example.aforum.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminBoardDTO {

    @NotBlank(message = "name is required")
    @Size(max = 50)
    private String name;

    @NotBlank(message = "code is required")
    @Size(max = 50)
    private String code;

    @Size(max = 200)
    private String description;

    @Size(max = 100)
    private String icon;

    private Integer sort = 0;

    private Integer status = 0;
}
