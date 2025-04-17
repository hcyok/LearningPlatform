package com.learningonline.content.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learningonline.base.model.PageParams;
import com.learningonline.base.model.PageResult;
import com.learningonline.content.mapper.CourseBaseMapper;
import com.learningonline.content.model.dto.QueryCourseParamsDto;
import com.learningonline.content.model.pojo.CourseBase;
import com.learningonline.content.service.CourseBaseInfoService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 课程基本信息业务实现类
 * @author :yhc
 * @version :1.0
 * @since :2025-4-16-19:26
 */
@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {
    @Autowired
    private CourseBaseMapper courseBaseMapper;
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
        Page<CourseBase> page=new Page<>(pageParams.getPageNo(),pageParams.getPageSize());
        //调用mapper获取查询结果
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page,queryWrapper);
        //构建业务返回结果
        List<CourseBase> courseBaseList=pageResult.getRecords();
        long total=pageResult.getTotal();
        return new PageResult<>(courseBaseList,total,
                pageParams.getPageNo(),pageParams.getPageSize());
    }
}
