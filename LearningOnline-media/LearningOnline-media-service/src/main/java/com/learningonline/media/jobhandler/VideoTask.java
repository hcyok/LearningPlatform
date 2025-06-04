package com.learningonline.media.jobhandler;

import com.learningonline.base.utils.Mp4VideoUtil;
import com.learningonline.media.model.pojo.MediaProcess;
import com.learningonline.media.service.MediaFileProcessService;
import com.learningonline.media.service.MediaFilesService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

/**
 * 时评处理任务
 *
 * @author yhc
 * @version 1.0
 */
@Component
@Slf4j
public class VideoTask {
    @Autowired
    MediaFilesService mediaFilesService;
    @Autowired
    MediaFileProcessService mediaFileProcessService;


    @Value("${videoprocess.ffmpegpath}")
    String ffmpegpath;

    //将avi视频转换成MP4
    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception {
        //获取分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        //查询待处理任务
        //一次最大任务数量设为cpu核心数
        int count = Runtime.getRuntime().availableProcessors();
        List<MediaProcess> mediaProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, count);
        if (mediaProcessList.isEmpty()) {
            log.debug("没有待处理任务");
            return;
        }
        log.debug("待处理任务数：{}", mediaProcessList.size());
        //开启线程池处理任务
        ExecutorService threadPool = Executors.newFixedThreadPool(mediaProcessList.size());
        try{
        //计数器
        CountDownLatch countDownLatch = new CountDownLatch(mediaProcessList.size());
        mediaProcessList.forEach(mediaProcess -> {
            threadPool.execute(() -> {
                try {
                    //抢占任务（获取乐观锁）
                    long taskId = mediaProcess.getId();
                    boolean getTask = mediaFileProcessService.startTask(taskId);
                    if (!getTask) {
                        log.debug("获取任务：{}失败", taskId);
                        return;
                    }
                    log.debug("开始执行任务：{}", mediaProcess);
                    //下载待处理视频
                    File sourceVideo = mediaFilesService.downdloadFileFromMinio(mediaProcess.getBucket(), mediaProcess.getFilePath());
                    if (sourceVideo == null) {
                        log.error("下载处理文件失败:{}", mediaProcess.getUrl());
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", mediaProcess.getFileId(), mediaProcess.getUrl(), "下载视处理频失败");
                        return;
                    }
                    //创建临时文件来保存处理后的视频
                    File tempFile = null;
                    try {
                        tempFile = File.createTempFile("mp4", ".mp4");
                    } catch (IOException e) {
                        log.error("创建临时文件失败");
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", mediaProcess.getFileId(), mediaProcess.getUrl(), "创建临时文件失败");
                        return;
                    }
                    //视频转码
                    String result = "";
                    try {
                        Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegpath, sourceVideo.getAbsolutePath(), tempFile.getName(), tempFile.getAbsolutePath());
                        result = videoUtil.generateMp4();
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("处理视频文件:{},出错:{}", mediaProcess.getFilePath(), e.getMessage());
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", mediaProcess.getFileId(), mediaProcess.getUrl(), e.getMessage());
                        return;
                    }
                    if (!result.equals("success")) {
                        log.error("处理视频失败，文件位置：{}，错误原因：{}", mediaProcess.getUrl(), result);
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", mediaProcess.getFileId(), mediaProcess.getUrl(), result);
                        return;
                    }
                    String objectName = getFilePath(mediaProcess.getFileId(), ".mp4");
                    String url = "/" + mediaProcess.getBucket() + "/" + objectName;
                    //上传视频
                    boolean upload = false;
                    try {
                        upload = mediaFilesService.uploadFileToMinio("video/mp4", objectName, mediaProcess.getBucket(), tempFile.getAbsolutePath());
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("上传文件失败，任务：{}，原因：{}",taskId, e.getMessage() );
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", mediaProcess.getFileId(), mediaProcess.getUrl(), "上传文件失败");
                        return;
                    }
                    if (!upload) {
                        log.error("上传文件失败：{}", url);
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", mediaProcess.getFileId(), mediaProcess.getUrl(), "上传文件失败");
                        return;
                    }
                    //更新处理结果
                    try{
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "2", mediaProcess.getFileId(), url, null);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        log.error("更新处理结果失败，任务：{}，原因：{}",taskId, e.getMessage() );
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", mediaProcess.getFileId(), mediaProcess.getUrl(), "更新处理结果失败");
                        return;
                    }
                } finally {
                    countDownLatch.countDown();
                }

            });
        });
        countDownLatch.await(30, TimeUnit.MINUTES);}
        finally {
            threadPool.shutdown();
        }
    }

    /**
     * 根据MD5和拓展名获取对象名
     *
     * @param fileMd5
     * @param fileExt
     * @return
     */
    private String getFilePath(String fileMd5, String fileExt) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }

    /**
     * 待完善
     * @throws Exception
     */
    @XxlJob("timeoutHandler")
    public void timeoutHandler() throws Exception {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        Duration interval = Duration.ofMinutes(30);
        List<MediaProcess> timeoutProcessList=mediaFileProcessService.getTimeoutList(shardIndex, shardTotal, interval);

    }


}
