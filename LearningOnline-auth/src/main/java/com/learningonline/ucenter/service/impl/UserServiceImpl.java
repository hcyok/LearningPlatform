package com.learningonline.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learningonline.ucenter.mapper.XcUserMapper;
import com.learningonline.ucenter.model.dto.AuthParamsDto;
import com.learningonline.ucenter.model.dto.XcUserExt;
import com.learningonline.ucenter.model.po.XcUser;
import com.learningonline.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 自定义UserDetailsService用来对接Spring Security
 * @author yhc
 * @version 1.0
 */
@Slf4j
@Service
public class UserServiceImpl implements UserDetailsService {
    @Autowired
    XcUserMapper xcUserMapper;
    @Autowired
    ApplicationContext applicationContext;
    /**
     * 根据账号查询用户信息
     * @param userInfo AuthParamsDto类型的json数据
     * @return org.springframework.security.core.userdetails.UserDetails
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String userInfo) throws UsernameNotFoundException {
        AuthParamsDto authParamsDto = new AuthParamsDto();
        try{
            //将认证参数的json数据转换程对象
            authParamsDto = JSON.parseObject(userInfo, AuthParamsDto.class);
        }
        catch (Exception e){
            log.info("认证请求不符合要求：{}",userInfo);
            throw new RuntimeException("认证请求数据格式不对");
        }
        //匹配具体的验证模式开始认证
        String authType = authParamsDto.getAuthType();
        AuthService authService = (AuthService) applicationContext.getBean(authType+"_authservice");
        XcUserExt user=authService.execute(authParamsDto);
        return  getUserPrincipal(user);
    }

    /**
     * 封装用户信息
     * @param user 用户信息
     * @return org.springframework.security.core.userdetails.UserDetails
     */
    public UserDetails getUserPrincipal(XcUserExt user){
        String password=user.getPassword();
        String[] authorities= {"test"};
        user.setPassword(null);
        String userString=JSON.toJSONString(user);
        //拓展令牌中保存的用户信息
        return User.withUsername(userString).password(password).authorities(authorities).build();
    }
}