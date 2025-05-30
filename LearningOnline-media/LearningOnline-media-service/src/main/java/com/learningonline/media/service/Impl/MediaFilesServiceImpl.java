package com.learningonline.media.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learningonline.base.exception.LearningPlatformException;
import com.learningonline.base.model.PageParams;
import com.learningonline.base.model.PageResult;
import com.learningonline.base.model.RestResponse;
import com.learningonline.media.mapper.MediaFilesMapper;
import com.learningonline.media.mapper.MediaProcessMapper;
import com.learningonline.media.model.dto.QueryMediaParamsDto;
import com.learningonline.media.model.dto.UploadFileParamsDto;
import com.learningonline.media.model.dto.UploadFileResultDto;
import com.learningonline.media.model.pojo.MediaFiles;
import com.learningonline.media.model.pojo.MediaProcess;
import com.learningonline.media.service.MediaFilesService;

import io.minio.*;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.errors.*;
import io.minio.messages.Bucket;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.cli.Digest;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class MediaFilesServiceImpl implements MediaFilesService {
    @Autowired
    MediaFilesMapper mediaFilesMapper;
    @Autowired
    MinioClient minioClient;
    @Value("${minio.bucket.files}")
    String bucket_files;//普通文件存储bucket
    @Value("${minio.bucket.videofiles}")
    String bucket_videos;//视频存储bucket
    @Autowired
    MediaProcessMapper mediaProcessMapper;


    /**
     * 查询媒资列表
     *
     * @param companyId           机构id
     * @param pageParams          分页参数
     * @param queryMediaParamsDto 请求查询参数
     * @return com.learningonline.base.model.PageResult<com.learningonline.media.model.pojo.MediaFiles></>
     */
    @Override
    public PageResult<MediaFiles> queryMediaFiles(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {
        //构造查询条件
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<MediaFiles>();
        queryWrapper.eq(MediaFiles::getCompanyId, companyId)
                .eq(StringUtils.isNotEmpty(queryMediaParamsDto.getFileType()), MediaFiles::getFileType, queryMediaParamsDto.getFileType())
                .like(StringUtils.isNotEmpty(queryMediaParamsDto.getFilename()), MediaFiles::getFilename, queryMediaParamsDto.getFilename())
                .eq(StringUtils.isNotEmpty(queryMediaParamsDto.getAuditStatus()), MediaFiles::getAuditStatus, queryMediaParamsDto.getAuditStatus());
        //构造分页参数
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        //查询分页对象
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        //构造分页返回结果
        List<MediaFiles> mediaFilesList = pageResult.getRecords();
        long total = pageResult.getTotal();
        return new PageResult<>(mediaFilesList, total, pageResult.getSize(), pageResult.getCurrent());

    }

    /**
     * 上传普通文件
     *
     * @param uploadFileParamsDto 上传的文件信息
     * @param companyId           机构ID
     * @param LocalFilePath       上传文件的路径
     * @return com.learningonline.media.model.dto.UploadFileResultDto
     */
    @Override
    public UploadFileResultDto uploadFile(UploadFileParamsDto uploadFileParamsDto, Long companyId, String LocalFilePath) {
        //上传文件到minio
        //需要mimeType,bucket,objectName,filePath
        String fileName = uploadFileParamsDto.getFilename();
        String extension = fileName.substring(fileName.lastIndexOf("."));
        String mimeType = getMimeType(extension);
        //objectName要是唯一的，由bucket下的子目录加fileMD5加拓展名组成
        File file = new File(LocalFilePath);
        String fileMd5 = getFileMd5(file);
        String folder = getDefaultFolderPath();
        String objectName = folder + fileMd5 + extension;
        boolean isUpload = uploadFileToMinio(mimeType, objectName, bucket_files, LocalFilePath);
        //将文件信息保存到数据库
        MediaFiles mediaFiles = ((MediaFilesService) AopContext.currentProxy()).addMediaFilesToDB(uploadFileParamsDto, companyId, fileMd5, objectName, bucket_files);
        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);
        return uploadFileResultDto;
    }


    /**
     * 获得文件的mimeType
     *
     * @param extension 文件的拓展名
     * @return mimeType
     */
    private String getMimeType(String extension) {
        //mimeType由拓展名得到
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (extension == null) {
            extension = "";
        }
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;
    }

    /**
     * 获取普通文件在minio的默认存储子路径：按年月日
     *
     * @return 文件在minio的默认存储子路径
     */
    private String getDefaultFolderPath() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date()).replace("-", "/") + "/";
    }

    /**
     * @param file 上传的文件
     * @return 文件mad5
     */
    private String getFileMd5(File file) {
        try (FileInputStream inputStream = new FileInputStream(file);) {
            return DigestUtils.md5Hex(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 向minio上传文件
     *
     * @param mimeType      文件内容类型
     * @param objectName    minio存储的对象名
     * @param bucket        minio桶
     * @param localFilePath 文件路径
     * @return
     */
    private boolean uploadFileToMinio(String mimeType, String objectName, String bucket, String localFilePath) {
        try {
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket(bucket).
                    object(objectName)
                    .filename(localFilePath)
                    .contentType(mimeType).build();
            minioClient.uploadObject(uploadObjectArgs);
            log.debug("上传文件到minio成功，objectName:{},bucket:{},", objectName, bucket);
            System.out.println("上传成功");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上传文件到minio出错，bucket:{},objectName:{},错误原因:{}", bucket, objectName, e.getMessage());
            LearningPlatformException.cast("上传文件到文件系统失败");
            return false;
        }
    }

    /**
     * 向数据库保存文件信息
     *
     * @param uploadFileParamsDto 文件信息
     * @param companyId           机构id
     * @param fileMd5             文件md5
     * @param objectName          bucket中对象名
     * @param bucket              minio bucket
     * @return com.learningonline.media.model.pojo.MediaFiles
     */
    @Transactional
    public MediaFiles addMediaFilesToDB(UploadFileParamsDto uploadFileParamsDto, Long companyId, String fileMd5,
                                        String objectName, String bucket) {
        //需要上传的文件信息，companyId，文件md5作为id,objectName作为file_path，url为bucket加file_path
        MediaFiles mediaFile = mediaFilesMapper.selectById(fileMd5);
        if (mediaFile == null) {
            mediaFile = new MediaFiles();
            BeanUtils.copyProperties(uploadFileParamsDto, mediaFile);
            mediaFile.setId(fileMd5);
            mediaFile.setCompanyId(companyId);
            mediaFile.setFileId(fileMd5);
            mediaFile.setFilePath(objectName);
            mediaFile.setBucket(bucket);
            mediaFile.setUrl("/" + bucket + "/" + objectName);
            mediaFile.setAuditStatus("002003");
            mediaFile.setStatus("1");
            mediaFile.setCreateDate(LocalDateTime.now());
            int i = mediaFilesMapper.insert(mediaFile);
            if (i <= 0) {
                log.error("数据库保存文件信息失败:{}", mediaFile.toString());
                LearningPlatformException.cast("保存文件信息失败");
            }
            //向文件处理表添加待处理任务
            addWaitingTask(mediaFile);
            log.debug("文件信息保存成功:{}", mediaFile.toString());
        }
        return mediaFile;
    }

    /**
     * 添加待处理任务
     *
     * @param mediaFile 文件信息
     */
    private void addWaitingTask(MediaFiles mediaFile) {
        /*如果要处理多种类型的文件，可以定义一个枚举类
         * 从枚举类中匹配类型
         */
        //选择处理avi视频
        String fileName = mediaFile.getFilename();
        String extension = fileName.substring(fileName.lastIndexOf("."));
        String mimeType = getMimeType(extension);
        if (mimeType.equals("video/x-msvideo")) {
            MediaProcess mediaProcess = new MediaProcess();
            BeanUtils.copyProperties(mediaFile, mediaProcess);
            mediaProcess.setStatus("1");
            mediaProcess.setCreateDate(LocalDateTime.now());
            mediaProcess.setFailCount(0);
            int i = mediaProcessMapper.insert(mediaProcess);
            if (i > 0) {
                log.debug("添加待处理任务：{}", mediaProcess.toString());
            }
        }
    }

    /**
     * 检查文件
     *
     * @param fileMd5 文件MD5
     * @return com.learningonline.base.model.RestResponse<Boolean></Boolean>
     */
    @Override
    public RestResponse<Boolean> checkFile(String fileMd5) {
        //检查数据库信息
        MediaFiles mediaFile = mediaFilesMapper.selectById(fileMd5);
        if (mediaFile == null) {
            return RestResponse.success(false);
        }
        //检查minio信息
        /*
        getObject() 方法在对象存在时会返回一个非空的 InputStream，在对象不存在时会直接抛出 NoSuchKeyException（而不是返回 null）
         */
        try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(mediaFile.getBucket())
                .object(mediaFile.getFilePath())//对象名也就是存储路径
                .build())) {
            if (stream == null) {
                return RestResponse.success(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("检查文件失败:{},文件:{}", e.getMessage(), fileMd5);
        }
        return RestResponse.success(true);
    }

    /**
     * 检查minio中的文件分片
     *
     * @param fileMd5    文件MD5
     * @param chunkIndex 分片序号
     * @return com.learningonline.base.model.RestResponse<Boolean></Boolean>
     */
    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {
        //对象为存储路径加序号
        String chunkPath = getChunkFileFolderPath(fileMd5);
        try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket_videos)
                .object(chunkPath + chunkIndex)
                .build())) {
            if (stream != null) {
                return RestResponse.success(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return RestResponse.success(false);
    }

    /**
     * 得到分片文件的父目录
     *
     * @param fileMd5
     * @return
     */
    private String getChunkFileFolderPath(String fileMd5) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + "chunk" + "/";
    }


    /**
     * 上传分片
     *
     * @param fileMd5        文件MD5
     * @param chunkIndex     分片序号
     * @param localChunkPath 分片本地地址
     * @return com.learningonline.base.model.RestResponse<Boolean></Boolean>
     */
    @Override
    public RestResponse<Boolean> uploadChunk(String fileMd5, int chunkIndex, String localChunkPath) {
        //获取mimeType
        String mimeType = getMimeType(null);
        //获得objectname:chunkPath+chunkIndex
        String chunkPath = getChunkFileFolderPath(fileMd5);
        String objectName = chunkPath + chunkIndex;
        boolean isUpload = uploadFileToMinio(mimeType, objectName, bucket_videos, localChunkPath);
        if (!isUpload) {
            log.error("上传文件分片失败：bucket：{}，分片：{}", bucket_videos, objectName);
            return RestResponse.success(false, "上传分片失败");
        }
        return RestResponse.success(true);
    }

    /**
     * 合并分片
     *
     * @param fileMd5             文件MD5
     * @param chunkTotal          分片总数
     * @param uploadFileParamsDto 上传文件的信息
     * @param companyId           机构id
     * @return
     */
    @Override
    public RestResponse<Boolean> mergeChunk(String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto, Long companyId) {
        String chunkPath = getChunkFileFolderPath(fileMd5);
        //查询分片列表
        List<ComposeSource> composeSources = Stream.iterate(0, i -> ++i).limit(chunkTotal).map(
                i -> ComposeSource.builder().bucket(bucket_videos).object(chunkPath + i).build()
        ).collect(Collectors.toList());
        //获取合并后的地址
        String fileName = uploadFileParamsDto.getFilename();
        String fileExt = fileName.substring(fileName.lastIndexOf("."));
        String mergeFilePath = getFilePathByMd5(fileMd5, fileExt);
        //合并
        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                .bucket(bucket_videos)
                .sources(composeSources)
                .object(mergeFilePath)//合并的文件
                .build();
        try {
            minioClient.composeObject(composeObjectArgs);
            log.debug("合并成功：{}", mergeFilePath);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("合并失败：{}，bucket:{}，合并文件：{}", e.getMessage(), bucket_videos, mergeFilePath);
            return RestResponse.validfail(false, "合并文件失败");
        }
        //比较一致性
        //下载合并文件
        File mergeFile = downdloadFileFromMinio(bucket_videos, mergeFilePath);
        if (mergeFile == null) {
            log.error("下载合并文件失败，filePath:{}", mergeFilePath);
            return RestResponse.validfail(false, "下载合并文件失败");
        }
        //比较MD5
        String mergeFileMd5 = getFileMd5(mergeFile);
        if (!fileMd5.equals(mergeFileMd5)) {
            log.error("与源文件不一致，合并文件：{}", mergeFilePath);
            mergeFile.delete();
            return RestResponse.validfail(false, "合并与原文件不一致");
        }
        mergeFile.delete();
        //信息入库
        MediaFiles mediaFile = ((MediaFilesService) AopContext.currentProxy()).addMediaFilesToDB(uploadFileParamsDto, companyId, fileMd5, mergeFilePath, bucket_videos);
        if (mediaFile == null) {
            log.error("文件信息入库失败:{},bucket:{}", mergeFilePath, bucket_videos);
            return RestResponse.validfail(false, "文件信息保存失败");
        }
        //清楚分片
        clearChunks(chunkPath, chunkTotal);
        return RestResponse.success(true);
    }

    /**
     * 得到合并后的文件的地址
     *
     * @param fileMd5 文件id即md5值
     * @param fileExt 文件扩展名
     * @return
     */
    private String getFilePathByMd5(String fileMd5, String fileExt) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }

    /**
     * 从minio下载文件
     *
     * @param bucket
     * @param filePath objectname即存储路径
     * @return 下载的文件
     */
    private File downdloadFileFromMinio(String bucket, String filePath) {
        File minioFile = null;
        FileOutputStream fileOutputStream = null;
        try {
            FilterInputStream minioStream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket).object(filePath).build());
            minioFile = File.createTempFile("minio", "merge");
            fileOutputStream = new FileOutputStream(minioFile);
            IOUtils.copy(minioStream, fileOutputStream);
            return minioFile;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 清楚分块
     *
     * @param chunkPath  分片父目录
     * @param chunkTotal 分片总数
     */
    private void clearChunks(String chunkPath, int chunkTotal) {
        try {
            List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i).limit(chunkTotal)
                    .map(i -> new DeleteObject(chunkPath + i)).collect(Collectors.toList());
            RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder()
                    .bucket(bucket_videos)
                    .objects(deleteObjects).build();
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
            results.forEach(result -> {
                DeleteError deleteError = null;
                try {
                    deleteError = result.get();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("清楚分块文件失败,objectname:{},：{}", deleteError.objectName(), e.getMessage());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            log.error("清除文件分片失败:{},chunkPath:{}", e.getMessage(), chunkPath);
        }
    }

}
