package com.example.aforum.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SystemNotificationDTO {

    private Boolean broadcast = false;

    private List<Long> receiverIds;

    @NotBlank(message = "通知标题不能为空")
    @Size(max = 100, message = "通知标题最长100个字符")
    private String title;

    @NotBlank(message = "通知内容不能为空")
    @Size(max = 500, message = "通知内容最长500个字符")
    private String content;
}
