package com.learningonline.content.api.controller;

import com.learningonline.content.model.pojo.CourseTeacher;
import com.learningonline.content.service.CourseTeacherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程师资编辑接口
 * @author yhc
 * @version 1.0
 */
@Api(value = "课程师资编辑接口",tags = "课程师资编辑接口")
@RestController
public class CourseTeacherController {
    @Autowired
    private CourseTeacherService courseTeacherService;
    @ApiOperation(value = "查询课程下所有教师")
    @GetMapping("/courseTeacher/list/{courseId}")
    public List<CourseTeacher> queryAllCourseTeacher(@PathVariable("courseId") Long courseId) {

        return courseTeacherService.queryAllCourseTeacher(courseId);
    }
    @ApiOperation(value = "修改或新增教师")
    @PostMapping("/courseTeacher")
    public CourseTeacher saveCourseTeacher(@RequestBody CourseTeacher courseTeacher) {
        Long companyId = 1232141425L;
        return courseTeacherService.saveCourseTeacher(companyId,courseTeacher);
    }
    @ApiOperation(value = "删除教师")
    @DeleteMapping("/courseTeacher/course/{courseId}/{teacherId}")
    public void deleteCourseTeacher(@PathVariable("courseId") Long courseId,@PathVariable("teacherId") Long teacherId) {
        Long companyId = 1232141425L;
        courseTeacherService.deleteCourseTeacher(companyId,courseId,teacherId);
    }
}
