package com.learningonline.content.service;

import com.learningonline.content.model.pojo.CourseTeacher;

import java.util.List;

/**
 * 课程师资服务
 *
 * @author yhc
 * @version 1.0
 */
public interface CourseTeacherService {
    /**
     * 查询课程下所有教师
     *
     * @param courseId 课程id
     * @return com.learningonline.content.model.pojo.CourseTeacher
     */
    public List<CourseTeacher> queryAllCourseTeacher(Long courseId);

    /**
     * 新增或修改课程师资
     *
     * @param companyId     机构id
     * @param courseTeacher 新增或修改内容
     * @return com.learningonline.content.model.pojo.CourseTeacher
     */
    public CourseTeacher saveCourseTeacher(Long companyId, CourseTeacher courseTeacher);

    /**
     * 删除教师
     * @param companyId 机构id
     * @param courseId 课程id
     * @param teacherId 教师id
     */
    public void deleteCourseTeacher(Long companyId, Long courseId, Long teacherId);
}
