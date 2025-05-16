package com.learningonline.media.model.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author yhc
 * @since 2025-05-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MqMessageHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private String id;

    /**
     * 消息类型代码
     */
    private String messageType;

    /**
     * 关联业务信息
     */
    private String businessKey1;

    /**
     * 关联业务信息
     */
    private String businessKey2;

    /**
     * 关联业务信息
     */
    private String businessKey3;

    /**
     * 消息队列主机
     */
    private String mqHost;

    /**
     * 消息队列端口
     */
    private Integer mqPort;

    /**
     * 消息队列虚拟主机
     */
    private String mqVirtualhost;

    /**
     * 队列名称
     */
    private String mqQueue;

    /**
     * 通知次数
     */
    private Integer informNum;

    /**
     * 处理状态，0:初始，1:成功，2:失败
     */
    private Integer state;

    /**
     * 回复失败时间
     */
    private LocalDateTime returnfailureDate;

    /**
     * 回复成功时间
     */
    private LocalDateTime returnsuccessDate;

    /**
     * 回复失败内容
     */
    private String returnfailureMsg;

    /**
     * 最近通知时间
     */
    private LocalDateTime informDate;

    private String stageState1;

    private String stageState2;

    private String stageState3;

    private String stageState4;


}
