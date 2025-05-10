package com.learningonline.content.model.dto;
import java.util.List;
import com.learningonline.content.model.pojo.Teachplan;
import com.learningonline.content.model.pojo.TeachplanMedia;
import lombok.Data;
import lombok.ToString;

/**
 * 课程计划树型结构dto
 * @author yhc
 * @version 1.0
 */
@Data
@ToString
public class TeachplanDto extends Teachplan {

    //课程计划关联的媒资信息
    TeachplanMedia teachplanMedia;

    //子结点
    List<TeachplanDto> teachPlanTreeNodes;

}
