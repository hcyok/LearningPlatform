package com.learningonline.media.model.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 媒资信息
 * </p>
 *
 * @author yhc
 * @since 2025-05-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MediaFiles implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件id,md5值
     */
    @TableId(value = "id", type = IdType.AUTO)
    private String id;

    /**
     * 机构ID
     */
    private Long companyId;

    /**
     * 机构名称
     */
    private String companyName;

    /**
     * 文件名称
     */
    private String filename;

    /**
     * 文件类型（图片、文档，视频）
     */
    private String fileType;

    /**
     * 标签
     */
    private String tags;

    /**
     * 存储目录
     */
    private String bucket;

    /**
     * 存储路径
     */
    private String filePath;

    /**
     * 文件id
     */
    private String fileId;

    /**
     * 媒资文件访问地址
     */
    private String url;

    /**
     * 上传人
     */
    private String username;

    /**
     * 上传时间
     */
    private LocalDateTime createDate;

    /**
     * 修改时间
     */
    private LocalDateTime changeDate;

    /**
     * 状态,1:正常，0:不展示
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 审核状态
     */
    private String auditStatus;

    /**
     * 审核意见
     */
    private String auditMind;

    /**
     * 文件大小
     */
    private Long fileSize;


}
