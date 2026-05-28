package com.example.aforum.common.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用分页请求参数
 */
@Data
public class PageRequest implements Serializable {

    /**
     * 当前页码，默认第 1 页
     */
    private Long pageNum = 1L;

    /**
     * 每页条数，默认 10 条
     */
    private Long pageSize = 10L;

    /**
     * 排序字段
     */
    private String orderBy;

    /**
     * 是否升序，默认降序
     */
    private boolean asc = false;
}
