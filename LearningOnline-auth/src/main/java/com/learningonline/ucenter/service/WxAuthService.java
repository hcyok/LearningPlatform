package com.learningonline.ucenter.service;

import com.learningonline.ucenter.model.po.XcUser;

/**
 * @author yhc
 * @version 1.0
 * @description 微信认证接口
 */
public interface WxAuthService {
    /**
     * 申请微信令牌来获取微信用户信息
     * @param code 微信授权码
     * @return
     */
    public XcUser wxAuth(String code);

}
