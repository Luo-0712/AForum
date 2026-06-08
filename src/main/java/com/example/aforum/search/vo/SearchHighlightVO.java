package com.example.aforum.search.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchHighlightVO {

    private List<String> title;

    private List<String> summary;

    private List<String> content;
}
