package com.learningonline.content.model.dto;

import com.learningonline.content.model.pojo.CourseTeacher;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * 课程预览数据模型
 * @version 1.0
 */
@Data
@ToString
public class CoursePreviewDto {

    //课程基本信息,课程营销信息
    CourseBaseInfoDto courseBase;


    //课程计划信息
    List<TeachplanDto> teachplans;

    //师资信息
    List<CourseTeacher> courseTeachers;

}
