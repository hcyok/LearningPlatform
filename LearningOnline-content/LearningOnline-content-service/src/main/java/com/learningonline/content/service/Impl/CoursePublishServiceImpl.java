package com.learningonline.content.service.Impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learningonline.base.exception.LearningPlatformException;
import com.learningonline.content.mapper.CourseAuditMapper;
import com.learningonline.content.mapper.CourseBaseMapper;
import com.learningonline.content.mapper.CoursePublishMapper;
import com.learningonline.content.mapper.CoursePublishPreMapper;
import com.learningonline.content.model.dto.CourseBaseInfoDto;
import com.learningonline.content.model.dto.CoursePreviewDto;
import com.learningonline.content.model.dto.TeachplanDto;
import com.learningonline.content.model.pojo.*;
import com.learningonline.content.service.CourseBaseInfoService;
import com.learningonline.content.service.CoursePublishService;
import com.learningonline.content.service.CourseTeacherService;
import com.learningonline.content.service.TeachplanService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.LambdaConversionException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CoursePublishServiceImpl implements CoursePublishService {
    @Autowired
    CourseBaseInfoService courseBaseInfoService;
    @Autowired
    TeachplanService teachplanService;
    @Autowired
    CourseTeacherService courseTeacherService;
    @Autowired
    CoursePublishPreMapper coursePublishPreMapper;
    @Autowired
    CourseBaseMapper courseBaseMapper;
    @Autowired
    CoursePublishMapper coursePublishMapper;

    /**
     * 获取课程预览信息
     *
     * @param courseId 课程id
     * @return com.learningonline.content.model.dto.CoursePreviewDto
     */
    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        List<TeachplanDto> teachplans = teachplanService.findTeachplanTree(courseId);
        List<CourseTeacher> courseTeachers = courseTeacherService.queryAllCourseTeacher(courseId);
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        coursePreviewDto.setCourseBase(courseBaseInfo);
        coursePreviewDto.setTeachplans(teachplans);
        coursePreviewDto.setCourseTeachers(courseTeachers);
        return coursePreviewDto;
    }

    /**
     * 提交课程审核
     *
     * @param companyId 机构id
     * @param courseId  课程id
     */
    @Transactional
    @Override
    public void commitAudit(Long companyId, Long courseId) {
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        if (courseBaseInfo == null) {
            LearningPlatformException.cast("课程不存在");
        }
        //对已提交审核的课程不允许提交审核。
        if (courseBaseInfo.getAuditStatus().equals("202003")) {
            LearningPlatformException.cast("课程正在审核种，请稍作等待");
        }
//本机构只允许提交本机构的课程。
        if (!courseBaseInfo.getCompanyId().equals(companyId)) {
            LearningPlatformException.cast("非本机构课程");
        }
//没有上传图片不允许提交审核。
        if (courseBaseInfo.getPic() == null) {
            LearningPlatformException.cast("提交审核失败，请先上传课程图片");
        }
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        BeanUtils.copyProperties(courseBaseInfo, coursePublishPre);
//没有添加课程计划不允许提交审核。
        List<TeachplanDto> teachplans = teachplanService.findTeachplanTree(courseId);
        if (teachplans.isEmpty()) {
            LearningPlatformException.cast("提交审核失败，请先添加课程计划");
        }
        String teachplansString = JSON.toJSONString(teachplans);
        coursePublishPre.setTeachplan(teachplansString);
        List<CourseTeacher> courseTeachers = courseTeacherService.queryAllCourseTeacher(courseId);
        if (courseTeachers.isEmpty()) {
            LearningPlatformException.cast("提交审核失败，请先添加课程教师");
        }
        String courseTeachersString = JSON.toJSONString(courseTeachers);
        coursePublishPre.setTeachers(courseTeachersString);
        coursePublishPre.setCreateDate(LocalDateTime.now());
        coursePublishPre.setStatus("202003");
        coursePublishPre.setCompanyId(companyId);
        CoursePublishPre coursePublishPre1 = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre1 == null) {
            coursePublishPreMapper.insert(coursePublishPre);
        } else {
            coursePublishPreMapper.updateById(coursePublishPre);
        }
        //更新课程进本信息表审核状态
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setAuditStatus("202003");
        courseBaseMapper.updateById(courseBase);
        //向课程审核表插入数据，待完善
    }

    /**
     * 课程发布接口
     *
     * @param companyId 机构id
     * @param courseId  课程id
     */
    @Transactional
    @Override
    public void publish(Long companyId, Long courseId) {
        //查找课程预发布表
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre == null) {
            LearningPlatformException.cast("该课程没有提交审核");
        }
        if(!coursePublishPre.getCompanyId().equals(companyId)){
            LearningPlatformException.cast("非本机构课程");
        }
        //审核是否通过
        if(!coursePublishPre.getStatus().equals("202004")){
            LearningPlatformException.cast("课程审核未通过");
        }
        //插入课程发布表
        //更新课程基本信息表
        saveCoursePublish(courseId);
        //向消息表插入信息
    }

    /**
     * 保存课程发布表信息，更新相关课程发布状态
     * @param courseId
     */
    private void saveCoursePublish(Long courseId){
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPre == null){
            LearningPlatformException.cast("课程预发布数据为空");
        }

        CoursePublish coursePublish = new CoursePublish();

        //拷贝到课程发布对象
        BeanUtils.copyProperties(coursePublishPre,coursePublish);
        coursePublish.setStatus("203002");
        CoursePublish coursePublishUpdate = coursePublishMapper.selectById(courseId);
        if(coursePublishUpdate == null){
            coursePublishMapper.insert(coursePublish);
        }else{
            coursePublishMapper.updateById(coursePublish);
        }
        //更新课程基本表的发布状态
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setStatus("203002");
        courseBaseMapper.updateById(courseBase);

    }

}


