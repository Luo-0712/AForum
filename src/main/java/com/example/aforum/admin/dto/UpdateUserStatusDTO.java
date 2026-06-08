package com.example.aforum.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserStatusDTO {

    @NotNull(message = "status is required")
    @Min(0)
    @Max(2)
    private Integer status;
}
