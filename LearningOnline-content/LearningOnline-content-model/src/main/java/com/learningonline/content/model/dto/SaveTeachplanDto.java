package com.learningonline.content.model.dto;


import com.learningonline.content.model.pojo.Teachplan;
import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;

/**
 *  保存课程计划dto，包括新增、修改
 * @author Mr.M
 * @version 1.0
 */
@Data
@ToString
public class SaveTeachplanDto  extends Teachplan{
       @NotEmpty(message = "章节名称不能为空")
        private String pname;
}