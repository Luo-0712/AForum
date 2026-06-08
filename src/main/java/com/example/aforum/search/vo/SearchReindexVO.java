package com.example.aforum.search.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchReindexVO {

    private long indexedCount;
    private long failedCount;
    private String indexAlias;
    private String physicalIndex;
}
