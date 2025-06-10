package com.learningonline.content.jobhandler;

import com.learningonline.base.exception.LearningPlatformException;
import com.learningonline.content.feignclient.SearchServiceClient;
import com.learningonline.content.mapper.CoursePublishMapper;
import com.learningonline.content.model.dto.CourseIndex;
import com.learningonline.content.model.pojo.CoursePublish;
import com.learningonline.content.service.CoursePublishService;
import com.learningonline.messagesdk.model.po.MqMessage;
import com.learningonline.messagesdk.service.MessageProcessAbstract;
import com.learningonline.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * 课程发布任务处理
 */
@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {
    @Autowired
    CoursePublishService coursePublishService;
    @Autowired
    CoursePublishMapper coursePublishMapper;
    @Autowired
    SearchServiceClient searchServiceClient;

    /**
     * @param mqMessage 执行任务内容
     * @return boolean true:处理成功，false处理失败
     *  任务处理
     */
    @Override
    public boolean execute(MqMessage mqMessage) {
        //查询关联业务：课程id
        String businessKey1= mqMessage.getBusinessKey1();
        long courseId = Long.parseLong(businessKey1);
        //课程模板静态化
        generateCourseHtml(mqMessage,courseId);
        //课程索引
        saveCourseIndex(mqMessage,courseId);
        //课程缓存
        saveCourseCache(mqMessage,courseId);
        return true;

    }

    /**
     * 生成静态化页面上传到minio
     * @param mqMessage 事务消息
     * @param courseId 课程id
     */
    private void generateCourseHtml(MqMessage mqMessage, long courseId) {
        log.debug("开始进行课程静态化,课程id:{}",courseId);
        long messageId=mqMessage.getId();
        //消息幂等性处理
        MqMessageService messageService=this.getMqMessageService();
        int stageOne=messageService.getStageOne(messageId);
        if(stageOne>0){
            log.debug("课程静态化已经处理，courseId:{}",courseId);
            return;
        }
        //生成静态化页面
        File file = coursePublishService.generateCourseHtml(courseId);
        //上传静态化页面
        if(file!=null){
            coursePublishService.uploadCourseHtml(courseId,file);
        }
        messageService.completedStageOne(messageId);
    }

    /**
     * 保存缓存到redis
     * @param mqMessage
     * @param courseId
     */
    private void saveCourseCache(MqMessage mqMessage, long courseId) {
        log.debug("将课程信息缓存至redis,课程id:{}",courseId);
        long messageId=mqMessage.getId();
        MqMessageService messageService=this.getMqMessageService();
        int stageTwo=messageService.getStageThree(messageId);
        if(stageTwo>0){
            log.debug("课程缓存已经处理，courseId:{}",courseId);
            return;
        }
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        messageService.getStageThree(messageId);
    }

    /**
     * 保存索引到elestaicsearch
     * @param mqMessage
     * @param courseId
     */
    private void saveCourseIndex(MqMessage mqMessage, long courseId) {
        log.debug("保存课程索引信息,课程id:{}",courseId);
        long messageId=mqMessage.getId();
        MqMessageService messageService=this.getMqMessageService();
        int stageThree=messageService.getStageTwo(messageId);
        if(stageThree>0){
            log.debug("课程索引已经处理，courseId:{}",courseId);
            return;
        }
        Boolean result = saveCourseIndex(courseId);
        if(result){
            //保存第一阶段状态
            messageService.completedStageTwo(messageId);
        }

    }

    /**
     * 保存索引
     * @param courseId
     * @return
     */
    private Boolean saveCourseIndex(Long courseId) {

        //取出课程发布信息
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        //拷贝至课程索引对象
        CourseIndex courseIndex = new CourseIndex();
        BeanUtils.copyProperties(coursePublish,courseIndex);
        //远程调用搜索服务api添加课程信息到索引
        Boolean add = searchServiceClient.add(courseIndex);
        if(!add){
            LearningPlatformException.cast("添加索引失败");
        }
        return add;

    }


    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.debug("shardIndex={},shardTotal={}", shardIndex, shardTotal);
        //参数:分片序号、分片总数、消息类型、一次最多取到的任务数量、一次任务调度执行的超时时间
        process(shardIndex,shardTotal,"course_publish",30,60);
    }


}
