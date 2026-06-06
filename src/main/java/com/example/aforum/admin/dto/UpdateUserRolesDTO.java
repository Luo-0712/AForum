package com.example.aforum.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UpdateUserRolesDTO {

    @NotNull(message = "角色列表不能为空")
    private List<@NotNull(message = "角色ID不能为空") Long> roleIds = new ArrayList<>();
}
