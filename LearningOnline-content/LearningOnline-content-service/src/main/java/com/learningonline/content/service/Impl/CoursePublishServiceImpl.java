package com.learningonline.content.service.Impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learningonline.base.exception.CommonError;
import com.learningonline.base.exception.LearningPlatformException;
import com.learningonline.content.config.MultipartSupportConfig;
import com.learningonline.content.feignclient.MediaServiceClient;
import com.learningonline.content.mapper.CourseAuditMapper;
import com.learningonline.content.mapper.CourseBaseMapper;
import com.learningonline.content.mapper.CoursePublishMapper;
import com.learningonline.content.mapper.CoursePublishPreMapper;
import com.learningonline.content.model.dto.CourseBaseInfoDto;
import com.learningonline.content.model.dto.CoursePreviewDto;
import com.learningonline.content.model.dto.TeachplanDto;
import com.learningonline.content.model.pojo.*;
import com.learningonline.content.service.CourseBaseInfoService;
import com.learningonline.content.service.CoursePublishService;
import com.learningonline.content.service.CourseTeacherService;
import com.learningonline.content.service.TeachplanService;

import com.learningonline.messagesdk.model.po.MqMessage;
import com.learningonline.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CoursePublishServiceImpl implements CoursePublishService {
    @Autowired
    CourseBaseInfoService courseBaseInfoService;
    @Autowired
    TeachplanService teachplanService;
    @Autowired
    CourseTeacherService courseTeacherService;
    @Autowired
    CoursePublishPreMapper coursePublishPreMapper;
    @Autowired
    CourseBaseMapper courseBaseMapper;
    @Autowired
    CoursePublishMapper coursePublishMapper;
    @Autowired
    MqMessageService mqMessageService;
    @Autowired
    MediaServiceClient mediaServiceClient;
    /**
     * 获取课程预览信息
     *
     * @param courseId 课程id
     * @return com.learningonline.content.model.dto.CoursePreviewDto
     */
    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId)  {
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        List<TeachplanDto> teachplans = teachplanService.findTeachplanTree(courseId);
        List<CourseTeacher> courseTeachers = courseTeacherService.queryAllCourseTeacher(courseId);
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        coursePreviewDto.setCourseBase(courseBaseInfo);
        coursePreviewDto.setTeachplans(teachplans);
        coursePreviewDto.setCourseTeachers(courseTeachers);
        return coursePreviewDto;
    }

    /**
     * 提交课程审核
     *
     * @param companyId 机构id
     * @param courseId  课程id
     */
    @Transactional
    @Override
    public void commitAudit(Long companyId, Long courseId) {
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        if (courseBaseInfo == null) {
            LearningPlatformException.cast("课程不存在");
        }
        //对已提交审核的课程不允许提交审核。
        if (courseBaseInfo.getAuditStatus().equals("202003")) {
            LearningPlatformException.cast("课程正在审核种，请稍作等待");
        }
//本机构只允许提交本机构的课程。
        if (!courseBaseInfo.getCompanyId().equals(companyId)) {
            LearningPlatformException.cast("非本机构课程");
        }
//没有上传图片不允许提交审核。
        if (courseBaseInfo.getPic() == null) {
            LearningPlatformException.cast("提交审核失败，请先上传课程图片");
        }
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        BeanUtils.copyProperties(courseBaseInfo, coursePublishPre);
//没有添加课程计划不允许提交审核。
        List<TeachplanDto> teachplans = teachplanService.findTeachplanTree(courseId);
        if (teachplans.isEmpty()) {
            LearningPlatformException.cast("提交审核失败，请先添加课程计划");
        }
        String teachplansString = JSON.toJSONString(teachplans);
        coursePublishPre.setTeachplan(teachplansString);
        List<CourseTeacher> courseTeachers = courseTeacherService.queryAllCourseTeacher(courseId);
        if (courseTeachers.isEmpty()) {
            LearningPlatformException.cast("提交审核失败，请先添加课程教师");
        }
        String courseTeachersString = JSON.toJSONString(courseTeachers);
        coursePublishPre.setTeachers(courseTeachersString);
        coursePublishPre.setCreateDate(LocalDateTime.now());
        coursePublishPre.setStatus("202003");
        coursePublishPre.setCompanyId(companyId);
        CoursePublishPre coursePublishPre1 = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre1 == null) {
            coursePublishPreMapper.insert(coursePublishPre);
        } else {
            coursePublishPreMapper.updateById(coursePublishPre);
        }
        //更新课程进本信息表审核状态
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setAuditStatus("202003");
        courseBaseMapper.updateById(courseBase);
        //向课程审核表插入数据，待完善
    }

    /**
     * 课程发布接口
     *
     * @param companyId 机构id
     * @param courseId  课程id
     */
    @Transactional
    @Override
    public void publish(Long companyId, Long courseId) {
        //查找课程预发布表
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre == null) {
            LearningPlatformException.cast("该课程没有提交审核");
        }
        if(!coursePublishPre.getCompanyId().equals(companyId)){
            LearningPlatformException.cast("非本机构课程");
        }
        //审核是否通过
        if(!coursePublishPre.getStatus().equals("202004")){
            LearningPlatformException.cast("课程审核未通过");
        }
        //插入课程发布表
        //更新课程基本信息表
        saveCoursePublish(courseId);
        //向消息表插入信息
        saveCoursePublishMessage(courseId);

        coursePublishPreMapper.deleteById(courseId);
    }

    /**
     * 生成课程静态化页面
     *
     * @param courseId 课程id
     * @return 静态化文件html
     */
    @Override
    public File generateCourseHtml(Long courseId) {
        //静态化文件
        File htmlFile  = null;

        try {
            //配置freemarker
            Configuration configuration = new Configuration(Configuration.getVersion());

            //加载模板
            //选指定模板路径,classpath下templates下
            //得到classpath路径
            String templatePath = "/templates/";
            configuration.setClassLoaderForTemplateLoading(
                    getClass().getClassLoader(),
                    templatePath
            );

            //指定模板文件名称
            Template template = configuration.getTemplate("course_template.ftl");

            //准备数据
            CoursePreviewDto coursePreviewInfo = this.getCoursePreviewInfo(courseId);

            Map<String, Object> map = new HashMap<>();
            map.put("model", coursePreviewInfo);

            //静态化
            //参数1：模板，参数2：数据模型
            String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
//            System.out.println(content);
            //将静态化内容输出到文件中
            InputStream inputStream = IOUtils.toInputStream(content);
            //创建静态化文件
            htmlFile = File.createTempFile("course",".html");
            log.debug("课程静态化，生成静态文件:{}",htmlFile.getAbsolutePath());
            //输出流
            FileOutputStream outputStream = new FileOutputStream(htmlFile);
            IOUtils.copy(inputStream, outputStream);
        } catch (Exception e) {
            log.error("课程静态化异常:{}",e.toString());
            LearningPlatformException.cast("课程静态化异常");
        }

        return htmlFile;

    }

    /**
     * 上传课程静态化页面
     *
     * @param courseId
     * @param file     静态化文件
     * @return void
     */
    @Override
    public void uploadCourseHtml(Long courseId, File file) {
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        String course = mediaServiceClient.uploadFile(multipartFile, "course/"+courseId+".html");
        if(course==null){
            LearningPlatformException.cast("上传静态文件异常");
        }

    }

    private void saveCoursePublishMessage(Long courseId){
        MqMessage mqMessage = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if(mqMessage==null){
            LearningPlatformException.cast(CommonError.UNKNOWN_ERROR);
        }
    }


    /**
     * 保存课程发布表信息，更新相关课程发布状态
     * @param courseId
     */
    private void saveCoursePublish(Long courseId){
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPre == null){
            LearningPlatformException.cast("课程预发布数据为空");
        }

        CoursePublish coursePublish = new CoursePublish();

        //拷贝到课程发布对象
        BeanUtils.copyProperties(coursePublishPre,coursePublish);
        coursePublish.setStatus("203002");
        CoursePublish coursePublishUpdate = coursePublishMapper.selectById(courseId);
        if(coursePublishUpdate == null){
            coursePublishMapper.insert(coursePublish);
        }else{
            coursePublishMapper.updateById(coursePublish);
        }
        //更新课程基本表的发布状态
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setStatus("203002");
        courseBaseMapper.updateById(courseBase);

    }

}


