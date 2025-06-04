package com.learningonline.media.controller;

import com.learningonline.base.model.RestResponse;
import com.learningonline.media.model.dto.UploadFileParamsDto;
import com.learningonline.media.service.MediaFilesService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * 上传视频
 * @author yhc
 * @version 1.0
 */
@Api(value = "视频上传接口",tags = "视频上传接口")
@RestController
public class BigFileController {
    @Autowired
    MediaFilesService mediaFilesService;
    @ApiOperation(value = "文件检查接口")
    @PostMapping("/upload/checkfile")
    public RestResponse<Boolean> checkFile(@RequestParam("fileMd5")String fileMd5) throws Exception {
        return mediaFilesService.checkFile(fileMd5);
    }
    @ApiOperation(value = "分块文件上传前的检测")
    @PostMapping("/upload/checkchunk")
    public RestResponse<Boolean> checkchunk(@RequestParam("fileMd5") String fileMd5,
                                            @RequestParam("chunk") int chunk) throws Exception {
        return mediaFilesService.checkChunk(fileMd5, chunk);
    }
    @ApiOperation(value = "上传分块文件")
    @PostMapping("/upload/uploadchunk")
    public RestResponse<Boolean> uploadchunk(@RequestParam("file") MultipartFile file,
                                    @RequestParam("fileMd5") String fileMd5,
                                    @RequestParam("chunk") int chunk) throws Exception {
        //保存前端上传的文件到临时文件
        File chunkFile=File.createTempFile("minio","temp");
        file.transferTo(chunkFile);
        String localChunkPath=chunkFile.getAbsolutePath();
        return mediaFilesService.uploadChunk(fileMd5,chunk,localChunkPath);
    }
    @ApiOperation(value = "合并文件")
    @PostMapping("/upload/mergechunks")
    public RestResponse<Boolean> mergechunks(@RequestParam("fileMd5") String fileMd5,
                                    @RequestParam("fileName") String fileName,
                                    @RequestParam("chunkTotal") int chunkTotal) throws Exception {
        Long companyId = 1232141425L;
        UploadFileParamsDto uploadFileParamsDto = new UploadFileParamsDto();
        uploadFileParamsDto.setFilename(fileName);
        uploadFileParamsDto.setFileType("001002");
        uploadFileParamsDto.setRemark("课程视频");
        return mediaFilesService.mergeChunk(fileMd5,chunkTotal,uploadFileParamsDto,companyId);

    }

}
