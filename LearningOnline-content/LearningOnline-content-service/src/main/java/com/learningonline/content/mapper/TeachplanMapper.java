package com.learningonline.content.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learningonline.content.model.dto.TeachplanDto;
import com.learningonline.content.model.pojo.Teachplan;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 课程计划 Mapper 接口
 * </p>
 *
 * @author yhc
 * @since 2025-04-07
 */
public interface TeachplanMapper extends BaseMapper<Teachplan> {
    /**
     * 查询某课程的课程计划，组成树型结构
     * @param courseId 课程id
     * @return com.learningonline.content.model.dto.TeachplanDto
     *
     *
     */
    public List<TeachplanDto> selectTreeNodes(long courseId);

    /**
     * 查询某个课程同级课程计划的最大orderby
     * @param courseId 课程id
     * @param parentId 父节点id
     *
     */
    public int getMaxOrderBy(@Param("courseId") long courseId, @Param("parentId") long parentId);

}
