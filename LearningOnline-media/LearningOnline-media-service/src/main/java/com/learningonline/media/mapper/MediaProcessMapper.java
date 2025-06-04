package com.learningonline.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learningonline.media.model.pojo.MediaProcess;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.Duration;
import java.util.List;


/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author yhc
 * @since 2025-05-14
 */
public interface MediaProcessMapper extends BaseMapper<MediaProcess> {
    /**
     * 分片查询待处理任务
     * @param shardTotal 分片总数
     * @param shardIndex 分片序号
     * @param count 任务数量
     * @return List<com.learningonline.media.model.pojo.MediaProcess></com.learningonline.media.model.pojo.MediaProcess>
     */
    @Select("select * from media_process t where t.id % #{shardTotal}=#{shardIndex} and (t.status='1'or status='3')" +
            "and t.fail_count<3 limit #{count}")
    List<MediaProcess> selectListByShardIndex(@Param("shardTotal") int shardTotal,
                                              @Param("shardIndex") int shardIndex,
                                              @Param("count") int count);

    /**
     * 开启一个任务
     * @param id 任务id
     * @return 更新记录数
     */
    @Update("update media_process m set m.status='4' where (m.status='1' or m.status='3') " +
            "and m.fail_count<3 and m.id=#{id}")
    int startTask(@Param("id") long id);

    /**
     * 分片查询超时任务,待完善
     * @param timeout 超时时间
     * @param shardTotal 分片总数
     * @param shardIndex 分片序号
     * @return List<com.learningonline.media.model.pojo.MediaProcess></com.learningonline.media.model.pojo.MediaProcess>
     */
    @Select("select* FROM media_process\n" +
            "WHERE status='4'and create_date<DATE_SUB(NOW(),INTERVAL #{timeout})and id % #{shardTotal}=#{shardIndex}")
    List<MediaProcess> selectTimeOutList(@Param("timeout") Duration timeout,
                                         @Param("shardTotal") int shardTotal,
                                         @Param("shardIndex") int shardIndex);

    /**
     * 更新超时任务，待完善，不能保证分布式下的并发冲突
     * @param id 任务id
     * @return
     */
    @Update("update media_process m set m.status='3',fail_count=fail_count+1,errormsg='超时' where m.id=#{id} and m.status='4'")
    int updateTimeoutTask(@Param("id") long id);

}
