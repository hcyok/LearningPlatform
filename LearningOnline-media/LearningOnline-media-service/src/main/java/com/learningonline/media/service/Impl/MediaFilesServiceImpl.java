package com.learningonline.media.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learningonline.base.exception.LearningPlatformException;
import com.learningonline.base.model.PageParams;
import com.learningonline.base.model.PageResult;
import com.learningonline.media.mapper.MediaFilesMapper;
import com.learningonline.media.model.dto.QueryMediaParamsDto;
import com.learningonline.media.model.dto.UploadFileParamsDto;
import com.learningonline.media.model.dto.UploadFileResultDto;
import com.learningonline.media.model.pojo.MediaFiles;
import com.learningonline.media.service.MediaFilesService;

import io.minio.MinioClient;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.UploadObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.cli.Digest;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

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
    MediaFilesService currentProxy;

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
                .eq(StringUtils.isNotEmpty(queryMediaParamsDto.getFileType()),MediaFiles::getFileType, queryMediaParamsDto.getFileType())
                .like(StringUtils.isNotEmpty(queryMediaParamsDto.getFilename()),MediaFiles::getFilename, queryMediaParamsDto.getFilename())
                .eq(StringUtils.isNotEmpty(queryMediaParamsDto.getAuditStatus()),MediaFiles::getAuditStatus, queryMediaParamsDto.getAuditStatus());
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
        String fileName=uploadFileParamsDto.getFilename();
        String extension=fileName.substring(fileName.lastIndexOf("."));
        String mimeType=getMimeType(extension);
        //objectName要是唯一的，由bucket下的子目录加fileMD5加拓展名组成
        File file=new File(LocalFilePath);
        String fileMd5=getFileMd5(file);
        String folder=getDefaultFolderPath();
        String objectName=folder+fileMd5+extension;
        boolean isUpload=uploadFileToMinio(mimeType,objectName,bucket_files,LocalFilePath);
        //将文件信息保存到数据库
        MediaFiles mediaFiles=currentProxy.addMediaFilesToDB(uploadFileParamsDto,companyId,fileMd5,objectName,bucket_files);
        UploadFileResultDto uploadFileResultDto=new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles,uploadFileResultDto);
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
     * 获取文件在minio的默认存储子路径：按年月日
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
     * @param mimeType 文件内容类型
     * @param objectName minio存储的对象名
     * @param bucket minio桶
     * @param localFilePath 文件路径
     * @return
     */
    private boolean uploadFileToMinio(String mimeType,String objectName,String bucket,String localFilePath) {
        try {
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket(bucket).
                    object(objectName)
                    .filename(localFilePath)
                    .contentType(mimeType).build();
            minioClient.uploadObject(uploadObjectArgs);
            log.debug("上传文件到minio成功，objectName:{},bucket:{},",objectName,bucket);
            System.out.println("上传成功");
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            log.error("上传文件到minio出错，bucket:{},objectName:{},错误原因:{}",bucket,objectName,e.getMessage());
            LearningPlatformException.cast("上传文件到文件系统失败");
            return false;
        }
    }

    /**
     * 向数据库保存文件信息
     * @param uploadFileParamsDto 文件信息
     * @param companyId 机构id
     * @param fileMd5 文件md5
     * @param objectName bucket中对象名
     * @param bucket minio bucket
     * @return com.learningonline.media.model.pojo.MediaFiles
     */
    @Transactional
    public MediaFiles addMediaFilesToDB(UploadFileParamsDto uploadFileParamsDto,Long companyId,String fileMd5,
                                                  String objectName,String bucket) {
        //需要上传的文件信息，companyId，文件md5作为id,objectName作为file_path，url为bucket加file_path
        MediaFiles mediaFile=mediaFilesMapper.selectById(fileMd5);
        if(mediaFile==null){
            mediaFile=new MediaFiles();
            BeanUtils.copyProperties(uploadFileParamsDto,mediaFile);
            mediaFile.setId(fileMd5);
            mediaFile.setCompanyId(companyId);
            mediaFile.setFileId(fileMd5);
            mediaFile.setFilePath(objectName);
            mediaFile.setBucket(bucket);
            mediaFile.setUrl("/"+bucket+"/"+objectName);
            mediaFile.setAuditStatus("002003");
            mediaFile.setStatus("1");
            mediaFile.setCreateDate(LocalDateTime.now());
            int i=mediaFilesMapper.insert(mediaFile);
            if(i<=0){
                log.error("数据库保存文件信息失败:{}",mediaFile.toString());
                LearningPlatformException.cast("保存文件信息失败");
            }
            log.debug("文件信息保存成功:{}",mediaFile.toString());
        }
        return mediaFile;
    }

}
