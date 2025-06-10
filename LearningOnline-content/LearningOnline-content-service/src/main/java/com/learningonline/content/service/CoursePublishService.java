package com.learningonline.content.service;

import com.learningonline.content.model.dto.CoursePreviewDto;

import java.io.File;

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

    /**
     * 提交课程审核
     * @param companyId 机构id
     * @param courseId 课程id
     */
    public void commitAudit(Long companyId,Long courseId);

    /**
     *  课程发布接口
     * @param companyId 机构id
     * @param courseId 课程id
     */
    public void publish(Long companyId,Long courseId);

    /**
     * 生成课程静态化页面
     * @param courseId 课程id
     * @return 静态化文件html
     */
    public File generateCourseHtml(Long courseId);
    /**
     *  上传课程静态化页面
     * @param file  静态化文件
     * @return void
     */
    public void  uploadCourseHtml(Long courseId,File file);


}
