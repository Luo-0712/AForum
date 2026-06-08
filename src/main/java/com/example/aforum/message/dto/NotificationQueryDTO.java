package com.example.aforum.message.dto;

import com.example.aforum.common.request.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationQueryDTO extends PageRequest {

    private String type;

    private Integer readStatus;
}
