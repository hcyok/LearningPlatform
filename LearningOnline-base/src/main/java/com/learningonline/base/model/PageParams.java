package com.learningonline.base.model;

import lombok.Data;
import lombok.ToString;

/**
 * 分页查询通用参数
 * @author :yhc
 * @since  :2025.4.7
 * @version :1.0
 */
@Data
@ToString
public class PageParams {
    //当前页码
    private long pageNumber=1L;
    //每页默认值
    private long pageSize=10L;
    public PageParams() {}
    public PageParams(long pageNumber, long pageSize) {
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }
}
