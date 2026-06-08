package com.example.aforum.search.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchOptionsVO {

    private List<SearchOptionVO> categories;

    private List<SearchOptionVO> boards;
}
