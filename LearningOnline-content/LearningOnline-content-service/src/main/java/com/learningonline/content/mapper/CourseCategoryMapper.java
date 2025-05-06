package com.learningonline.content.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learningonline.content.model.dto.CourseCategoryTreeDto;
import com.learningonline.content.model.pojo.CourseCategory;

import java.util.List;

/**
 * <p>
 * 课程分类 Mapper 接口
 * </p>
 *
 * @author yhc
 * @since 2025-04-07
 */
public interface CourseCategoryMapper extends BaseMapper<CourseCategory> {
    public List<CourseCategoryTreeDto> selectTreeNodes(String id);
}
