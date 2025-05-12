package com.learningonline.content.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.learningonline.base.exception.LearningPlatformException;
import com.learningonline.content.LearningOnlineContentServiceApplication;
import com.learningonline.content.mapper.CourseBaseMapper;
import com.learningonline.content.mapper.CourseTeacherMapper;
import com.learningonline.content.model.pojo.CourseBase;
import com.learningonline.content.model.pojo.CourseTeacher;
import com.learningonline.content.service.CourseTeacherService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CourseTeacherServiceImpl implements CourseTeacherService {
    @Autowired
    private CourseTeacherMapper courseTeacherMapper;
    @Autowired
    private CourseBaseMapper courseBaseMapper;
    @Autowired
    private LearningOnlineContentServiceApplication learningOnlineContentServiceApplication;

    /**
     * 查询课程下所有教师
     *
     * @param courseId 课程id
     * @return com.learningonline.content.model.pojo.CourseTeacher
     */
    @Override
    public List<CourseTeacher> queryAllCourseTeacher(Long courseId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId, courseId);
        return courseTeacherMapper.selectList(queryWrapper);
    }

    /**
     * 新增或修改课程师资
     *
     * @param companyId     机构id
     * @param courseTeacherDto 新增或修改内容
     * @return com.learningonline.content.model.pojo.CourseTeacher
     */
    @Override
    public CourseTeacher saveCourseTeacher(Long companyId, CourseTeacher courseTeacherDto) {
       //验证机构
        Long courseId = courseTeacherDto.getCourseId();
        CourseBase courseBase =courseBaseMapper.selectById(courseId) ;
        if(!courseBase.getCompanyId().equals(companyId)){
            LearningPlatformException.cast("机构不能修改不属于自己的课程的师资信息");
        }
        Long teacherId=courseTeacherDto.getId();
        if(teacherId==null){
            //新增
            CourseTeacher courseTeacherNew=new CourseTeacher();
            BeanUtils.copyProperties(courseTeacherDto,courseTeacherNew);
            courseTeacherNew.setCreateDate(LocalDateTime.now());
            courseTeacherMapper.insert(courseTeacherNew);
            return courseTeacherMapper.selectById(courseTeacherNew.getId());
        }
        else{
            //修改
            CourseTeacher courseTeacher=courseTeacherMapper.selectById(teacherId);
            if(courseTeacher==null){
                LearningPlatformException.cast("该老师信息不存在");
            }
            CourseTeacher courseTeacherUpdate=new CourseTeacher();
            BeanUtils.copyProperties(courseTeacherDto,courseTeacherUpdate);
            int i=courseTeacherMapper.updateById(courseTeacherUpdate);
            if(i<1){
                LearningPlatformException.cast("修改老师信息失败");
            }
            return courseTeacherMapper.selectById(teacherId);
        }
    }

    /**
     * 删除教师
     *
     * @param companyId 机构id
     * @param courseId  课程id
     * @param teacherId 教师id
     */
    @Override
    public void deleteCourseTeacher(Long companyId, Long courseId, Long teacherId) {
        //验证机构
        CourseBase courseBase =courseBaseMapper.selectById(courseId) ;
        if(!courseBase.getCompanyId().equals(companyId)){
            LearningPlatformException.cast("机构不能修改不属于自己的课程的师资信息");
        }
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId, courseId)
                .eq(CourseTeacher::getId, teacherId);
        CourseTeacher courseTeacher=courseTeacherMapper.selectOne(queryWrapper);
        if(courseTeacher==null){
            LearningPlatformException.cast("该教师不存在");
        }else{
            int i=courseTeacherMapper.delete(queryWrapper);
            if(i<1){
                LearningPlatformException.cast("删除失败");
            }
        }
    }
}
