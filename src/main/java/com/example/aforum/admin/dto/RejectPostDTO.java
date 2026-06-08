package com.example.aforum.admin.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RejectPostDTO {

    @Size(max = 500)
    private String reason;
}
