package com.learningonline.content.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.learningonline.base.exception.LearningPlatformException;
import com.learningonline.content.mapper.TeachplanMapper;
import com.learningonline.content.mapper.TeachplanMediaMapper;
import com.learningonline.content.model.dto.SaveTeachplanDto;
import com.learningonline.content.model.dto.TeachplanDto;
import com.learningonline.content.model.pojo.Teachplan;
import com.learningonline.content.model.pojo.TeachplanMedia;
import com.learningonline.content.service.TeachplanService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
@Service
public class TeachplanServiceImpl implements TeachplanService {
    @Autowired
    TeachplanMapper teachplanMapper;
    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;
    /**
     * @param courseId 课程id
     * @return List<TeachplanDto>
     * 查询课程计划树型结构
     * @author yhc
     */
    @Override
    public List<TeachplanDto> findTeachplanTree(long courseId) {
        return teachplanMapper.selectTreeNodes(courseId);
    }

    /**
     * 保存课程计划
     *
     * @param teachplanDto 课程计划信息
     * @author yhc
     */
    @Override
    public void saveTeachplan(SaveTeachplanDto teachplanDto) {
        Long id = teachplanDto.getId();
        if (id == null) {
            //新增
            Teachplan teachplanNew = new Teachplan();
            BeanUtils.copyProperties(teachplanDto, teachplanNew);
            teachplanNew.setCreateDate(LocalDateTime.now());
            int count =getTeachplanCount(teachplanDto.getCourseId(),teachplanDto.getParentid());
            teachplanNew.setOrderby(count+1);
            int insert = teachplanMapper.insert(teachplanNew);
            if (insert < 0) {
                LearningPlatformException.cast("新增章节失败");
            }
        } else {
            //修改
            //根据计划id获取当前章节的计划
            Teachplan teachplan = teachplanMapper.selectById(id);
            LambdaUpdateWrapper<Teachplan> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
            //修改章节名称
            lambdaUpdateWrapper.eq(Teachplan::getId, id)
                    .set(StringUtils.isNotEmpty(teachplanDto.getPname()), Teachplan::getPname, teachplanDto.getPname())
                    .set(Teachplan::getChangeDate, LocalDateTime.now());
            int update=teachplanMapper.update(teachplan, lambdaUpdateWrapper);
            if (update < 0) {
                LearningPlatformException.cast("修改章节名称失败");
            }
        }
    }

    /**
     * 获取最新的排序号
     *
     * @param courseId 课程id
     * @param parentId 父课程计划id
     * @return int 最新排序号
     * @author yhc
     */
    private int getTeachplanCount(long courseId, long parentId) {
        LambdaQueryWrapper<Teachplan> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Teachplan::getCourseId, courseId)
                .eq(Teachplan::getParentid, parentId);
        return teachplanMapper.selectCount(lambdaQueryWrapper).intValue();
    }

    /**
     * 删除课程计划
     *
     * @param teachplanId 课程计划id
     * @author yhc
     */
    @Override
    @Transactional
    public void deleteTeachplan(long teachplanId) {
//        课程计划添加成功，如果课程还没有提交时可以删除课程计划。
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if (teachplan == null) {
            LearningPlatformException.cast("课程不存在");
        }
        if(teachplan.getCoursePubId()!=null){
            LearningPlatformException.cast("课程已经发布，无法删除课程计划");
        }
//        删除第一级别的大章节时要求大章节下边没有小章节时方可删除。
        LambdaQueryWrapper<Teachplan> teachplanQueryWrapper = new LambdaQueryWrapper<>();
        teachplanQueryWrapper.eq(Teachplan::getParentid, teachplanId);
            //不应该直接删除的，应该修改status字段，但是由于之前的查询没有匹配恰当的status，所以这里还是直接删除数据
        //子目录数量
        int count=teachplanMapper.selectCount(teachplanQueryWrapper).intValue();
        if(count>0){
            //有子目录
            LearningPlatformException.cast("课程计划信息还有子级信息，无法操作");
        }
        else{
            int delete=teachplanMapper.deleteById(teachplanId);
            if(delete<=0){
                LearningPlatformException.cast("删除课程计划失败");
            }
            //        删除第二级别的小章节的同时需要将teachplan_media表关联的信息也删除。
            LambdaQueryWrapper<TeachplanMedia> teachplanMediaQueryWrapper = new LambdaQueryWrapper<>();
            teachplanMediaQueryWrapper.eq(TeachplanMedia::getTeachplanId, teachplanId);
            if(teachplanMediaMapper.selectCount(teachplanMediaQueryWrapper)>0){
                int deleteTeachplanMedia= teachplanMediaMapper.delete(teachplanMediaQueryWrapper);
                if(deleteTeachplanMedia<=0){
                    LearningPlatformException.cast("课程计划媒资删除失败");
                }
            }
        }

    }
}
