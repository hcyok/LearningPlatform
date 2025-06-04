package com.learningonline.media.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learningonline.media.mapper.MediaFilesMapper;
import com.learningonline.media.mapper.MediaProcessHistoryMapper;
import com.learningonline.media.mapper.MediaProcessMapper;
import com.learningonline.media.model.pojo.MediaFiles;
import com.learningonline.media.model.pojo.MediaProcess;
import com.learningonline.media.model.pojo.MediaProcessHistory;
import com.learningonline.media.service.MediaFileProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class MediaFileProcessServiceImpl implements MediaFileProcessService {
    @Autowired
    MediaProcessMapper mediaProcessMapper;
    @Autowired
    MediaFilesMapper mediaFilesMapper;
    @Autowired
    MediaProcessHistoryMapper mediaProcessHistoryMapper;
    /**
     * 分片获取待处理任务
     *
     * @param shardIndex 分片序号
     * @param shardTotal 分片总数
     * @param count      获取记录数
     * @return java.util.List<com.learningonline.media.model.pojo.MediaProcess>
     * @author yhc
     */
    @Override
    public List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count) {
        return mediaProcessMapper.selectListByShardIndex(shardTotal,shardIndex, count);
    }

    /**
     * 开启一个任务
     *
     * @param id 任务id
     * @return true为成功，false为失败
     */
    @Override
    public boolean startTask(long id) {
        return mediaProcessMapper.startTask(id)>0;
    }

    /**
     * 保存任务结果
     *
     * @param taskId   任务id
     * @param status   任务状态
     * @param fileId   文件id
     * @param url      url
     * @param errorMsg 错误信息
     */
    @Transactional
    @Override
    public void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg) {
        //根据状态更新任务结果
        //验证是否存在任务
        MediaProcess mediaProcess=mediaProcessMapper.selectById(taskId);
        if(mediaProcess==null) {
            log.error("任务不存在：{}",taskId);
            return;
        }
        LambdaQueryWrapper<MediaProcess> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(MediaProcess::getId,taskId);
        //处理失败结果
        if(status.equals("3")){
            MediaProcess failureProcess=new MediaProcess();
            failureProcess.setStatus(status);
            failureProcess.setErrormsg(errorMsg);
            failureProcess.setFailCount(mediaProcess.getFailCount()+1);
            mediaProcessMapper.update(failureProcess,wrapper);
            log.debug("任务处理结果：失败，任务信息：{}",failureProcess.toString());
            return ;
        }
        //处理成功，更新状态，删除任务信息，并加入任务历史信息
        if(status.equals("2")){
            //更新媒资表的url
            MediaFiles mediaFile=mediaFilesMapper.selectById(fileId);
            if(mediaFile==null) {
                log.error("对应的媒资不存在：{}",fileId);
                return;
            }
            mediaFile.setUrl(url);
            mediaFilesMapper.updateById(mediaFile);
            //更新任务表
            mediaProcess.setStatus(status);
            mediaProcess.setUrl(url);
            mediaProcess.setFinishDate(LocalDateTime.now());
            mediaProcessMapper.updateById(mediaProcess);
            //添加历史任务
            MediaProcessHistory mediaProcessHistory=new MediaProcessHistory();
            BeanUtils.copyProperties(mediaProcess,mediaProcessHistory);
            //设置新的文件名
            mediaProcessHistory.setFilename(getMPAVideoName(mediaProcess.getFilename(),".mp4"));
            mediaProcessHistoryMapper.insert(mediaProcessHistory);
            //删除任务
            mediaProcessMapper.delete(wrapper);
        }

    }

    /**
     * 查询超时任务
     *
     * @param shardIndex 分片序号
     * @param shardTotal 分片总数
     * @param interval   超时间隔
     * @return
     */
    @Override
    public List<MediaProcess> getTimeoutList(int shardIndex, int shardTotal, Duration interval) {
        return mediaProcessMapper.selectTimeOutList(interval,shardTotal,shardIndex);
    }

    /**
     * 处理超时任务，待完善
     *
     * @param taskId
     */
    @Override
    public boolean handleTimeoutTask(Long taskId) {
        return mediaProcessMapper.updateTimeoutTask(taskId)>0;
    }

    private String getMPAVideoName(String fileName,String extension) {
        int index=fileName.lastIndexOf(".");
        if(index>0) {
            return fileName.substring(0,index)+extension;
        }
        else return null;
    }
}
