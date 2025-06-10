package com.learningonline.base.model;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * 分页查询结果响应模型类
 * @author :yhc
 * @version :1.0
 * @since :2025.4.8 16:50
 * @param <T>
 */
@Data
@ToString
public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    // 数据列表
    private List<T> items;

    //总记录数
    private long counts;

    //当前页码
    private long page;

    //每页记录数
    private long pageSize;
    public PageResult() {}

    public PageResult(List<T> items, long counts, long page, long pageSize) {
        this.items = items;
        this.counts = counts;
        this.page = page;
        this.pageSize = pageSize;
    }

}
