package com.example.aforum.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendPrivateMessageDTO {

    @NotNull(message = "接收人不能为空")
    private Long receiverId;

    @NotBlank(message = "私信内容不能为空")
    @Size(max = 2000, message = "私信内容最长2000个字符")
    private String content;

    private String messageType = "text";
}
