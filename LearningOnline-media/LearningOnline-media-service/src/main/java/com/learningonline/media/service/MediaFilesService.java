package com.learningonline.media.service;

import com.learningonline.base.model.PageParams;
import com.learningonline.base.model.PageResult;
import com.learningonline.media.model.dto.QueryMediaParamsDto;
import com.learningonline.media.model.dto.UploadFileParamsDto;
import com.learningonline.media.model.dto.UploadFileResultDto;
import com.learningonline.media.model.pojo.MediaFiles;
import org.springframework.web.multipart.MultipartFile;

/**
 * 媒资管理服务
 */
public interface MediaFilesService {
    /**
     * 上传普通文件
     * @param uploadFileParamsDto 上传的文件信息
     * @param companyId 机构ID
     * @param LocalFilePath 上传文件的路径
     * @return com.learningonline.media.model.dto.UploadFileResultDto
     */
    UploadFileResultDto uploadFile(UploadFileParamsDto uploadFileParamsDto,Long companyId,String LocalFilePath);

    /**
     *查询媒资列表
     * @param companyId 机构id
     * @param pageParams 分页参数
     * @param queryMediaParamsDto 请求查询参数
     * @return
     */
    PageResult<MediaFiles> queryMediaFiles(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto);
    /**
     * 向数据库保存文件信息
     * @param uploadFileParamsDto 文件信息
     * @param companyId 机构id
     * @param fileMd5 文件md5
     * @param objectName bucket中对象名
     * @param bucket minio bucket
     * @return com.learningonline.media.model.pojo.MediaFiles
     */
     MediaFiles addMediaFilesToDB(UploadFileParamsDto uploadFileParamsDto,Long companyId,String fileMd5,
                                        String objectName,String bucket);
}
