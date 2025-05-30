package com.learningonline.content.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learningonline.base.exception.CommonError;
import com.learningonline.base.exception.LearningPlatformException;
import com.learningonline.base.model.PageParams;
import com.learningonline.base.model.PageResult;
import com.learningonline.content.mapper.*;
import com.learningonline.content.model.dto.AddCourseDto;
import com.learningonline.content.model.dto.CourseBaseInfoDto;
import com.learningonline.content.model.dto.EditCourseDto;
import com.learningonline.content.model.dto.QueryCourseParamsDto;
import com.learningonline.content.model.pojo.*;
import com.learningonline.content.service.CourseBaseInfoService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 课程基本信息业务实现类
 *
 * @author :yhc
 * @version :1.0
 * @since :2025-4-16-19:26
 */
@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {
    @Autowired
    private CourseBaseMapper courseBaseMapper;
    @Autowired
    private CourseCategoryMapper courseCategoryMapper;
    @Autowired
    private CourseMarketMapper courseMarketMapper;
    @Autowired
    private TeachplanMapper teachplanMapper;
    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;
    @Autowired
    TeachplanWorkMapper teachplanWorkMapper;
    @Autowired
    CourseTeacherMapper courseTeacherMapper;

    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto) {
        //动态构建查询条件
        //构建查询条件对象
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        //按课程名称、审核状态、发布状态搜索
        queryWrapper
                .like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()),
                        CourseBase::getName, queryCourseParamsDto.getCourseName())
                .eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()),
                        CourseBase::getAuditStatus, queryCourseParamsDto.getAuditStatus())
                .eq(StringUtils.isNotEmpty(queryCourseParamsDto.getPublishStatus()),
                        CourseBase::getStatus, queryCourseParamsDto.getPublishStatus());
        //构建分页对象
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        //调用mapper获取查询结果
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);
        //构建业务返回结果
        List<CourseBase> courseBaseList = pageResult.getRecords();
        long total = pageResult.getTotal();
        return new PageResult<>(courseBaseList, total,
                pageParams.getPageNo(), pageParams.getPageSize());
    }


    @Override
    @Transactional
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto dto) {

        //向base表插入数据
        //新增对象
        CourseBase courseBaseNew = new CourseBase();
        //将填写的课程信息赋值给新增对象
        BeanUtils.copyProperties(dto, courseBaseNew);
        //设置审核状态
        courseBaseNew.setAuditStatus("202002");
        //设置发布状态
        courseBaseNew.setStatus("203001");
        //机构id
        courseBaseNew.setCompanyId(companyId);
        //添加时间
        courseBaseNew.setCreateDate(LocalDateTime.now());
        //插入课程基本信息表
        int insert = courseBaseMapper.insert(courseBaseNew);
        if (insert <= 0) {
            throw new LearningPlatformException("新增课程基本信息失败");
        }
        //向课程营销表保存课程营销信息
        //课程营销信息
        CourseMarket courseMarketNew = new CourseMarket();
        Long courseId = courseBaseNew.getId();
        BeanUtils.copyProperties(dto, courseMarketNew);
        courseMarketNew.setId(courseId);
        int i = saveCourseMarket(courseMarketNew);
        if (i <= 0) {
            throw new LearningPlatformException("保存课程营销信息失败");
        }
        //查询课程基本信息及营销信息并返回
        return getCourseBaseInfo(courseId);

    }

    //保存课程营销信息
    private int saveCourseMarket(CourseMarket courseMarketNew) {
        //收费规则
        String charge = courseMarketNew.getCharge();
        if (StringUtils.isBlank(charge)) {
            throw new LearningPlatformException("收费规则没有选择");
        }
        //收费规则为收费
        if (charge.equals("201001")) {
            if (courseMarketNew.getPrice() == null || courseMarketNew.getPrice().floatValue() <= 0) {
                throw new LearningPlatformException("课程为收费价格不能为空且必须大于0");
            }
        }
        //根据id从课程营销表查询
        CourseMarket courseMarketObj = courseMarketMapper.selectById(courseMarketNew.getId());
        if (courseMarketObj == null) {
            return courseMarketMapper.insert(courseMarketNew);
        } else {
            BeanUtils.copyProperties(courseMarketNew, courseMarketObj);
            courseMarketObj.setId(courseMarketNew.getId());
            return courseMarketMapper.updateById(courseMarketObj);
        }
    }

    //根据课程id查询课程基本信息，包括基本信息和营销信息
    @Override
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId) {
        //从基本信息表查询信息
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null) {
            return null;
        }
        //从营销信息表获取信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        //封装两个信息
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase, courseBaseInfoDto);
        if (courseMarket != null) {
            BeanUtils.copyProperties(courseMarket, courseBaseInfoDto);
        }

        //查询分类名称
        CourseCategory courseCategoryBySt = courseCategoryMapper.selectById(courseBase.getSt());
        courseBaseInfoDto.setStName(courseCategoryBySt.getName());
        CourseCategory courseCategoryByMt = courseCategoryMapper.selectById(courseBase.getMt());
        courseBaseInfoDto.setMtName(courseCategoryByMt.getName());

        return courseBaseInfoDto;

    }

    /**
     * 修改课程基本信息
     *
     * @param companyId     机构id
     * @param editCourseDto 修改课程金额本信息
     * @return com.learningonline.content.model.dto.CourseBaseInfoDto
     */
    @Override
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto) {
       Long courseId = editCourseDto.getId();
       CourseBase courseBase=courseBaseMapper.selectById(courseId);
       if(courseBase==null){
           LearningPlatformException.cast("课程不存在");
       }
       if(!courseBase.getCompanyId().equals(companyId)) {
            LearningPlatformException.cast("该机构不能修改不属于自己的课程");
       }
       BeanUtils.copyProperties(editCourseDto, courseBase);
        //重新设置审核状态
        courseBase.setAuditStatus("202002");
        //重新设置发布状态
        courseBase.setStatus("203001");
        courseBase.setChangeDate(LocalDateTime.now());
        int update = courseBaseMapper.updateById(courseBase);
        if(update<=0){
            throw new LearningPlatformException("修改基本信息失败");
        }
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        if(courseMarket==null){
            LearningPlatformException.cast("课程营销信息不存在");
        }
        BeanUtils.copyProperties(editCourseDto, courseMarket);
        int update2=courseMarketMapper.updateById(courseMarket);
        if(update2<=0){
            throw new LearningPlatformException("修改课程营销信息失败");
        }
        return this.getCourseBaseInfo(courseId);
    }

    /**
     * 删除课程
     *
     * @param companyId 机构id
     * @param courseId  课程id
     */
    @Transactional
    @Override
    public void deleteCourse(Long companyId, Long courseId) {
        //基本信息
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if(courseBase==null){
            LearningPlatformException.cast("课程不存在");
        }
        //验证机构
        if(!courseBase.getCompanyId().equals(companyId)) {
            LearningPlatformException.cast("课程不属于该机构");
        }
        if(courseBase.getAuditStatus().equals("202002")){
            courseBaseMapper.deleteById(courseId);
        }
       else{
           LearningPlatformException.cast("审核状态不是未提交");
        }
        //营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        if(courseMarket!=null){
            courseMarketMapper.deleteById(courseId);
        }
        //课程计划
        LambdaQueryWrapper<Teachplan> teachplanLambdaQueryWrapper = new LambdaQueryWrapper<>();
        teachplanLambdaQueryWrapper.eq(Teachplan::getCourseId, courseId);
        List<Teachplan> teachplanList = teachplanMapper.selectList(teachplanLambdaQueryWrapper);
        if(!teachplanList.isEmpty()){
            teachplanMapper.delete(teachplanLambdaQueryWrapper);
        }
        //课程计划关联信息，media和work
        LambdaQueryWrapper<TeachplanMedia> teachplanMediaLambdaQueryWrapper = new LambdaQueryWrapper<>();
        teachplanMediaLambdaQueryWrapper.eq(TeachplanMedia::getCourseId, courseId);
        List<TeachplanMedia> teachplanMediaList=teachplanMediaMapper.selectList(teachplanMediaLambdaQueryWrapper);
        if(!teachplanMediaList.isEmpty()){
            teachplanMediaMapper.delete(teachplanMediaLambdaQueryWrapper);
        }
        LambdaQueryWrapper<TeachplanWork> teachplanWorkLambdaQueryWrapper = new LambdaQueryWrapper<>();
        teachplanWorkLambdaQueryWrapper.eq(TeachplanWork::getCourseId, courseId);
        List<TeachplanWork> teachplanWorkList = teachplanWorkMapper.selectList(teachplanWorkLambdaQueryWrapper);
        if(!teachplanWorkList.isEmpty()){
            teachplanWorkMapper.delete(teachplanWorkLambdaQueryWrapper);
        }
        //师资信息
        LambdaQueryWrapper<CourseTeacher> courseTeacherLambdaQueryWrapper = new LambdaQueryWrapper<>();
        courseTeacherLambdaQueryWrapper.eq(CourseTeacher::getCourseId, courseId);
        List<CourseTeacher> courseTeacherList=courseTeacherMapper.selectList(courseTeacherLambdaQueryWrapper);
        if(!courseTeacherList.isEmpty()){
            courseTeacherMapper.delete(courseTeacherLambdaQueryWrapper);
        }
    }

}
