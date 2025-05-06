package com.learningonline.content.service.Impl;

import com.learningonline.content.mapper.CourseCategoryMapper;
import com.learningonline.content.model.dto.CourseCategoryTreeDto;
import com.learningonline.content.model.pojo.CourseCategory;
import com.learningonline.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CourseCategoryServiceImpl implements CourseCategoryService {
    @Autowired
    CourseCategoryMapper courseCategoryMapper;

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        //调用分类查询mapper
        List<CourseCategoryTreeDto> allNodes = courseCategoryMapper.selectTreeNodes(id);
        //构建id和node的键值对，方便查找父节点
        Map<String, CourseCategoryTreeDto> nodeMap = allNodes.stream().filter(node -> !node.getId().equals(id))
                .collect(Collectors.toMap(CourseCategory::getId, node -> node, (existing, replacement) -> replacement));
        //构建返回结果
        List<CourseCategoryTreeDto> result = new ArrayList<>();
        allNodes.stream().filter(node -> !node.getId().equals(id))
                .forEach(node -> {
                    //把二级节点直接假如
                    if(node.getParentid().equals(id)) {
                        result.add(node);
                    }
                    //将低层级节点假如子节点中
                    CourseCategoryTreeDto parentNode=nodeMap.get(node.getParentid());
                    if(parentNode!=null) {
                        if(parentNode.getChildrenTreeNodes()==null) {
                            parentNode.setChildrenTreeNodes(new ArrayList<>());
                        }
                        parentNode.getChildrenTreeNodes().add(node);
                    }
                });
        return result;
    }
}
