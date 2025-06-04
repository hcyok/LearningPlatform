package com.learningonline.content.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.learningonline.base.exception.CommonError;
import com.learningonline.base.exception.LearningPlatformException;
import com.learningonline.content.mapper.TeachplanMapper;
import com.learningonline.content.mapper.TeachplanMediaMapper;
import com.learningonline.content.model.dto.BindTeachplanMediaDto;
import com.learningonline.content.model.dto.SaveTeachplanDto;
import com.learningonline.content.model.dto.TeachplanDto;
import com.learningonline.content.model.pojo.Teachplan;
import com.learningonline.content.model.pojo.TeachplanMedia;
import com.learningonline.content.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Slf4j
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
     * 课程计划排序,交换顺序
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
        //是否有其他同级子目录
        int count = getTeachplanCount(courseId, parentId);
        if (count > 1) {
            //得到升序排列的同级子目录
            List<Teachplan> teachplanList = new ArrayList<>();
            LambdaQueryWrapper<Teachplan> teachplanQueryWrapper = new LambdaQueryWrapper<>();
            teachplanQueryWrapper.eq(Teachplan::getCourseId, courseId)
                    .eq(Teachplan::getParentid, parentId)
                    .orderByAsc(Teachplan::getOrderby);
            teachplanList = teachplanMapper.selectList(teachplanQueryWrapper);
            //比较位置
            int indexOfTeachplan = teachplanList.indexOf(teachplanCurrent);
            if (moveType.equals("movedown")) {
                if (indexOfTeachplan == teachplanList.size() - 1) {
                    LearningPlatformException.cast("已经在最下边了，别移动了");
                }
                Teachplan teachplanTarget = teachplanList.get(indexOfTeachplan + 1);
                swapOrderby(teachplanCurrent, teachplanTarget);
            } else if (moveType.equals("moveup")) {
                if (indexOfTeachplan == 0) {
                    LearningPlatformException.cast("已经在最上边了，不能再移动了");
                }
                Teachplan teachplanTarget = teachplanList.get(indexOfTeachplan - 1);
                swapOrderby(teachplanCurrent, teachplanTarget);
            } else LearningPlatformException.cast("移动参数出错");
        } else LearningPlatformException.cast("该目录下可移动的课程计划");
    }

    /**
     * 教学计划绑定视频
     *
     * @param bindTeachplanMediaDto
     * @return com.learningonline.content.model.pojo.TeachplanMedia
     */
    @Transactional
    @Override
    public TeachplanMedia associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto) {
        Long teachplanId = bindTeachplanMediaDto.getTeachplanId();
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if(teachplan == null) {
            LearningPlatformException.cast("课程计划不存在");
        }
        Integer grade=teachplan.getGrade();
        if(grade!=2){
            LearningPlatformException.cast("只有第二级计划才可以添加媒资");
        }

        LambdaQueryWrapper<TeachplanMedia> teachplanMediaQueryWrapper = new LambdaQueryWrapper<>();
        teachplanMediaQueryWrapper.eq(TeachplanMedia::getTeachplanId, teachplanId);
        if(teachplanMediaMapper.selectCount(teachplanMediaQueryWrapper) >0) {
            teachplanMediaMapper.delete(teachplanMediaQueryWrapper);
        }
        Long courseId = teachplan.getCourseId();
        TeachplanMedia teachplanMedia = new TeachplanMedia();
        teachplanMedia.setTeachplanId(teachplanId);
        teachplanMedia.setCourseId(courseId);
        teachplanMedia.setMediaId(bindTeachplanMediaDto.getMediaId());
        teachplanMedia.setMediaFilename(bindTeachplanMediaDto.getFileName());
        teachplanMedia.setCreateDate(LocalDateTime.now());
        int i=teachplanMediaMapper.insert(teachplanMedia);
        if(i==0){
            log.error("保存数据库信息失败：{}",teachplanMedia);
            LearningPlatformException.cast("绑定失败");
        }
        return teachplanMedia;
    }

    /**
     * 解绑教学计划和媒资
     *
     * @param teachPlanId
     * @param mediaId
     */
    @Override
    public void unassociationMedia(Long teachPlanId, String mediaId) {
        Teachplan teachplan = teachplanMapper.selectById(teachPlanId);
        if(teachplan == null) {
            LearningPlatformException.cast("课程计划不存在");
        }
        LambdaQueryWrapper<TeachplanMedia> teachplanMediaQueryWrapper = new LambdaQueryWrapper<>();
        teachplanMediaQueryWrapper.eq(TeachplanMedia::getMediaId, mediaId)
                .eq(TeachplanMedia::getTeachplanId, teachPlanId);
        int d=teachplanMediaMapper.delete(teachplanMediaQueryWrapper);
        if(d==0){
            log.error("解绑失败,计划id:{}",teachPlanId);
            LearningPlatformException.cast("删除失败");
        }
    }

    /**
     * 交换课程计划orderby
     *
     * @param teachplanCurrent 原计划
     * @param teachplanTarget  要交换的计划
     */
    private void swapOrderby(Teachplan teachplanCurrent, Teachplan teachplanTarget) {
        int temOderby = teachplanCurrent.getOrderby();
        teachplanCurrent.setOrderby(teachplanTarget.getOrderby());
        teachplanTarget.setOrderby(temOderby);
        teachplanCurrent.setChangeDate(LocalDateTime.now());
        teachplanTarget.setChangeDate(LocalDateTime.now());
        teachplanMapper.updateById(teachplanCurrent);
        teachplanMapper.updateById(teachplanTarget);
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
