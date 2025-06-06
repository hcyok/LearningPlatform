package com.learningonline.content.service;

import com.learningonline.content.model.dto.CoursePreviewDto;

/**
 * 课程预览、发布接口
 * @author yhc
 * @version 1.0
 */
public interface CoursePublishService {


    /**
     *获取课程预览信息
     * @param courseId 课程id
     * @return com.learningonline.content.model.dto.CoursePreviewDto
     */
    public CoursePreviewDto getCoursePreviewInfo(Long courseId);


}
