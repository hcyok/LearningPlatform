package com.learningonline.content.service;

import com.learningonline.content.model.dto.CourseCategoryTreeDto;

import java.util.List;


public interface CourseCategoryService {
    /**
     * 课程分类树形结构查询
     *
     * @return com.learningonline.content.model.dto.CourseCategoryTreeDto
     */
    public List<CourseCategoryTreeDto> queryTreeNodes(String id);
}

