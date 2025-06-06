package com.learningonline.media.service;

import com.learningonline.base.model.PageParams;
import com.learningonline.base.model.PageResult;
import com.learningonline.base.model.RestResponse;
import com.learningonline.media.model.dto.QueryMediaParamsDto;
import com.learningonline.media.model.dto.UploadFileParamsDto;
import com.learningonline.media.model.dto.UploadFileResultDto;
import com.learningonline.media.model.pojo.MediaFiles;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

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

    /**
     * 检查文件
     * @param fileMd5 文件MD5
     * @return com.learningonline.base.model.RestResponse<Boolean></Boolean>
     */
     RestResponse<Boolean> checkFile(String fileMd5);

    /**
     * 检查文件分片
     * @param fileMd5 文件MD5
     * @param chunkIndex 分片序号
     * @return com.learningonline.base.model.RestResponse<Boolean></Boolean>
     */
     RestResponse<Boolean> checkChunk(String fileMd5,int chunkIndex);

    /**
     * 上传分片
     * @param fileMd5 文件MD5
     * @param chunkIndex 分片序号
     * @param localChunkPath 分片本地地址
     * @return com.learningonline.base.model.RestResponse<Boolean></Boolean>
     */
     RestResponse<Boolean> uploadChunk(String fileMd5, int chunkIndex,String localChunkPath);
    /**
     * 从minio下载文件
     *
     * @param bucket
     * @param filePath objectname即存储路径
     * @return 下载的文件
     */
     File downdloadFileFromMinio(String bucket, String filePath);
    /**
     * 向minio上传文件
     *
     * @param mimeType      文件内容类型
     * @param objectName    minio存储的对象名
     * @param bucket        minio桶
     * @param localFilePath 文件路径
     * @return
     */
     boolean uploadFileToMinio(String mimeType, String objectName, String bucket, String localFilePath);

    /**
     * 合并分片
     * @param fileMd5 文件MD5
     * @param chunkTotal 分片总数
     * @param uploadFileParamsDto 上传文件的信息
     * @param companyId 机构id
     * @return
     */
     RestResponse<Boolean> mergeChunk(String fileMd5, int chunkTotal,UploadFileParamsDto uploadFileParamsDto,Long companyId);

    /**
     *
     * @param mediaId
     * @return
     */
    MediaFiles getFileById(String mediaId);
}
