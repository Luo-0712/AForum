package com.example.aforum.admin.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminTrendPointVO {

    private String date;
    private Long userCount;
    private Long postCount;
    private Long commentCount;
}
