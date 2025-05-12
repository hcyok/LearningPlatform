package com.learningonline.content.service;

import com.learningonline.content.model.dto.SaveTeachplanDto;
import com.learningonline.content.model.dto.TeachplanDto;

import java.util.List;

/**
 *  课程基本信息管理业务接口
 * @author yhc
 * @version 1.0
 */
public interface TeachplanService {

    /**
     * @param courseId 课程id
     * @return List<TeachplanDto>
     * 查询课程计划树型结构
     * @author yhc
     */
    public List<TeachplanDto> findTeachplanTree(long courseId);

    /**
     *  保存课程计划
     * @param teachplanDto  课程计划信息
     * @author yhc
     */
    public void saveTeachplan(SaveTeachplanDto teachplanDto);

    /**
     * 删除课程计划
     * @param teachplanId 课程计划id
     * @author yhc
     */
    public void deleteTeachplan(long teachplanId);

    /**
     * 课程计划排序
     * @param moveType 移动类型
     * @param teachplanId 课程计划id
     */
    public void MoveTeachplan(String moveType, Long teachplanId);
}