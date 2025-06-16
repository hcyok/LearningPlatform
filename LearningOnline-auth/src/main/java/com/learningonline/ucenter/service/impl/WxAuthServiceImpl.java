package com.learningonline.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learningonline.ucenter.mapper.XcUserMapper;
import com.learningonline.ucenter.mapper.XcUserRoleMapper;
import com.learningonline.ucenter.model.dto.AuthParamsDto;
import com.learningonline.ucenter.model.dto.XcUserExt;
import com.learningonline.ucenter.model.po.XcUser;
import com.learningonline.ucenter.model.po.XcUserRole;
import com.learningonline.ucenter.service.AuthService;
import com.learningonline.ucenter.service.WxAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service("wx_authservice")
public class WxAuthServiceImpl implements AuthService, WxAuthService {
    @Autowired
    XcUserMapper xcUserMapper;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    XcUserRoleMapper xcUserRoleMapper;
    @Value("${weixin.appid}")
    String appid;
    @Value("${weixin.secret}")
    String secret;

    /**
     * 微信账户认证
     *
     * @param authParamsDto 认证参数
     * @return com.learningonline.ucenter.model.dto.XcUserExt 用户信息
     * @author yhc
     */
    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        String username = authParamsDto.getUsername();
        XcUser user = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        if (user == null) {
            throw new RuntimeException("账号不存在");
        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(user, xcUserExt);
        return xcUserExt;
    }

    /**
     * 申请微信令牌来获取微信用户信息
     *
     * @param code 微信授权码
     * @return
     */
    @Override
    public XcUser wxAuth(String code) {
        //收到code调用微信接口申请access_token
        Map<String,String> access_token_map=getAccess_token(code);
        if(access_token_map==null){
            return null;
        }
        String access_token=access_token_map.get("access_token");
        String openid=access_token_map.get("openid");
        //通过token获取微信用户信息
        Map<String,String> wx_userInfo_map=getUserinfo(access_token,openid);
        if(wx_userInfo_map==null){
            return null;
        }
        //保存微信用户信息到数据库
        return ((WxAuthServiceImpl)AopContext.currentProxy()).addWxUser(wx_userInfo_map);
    }

    /**
     * 调用微信接口获取token
     *
     * @param code 授权码
     * @return
     */
    private Map<String, String> getAccess_token(String code) {

        String wxUrl_template = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
        //请求微信地址
        String wxUrl = String.format(wxUrl_template, appid, secret, code);
        log.info("调用微信接口申请access_token, url:{}", wxUrl);
        ResponseEntity<String> exchange = restTemplate.exchange(wxUrl, HttpMethod.POST, null, String.class);
        String result = exchange.getBody();
        log.info("调用微信接口申请access_token: 返回值:{}", result);
        Map<String, String> resultMap = JSON.parseObject(result, Map.class);
        return resultMap;
    }

    /**
     * 获取微信用户信息
     *
     * @param access_token
     * @param openid
     * @return
     */
    private Map<String, String> getUserinfo(String access_token, String openid) {

        String wxUrl_template = "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";
        String wxUrl = String.format(wxUrl_template, access_token, openid);
        log.info("调用微信接口申请用户信息, url:{}", wxUrl);
        ResponseEntity<String> exchange = restTemplate.exchange(wxUrl, HttpMethod.POST, null, String.class);
        String result = new String(exchange.getBody().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        log.info("调用微信接口申请用户信息: 返回值:{}", result);
        Map<String, String> resultMap = JSON.parseObject(result, Map.class);
        return resultMap;
    }

    /**
     * 保存用户信息到数据库
     * @param userInfo_map
     * @return
     */
    @Transactional
    public XcUser addWxUser(Map userInfo_map){
        String unionid = userInfo_map.get("unionid").toString();
        //根据unionid查询数据库
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getWxUnionid, unionid));
        if(xcUser!=null){
            return xcUser;
        }
        String userId = UUID.randomUUID().toString();
        xcUser = new XcUser();
        xcUser.setId(userId);
        xcUser.setWxUnionid(unionid);
        //记录从微信得到的昵称
        xcUser.setNickname(userInfo_map.get("nickname").toString());
        xcUser.setUserpic(userInfo_map.get("headimgurl").toString());
        xcUser.setName(userInfo_map.get("nickname").toString());
        xcUser.setUsername(unionid);
        xcUser.setPassword(unionid);
        xcUser.setUtype("101001");//学生类型
        xcUser.setStatus("1");//用户状态
        xcUser.setCreateTime(LocalDateTime.now());
        xcUserMapper.insert(xcUser);
        XcUserRole xcUserRole = new XcUserRole();
        xcUserRole.setId(UUID.randomUUID().toString());
        xcUserRole.setUserId(userId);
        xcUserRole.setRoleId("17");//学生角色
        xcUserRoleMapper.insert(xcUserRole);
        return xcUser;
    }


}