package com.learningonline.content.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Data;

@Data
@ApiModel(value = "EditCourseDto",description = "修改课程信息基本信息")
public class EditCourseDto extends AddCourseDto{
    @ApiModelProperty(value="课程id",required = true)
    private Long id;
}
