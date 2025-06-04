package com.learningonline.media.service;

import com.learningonline.media.model.pojo.MediaProcess;

import java.time.Duration;
import java.util.List;

/**
 * @author yhc
 * @version 1.0
 *  媒资文件处理业务方法
 *
 */
public interface MediaFileProcessService {

    /**
     *  分片获取待处理任务
     * @param shardIndex 分片序号
     * @param shardTotal 分片总数
     * @param count 获取记录数
     * @return java.util.List<com.learningonline.media.model.pojo.MediaProcess>
     * @author yhc
     */
     List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count);

    /**
     * 开启一个任务
     * @param id 任务id
     * @return true为成功，false为失败
     */
    boolean startTask(long id);
    /**
     *  保存任务结果
     * @param taskId  任务id
     * @param status 任务状态
     * @param fileId  文件id
     * @param url url
     * @param errorMsg 错误信息
     */
    void saveProcessFinishStatus(Long taskId,String status,String fileId,String url,String errorMsg);

    /**
     * 查询超时任务
     * @param shardIndex 分片序号
     * @param shardTotal 分片总数
     * @param interval 超时间隔
     * @return
     */
    List<MediaProcess> getTimeoutList(int shardIndex, int shardTotal, Duration interval);

    /**
     * 处理超时任务
     */
    boolean handleTimeoutTask(Long taskId);
}

