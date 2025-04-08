package com.learningonline.content.model.dto;

import lombok.Data;
import lombok.ToString;

/**
 * 课程查询请求参数dto
 * @author ：yhc
 * @since :2025.4.8
 * @version :1.0
 */
@Data
@ToString
public class QueryCourseParamsDto {
    //审核状态
    private String auditStatus;
    //课程名称
    private String courseName;
    //发布状态
    private String publishStatus;

}
