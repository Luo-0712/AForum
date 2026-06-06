package com.example.aforum.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UpdateRolePermissionsDTO {

    @NotNull(message = "权限列表不能为空")
    private List<@NotNull(message = "权限ID不能为空") Long> permissionIds = new ArrayList<>();
}
