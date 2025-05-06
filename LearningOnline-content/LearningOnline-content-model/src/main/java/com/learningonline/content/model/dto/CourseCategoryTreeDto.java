package com.learningonline.content.model.dto;

import com.learningonline.content.model.pojo.CourseCategory;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 课程分类树形节点dto
 * @author yhc
 * @since 2025-5-6
 * @version 1.0
 */
@Data
public class CourseCategoryTreeDto extends CourseCategory implements Serializable {
    //子节点
    List<CourseCategoryTreeDto> childrenTreeNodes;
}

