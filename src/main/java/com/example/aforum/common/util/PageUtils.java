package com.example.aforum.common.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aforum.common.result.PageResult;

import java.util.List;

/**
 * 分页工具类
 */
public class PageUtils {

    private PageUtils() {
    }

    /**
     * 创建分页对象
     */
    public static <T> Page<T> createPage(Integer pageNum, Integer pageSize) {
        return createPage(pageNum, pageSize, null, null);
    }

    /**
     * 创建分页对象（带排序）
     */
    public static <T> Page<T> createPage(Integer pageNum, Integer pageSize, String sortField, String sortOrder) {
        pageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        pageSize = (pageSize == null || pageSize < 1) ? 10 : pageSize;
        pageSize = Math.min(pageSize, 100);

        Page<T> page = new Page<>(pageNum, pageSize);
        if (sortField != null && !sortField.isEmpty()) {
            OrderItem orderItem = "desc".equalsIgnoreCase(sortOrder)
                    ? OrderItem.desc(sortField)
                    : OrderItem.asc(sortField);
            page.addOrder(orderItem);
        }
        return page;
    }

    /**
     * 转换为分页结果
     */
    public static <T> PageResult<T> toPageResult(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getSize(), page.getCurrent());
    }

    /**
     * 转换分页结果（带类型转换）
     */
    public static <T, R> PageResult<R> toPageResult(IPage<T> page, List<R> records) {
        return new PageResult<>(records, page.getTotal(), page.getSize(), page.getCurrent());
    }
}
