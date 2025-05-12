package com.learningonline.content.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.learningonline.base.exception.CommonError;
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
import java.util.ArrayList;
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
            int count = teachplanMapper.getMaxOrderBy(teachplanDto.getCourseId(), teachplanDto.getParentid());
            teachplanNew.setOrderby(count + 1);
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
            int update = teachplanMapper.update(teachplan, lambdaUpdateWrapper);
            if (update < 0) {
                LearningPlatformException.cast("修改章节名称失败");
            }
        }
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
        if (teachplan.getCoursePubId() != null) {
            LearningPlatformException.cast("课程已经发布，无法删除课程计划");
        }
//        删除第一级别的大章节时要求大章节下边没有小章节时方可删除。
        LambdaQueryWrapper<Teachplan> teachplanQueryWrapper = new LambdaQueryWrapper<>();
        teachplanQueryWrapper.eq(Teachplan::getParentid, teachplanId);
        //不应该直接删除的，应该修改status字段，但是由于之前的查询没有匹配恰当的status，所以这里还是直接删除数据
        //子目录数量
        int count = teachplanMapper.selectCount(teachplanQueryWrapper).intValue();
        if (count > 0) {
            //有子目录
            LearningPlatformException.cast("课程计划信息还有子级信息，无法操作");
        } else {
            int delete = teachplanMapper.deleteById(teachplanId);
            if (delete <= 0) {
                LearningPlatformException.cast("删除课程计划失败");
            }
            //        删除第二级别的小章节的同时需要将teachplan_media表关联的信息也删除。
            LambdaQueryWrapper<TeachplanMedia> teachplanMediaQueryWrapper = new LambdaQueryWrapper<>();
            teachplanMediaQueryWrapper.eq(TeachplanMedia::getTeachplanId, teachplanId);
            if (teachplanMediaMapper.selectCount(teachplanMediaQueryWrapper) > 0) {
                int deleteTeachplanMedia = teachplanMediaMapper.delete(teachplanMediaQueryWrapper);
                if (deleteTeachplanMedia <= 0) {
                    LearningPlatformException.cast("课程计划媒资删除失败");
                }
            }
        }

    }

    /**
     * 课程计划排序
     *
     * @param moveType    移动类型
     * @param teachplanId 课程计划id
     */
    @Override
    public void MoveTeachplan(String moveType, Long teachplanId) {
        Teachplan teachplanCurrent = teachplanMapper.selectById(teachplanId);
        long parentId = teachplanCurrent.getParentid();
        long courseId = teachplanCurrent.getCourseId();
        int oderby = teachplanCurrent.getOrderby();
        int count = getTeachplanCount(courseId, parentId);
        if (count > 1) {
            List<Teachplan> teachplanList = new ArrayList<>();
            LambdaQueryWrapper<Teachplan> teachplanQueryWrapper = new LambdaQueryWrapper<>();
            teachplanQueryWrapper.eq(Teachplan::getCourseId, courseId)
                    .eq(Teachplan::getParentid, parentId)
                    .orderByAsc(Teachplan::getOrderby);
            teachplanList = teachplanMapper.selectList(teachplanQueryWrapper);
            int indexOfTeachplan = teachplanList.indexOf(teachplanCurrent);
            if (moveType.equals("movedown")) {
                if (indexOfTeachplan == teachplanList.size() - 1) {
                    LearningPlatformException.cast("已经在最下边了，别移动了");
                }
                for (int i = indexOfTeachplan + 1; i < teachplanList.size(); i++) {
                    Teachplan teachplanTarget = teachplanList.get(i);
                   swapOrderby(teachplanCurrent, teachplanTarget);
                }
            } else if (moveType.equals("moveup")) {
                if (indexOfTeachplan == 0) {
                    LearningPlatformException.cast("已经在最上边了，不能再移动了");
                }
                for (int i = indexOfTeachplan - 1; i >= 0; i++) {
                    Teachplan teachplanTarget = teachplanList.get(i);
                    swapOrderby(teachplanCurrent, teachplanTarget);
                }
            } else LearningPlatformException.cast("移动参数出错");
        } else LearningPlatformException.cast("该目录下没有课程计划");
    }

    /**
     * 交换课程计划orderby
     * @param teachplanCurrent 原计划
     * @param teachplanTarget 要交换的计划
     */
    private void swapOrderby(Teachplan teachplanCurrent, Teachplan teachplanTarget) {
        int temOderby = teachplanCurrent.getOrderby();
        teachplanCurrent.setOrderby(teachplanTarget.getOrderby());
        teachplanTarget.setOrderby(temOderby);
        teachplanCurrent.setChangeDate(LocalDateTime.now());
        teachplanTarget.setChangeDate(LocalDateTime.now());
    }

    /**
     * 统计课程计划数量
     *
     * @param courseId 课程id
     * @param parentId 父课程计划id
     * @return int
     */
    private int getTeachplanCount(long courseId, long parentId) {
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId, courseId);
        queryWrapper.eq(Teachplan::getParentid, parentId);
        return teachplanMapper.selectCount(queryWrapper).intValue();
    }

}
