package com.learningonline.content.service;

import com.learningonline.base.model.PageParams;
import com.learningonline.base.model.PageResult;
import com.learningonline.content.model.dto.AddCourseDto;
import com.learningonline.content.model.dto.CourseBaseInfoDto;
import com.learningonline.content.model.dto.EditCourseDto;
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

    /**
     *  根据课程id查询基本信息和营销信息
     * @param courseId 课程id
     * @return com.learningonline.content.model.dto.CourseBaseInfoDto
     * @author yhc
     *
     */
    CourseBaseInfoDto getCourseBaseInfo(Long courseId);

    /**
     * 修改课程基本信息
     * @param companyId 机构id
     * @param editCourseDto 修改课程金额本信息
     * @return com.learningonline.content.model.dto.CourseBaseInfoDto
     */
    CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto);

    /**
     * 删除课程
     * @param companyId 机构id
     * @param courseId 课程id
     */
    void deleteCourse(Long companyId, Long courseId);
}
