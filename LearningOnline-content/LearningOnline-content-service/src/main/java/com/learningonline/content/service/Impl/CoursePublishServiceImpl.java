package com.learningonline.content.service.Impl;

import com.learningonline.content.model.dto.CourseBaseInfoDto;
import com.learningonline.content.model.dto.CoursePreviewDto;
import com.learningonline.content.model.dto.TeachplanDto;
import com.learningonline.content.model.pojo.CourseTeacher;
import com.learningonline.content.service.CourseBaseInfoService;
import com.learningonline.content.service.CoursePublishService;
import com.learningonline.content.service.CourseTeacherService;
import com.learningonline.content.service.TeachplanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CoursePublishServiceImpl implements CoursePublishService {
    @Autowired
    CourseBaseInfoService courseBaseInfoService;
    @Autowired
    TeachplanService teachplanService;
    @Autowired
    CourseTeacherService courseTeacherService;
    /**
     * 获取课程预览信息
     *
     * @param courseId 课程id
     * @return com.learningonline.content.model.dto.CoursePreviewDto
     */
    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {
        CourseBaseInfoDto courseBaseInfo=courseBaseInfoService.getCourseBaseInfo(courseId);
        List<TeachplanDto> teachplans=teachplanService.findTeachplanTree(courseId);
        List<CourseTeacher> courseTeachers=courseTeacherService.queryAllCourseTeacher(courseId);
        CoursePreviewDto coursePreviewDto=new CoursePreviewDto();
        coursePreviewDto.setCourseBase(courseBaseInfo);
        coursePreviewDto.setTeachplans(teachplans);
        coursePreviewDto.setCourseTeachers(courseTeachers);
        return coursePreviewDto;
    }
}
