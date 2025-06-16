package com.learningonline.ucenter.service;

import com.learningonline.ucenter.model.dto.AuthParamsDto;
import com.learningonline.ucenter.model.dto.XcUserExt;

/**
 * 认证service
 * @author Mr.M
 * @version 1.0
 */
public interface AuthService {

    /**
     *  认证方法
     * @param authParamsDto 认证参数
     * @return com.learningonline.ucenter.model.dto.XcUserExt 用户信息
     * @author yhc
     */
    XcUserExt execute(AuthParamsDto authParamsDto);

}
