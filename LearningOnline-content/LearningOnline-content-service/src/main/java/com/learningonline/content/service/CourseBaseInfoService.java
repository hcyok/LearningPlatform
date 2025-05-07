package com.learningonline.content.service;

import com.learningonline.base.model.PageParams;
import com.learningonline.base.model.PageResult;
import com.learningonline.content.model.dto.AddCourseDto;
import com.learningonline.content.model.dto.CourseBaseInfoDto;
import com.learningonline.content.model.dto.QueryCourseParamsDto;
import com.learningonline.content.model.pojo.CourseBase;

/**
 * 课程基本信息业务接口
 * @author :yhc
 * @version :1.0
 * @since :2025-4-16-19:10
 */
public interface CourseBaseInfoService {

    /**
     * 课程查询接口
     * @param pageParams 分页参数
     * @param queryCourseParamsDto 查询条件
     * @return com.learningonline.base.model.PageResult<com.learningonline.content.model.pojo.CourseBase>
     */
    PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto);
    /**
     *  添加课程基本信息
     * @param companyId  教学机构id
     * @param addCourseDto  课程基本信息
     * @return com.learningonline.content.model.dto.CourseBaseInfoDto
     * @author yhc
     *
     */
    CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto addCourseDto);

}
