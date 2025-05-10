package com.learningonline.content.api.controller;

import com.learningonline.base.exception.ValidationGroups;
import com.learningonline.base.model.PageParams;
import com.learningonline.base.model.PageResult;
import com.learningonline.content.model.dto.AddCourseDto;
import com.learningonline.content.model.dto.CourseBaseInfoDto;
import com.learningonline.content.model.dto.EditCourseDto;
import com.learningonline.content.model.dto.QueryCourseParamsDto;
import com.learningonline.content.model.pojo.CourseBase;
import com.learningonline.content.service.CourseBaseInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 课程信息接口
 * @author :yhc
 * @version :1.0
 */
@Api(value = "课程基本信息接口",tags = "课程基本信息接口")
@RestController
public class CourseBaseInfoController {
    @Autowired
    private CourseBaseInfoService courseBaseInfoService;
    @ApiOperation("课程查询接口")
    @PostMapping("/course/list")
    public PageResult<CourseBase> queryCourseList(
            PageParams pageParams, @RequestBody(required = false) QueryCourseParamsDto queryCourseParamsDto) {
        return courseBaseInfoService.queryCourseBaseList(pageParams, queryCourseParamsDto);
    }
    @ApiOperation("新增课程")
    @PostMapping("/course")
    public CourseBaseInfoDto addCourse(@RequestBody@Validated(value = ValidationGroups.Inster.class) AddCourseDto addCourseDto) {
        Long companyId = 1232141425L;
        return courseBaseInfoService.createCourseBase(companyId, addCourseDto);
    }
    @ApiOperation("根据id查询课程")
    @GetMapping("/course/{courseId}")
    public CourseBaseInfoDto queryCourseById(@PathVariable("courseId") Long courseId) {
        return courseBaseInfoService.getCourseBaseInfo(courseId);
    }
    @ApiOperation("修改课程基础信息")
    @PutMapping("/course")
    public CourseBaseInfoDto modifyCourseBase(@RequestBody @Validated EditCourseDto editCourseDto){
        //机构id，由于认证系统没有上线暂时硬编码
        Long companyId = 1232141425L;
        return courseBaseInfoService.updateCourseBase(companyId,editCourseDto);
    }

}
