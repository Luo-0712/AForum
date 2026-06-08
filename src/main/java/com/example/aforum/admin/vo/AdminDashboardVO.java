package com.example.aforum.admin.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminDashboardVO {

    private Long userCount;
    private Long activeUserCount;
    private Long disabledUserCount;
    private Long postCount;
    private Long publishedPostCount;
    private Long pendingPostCount;
    private Long rejectedPostCount;
    private Long commentCount;
    private Long boardCount;
    private Long categoryCount;
}
