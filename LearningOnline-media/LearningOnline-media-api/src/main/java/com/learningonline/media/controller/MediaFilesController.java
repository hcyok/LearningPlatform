package com.learningonline.media.controller;

import com.learningonline.base.model.PageParams;
import com.learningonline.base.model.PageResult;
import com.learningonline.media.model.dto.QueryMediaParamsDto;
import com.learningonline.media.model.dto.UploadFileParamsDto;
import com.learningonline.media.model.dto.UploadFileResultDto;
import com.learningonline.media.model.pojo.MediaFiles;
import com.learningonline.media.service.MediaFilesService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * 媒资管理接口
 * @author yhc
 * @version 1.0
 */
@Api(value = "媒资管理接口",tags = "媒资管理接口")
@RestController
public class MediaFilesController {
    @Autowired
    private MediaFilesService mediaFilesService;
    @ApiOperation("媒资列表查询接口")
    @PostMapping("/files")
    public PageResult<MediaFiles> list(PageParams pageParams, @RequestBody QueryMediaParamsDto queryMediaParamsDto){
        Long companyId = 1232141425L;
        return mediaFilesService.queryMediaFiles(companyId,pageParams,queryMediaParamsDto);
    }

    @ApiOperation("上传图片文件")
    @RequestMapping(value = "/upload/coursefile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadFileResultDto upload(@RequestPart("filedata") MultipartFile filedata,
                                      @RequestParam(value= "objectName",required=false) String objectName) throws IOException {
        Long companyId = 1232141425L;
        UploadFileParamsDto uploadFileParamsDto = new UploadFileParamsDto();
        uploadFileParamsDto.setFilename(filedata.getOriginalFilename());
        uploadFileParamsDto.setFileSize(filedata.getSize());
        uploadFileParamsDto.setFileType("001001");
        //把前端上传的文件保存到服务器的临时目录
        File tempFile=File.createTempFile("minio","temp");
        filedata.transferTo(tempFile);
        String absolutePath = tempFile.getAbsolutePath();
        return mediaFilesService.uploadFile(uploadFileParamsDto,companyId,absolutePath,objectName);
    }

}
