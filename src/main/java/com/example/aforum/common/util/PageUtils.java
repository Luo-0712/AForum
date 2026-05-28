package com.example.aforum.common.util;

import com.example.aforum.common.result.PageResult;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页工具类（基于 PageHelper）
 */
public class PageUtils {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private PageUtils() {
    }

    /**
     * 开启分页（在 Mapper 查询前调用）
     */
    public static void startPage(Integer pageNum, Integer pageSize) {
        startPage(pageNum, pageSize, null, null);
    }

    /**
     * 开启分页（带排序）
     */
    public static void startPage(Integer pageNum, Integer pageSize, String orderBy) {
        pageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        pageSize = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE : pageSize;
        pageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        PageHelper.startPage(pageNum, pageSize, orderBy);
    }

    /**
     * 开启分页（带排序字段和排序方式）
     */
    public static void startPage(Integer pageNum, Integer pageSize, String sortField, String sortOrder) {
        String orderBy = null;
        if (sortField != null && !sortField.isEmpty()) {
            orderBy = "desc".equalsIgnoreCase(sortOrder) ? sortField + " desc" : sortField + " asc";
        }
        startPage(pageNum, pageSize, orderBy);
    }

    /**
     * 将 PageInfo 转换为 PageResult
     */
    public static <T> PageResult<T> toPageResult(PageInfo<T> pageInfo) {
        return new PageResult<>(pageInfo.getList(), pageInfo.getTotal(),
                (long) pageInfo.getPageSize(), (long) pageInfo.getPageNum());
    }

    /**
     * 将 PageInfo 转换为 PageResult（带类型转换）
     */
    public static <T, R> PageResult<R> toPageResult(PageInfo<T> pageInfo, List<R> records) {
        return new PageResult<>(records, pageInfo.getTotal(),
                (long) pageInfo.getPageSize(), (long) pageInfo.getPageNum());
    }

    /**
     * 将 Page（PageHelper 返回的 List 实际类型）转换为 PageResult
     */
    public static <T> PageResult<T> toPageResult(List<T> list) {
        if (list instanceof Page) {
            Page<T> page = (Page<T>) list;
            PageInfo<T> pageInfo = new PageInfo<>(page);
            return toPageResult(pageInfo);
        }
        PageInfo<T> pageInfo = new PageInfo<>(list);
        return toPageResult(pageInfo);
    }

    /**
     * 将 Page 转换为 PageResult（带类型转换）
     */
    public static <T, R> PageResult<R> toPageResult(List<T> list, Function<T, R> converter) {
        PageInfo<T> pageInfo = new PageInfo<>(list);
        List<R> records = list.stream().map(converter).collect(Collectors.toList());
        return new PageResult<>(records, pageInfo.getTotal(),
                (long) pageInfo.getPageSize(), (long) pageInfo.getPageNum());
    }
}
