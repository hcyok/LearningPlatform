package com.learningonline.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learningonline.ucenter.feignclient.CheckCodeClient;
import com.learningonline.ucenter.mapper.XcUserMapper;
import com.learningonline.ucenter.model.dto.AuthParamsDto;
import com.learningonline.ucenter.model.dto.XcUserExt;
import com.learningonline.ucenter.model.po.XcUser;
import com.learningonline.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service("password_authservice")
public class PasswordAuthServiceImpl implements AuthService {
    @Autowired
    XcUserMapper xcUserMapper;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    CheckCodeClient checkCodeClient;

    /**
     * 密码认证方法
     *
     * @param authParamsDto 认证参数
     * @return com.learningonline.ucenter.model.dto.XcUserExt 用户信息
     * @author yhc
     */
    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        //由请求参数获取用户信息
        String username = authParamsDto.getUsername();
        XcUser user = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        if (user == null) {
            log.info("账号不存在:{}", username);
            throw new RuntimeException("账户不存在");
        }
        //校验密码
        String rawPassword = authParamsDto.getPassword();
        String dbPassword = user.getPassword();
        boolean isMatch = passwordEncoder.matches(rawPassword, dbPassword);
        if (!isMatch) {
            throw new RuntimeException("账号或者密码错误");
        }
        //校验验证码
        String checkCode = authParamsDto.getCheckcode();
        String checkCodeKey = authParamsDto.getCheckcodekey();
        boolean check = checkCodeClient.verify(checkCodeKey, checkCode);
        if (!check) {
            throw new RuntimeException("验证码错误");
        }
        //封装返回信息
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(user, xcUserExt);
        return xcUserExt;
    }
}
