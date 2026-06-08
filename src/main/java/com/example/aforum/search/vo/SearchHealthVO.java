package com.example.aforum.search.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchHealthVO {

    private boolean enabled;
    private boolean available;
    private boolean aliasExists;
    private String indexAlias;
    private String physicalIndex;
    private String message;
}
