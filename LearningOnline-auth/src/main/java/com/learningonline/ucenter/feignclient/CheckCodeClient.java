package com.learningonline.ucenter.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Mr.M
 * @version 1.0
 * @description 远程调用验证码服务
 * @date 2023/2/23 22:59
 */
@FeignClient(value = "checkcode",fallbackFactory = CheckCodeClientFactory.class)
public interface CheckCodeClient {

    @PostMapping(value = "/checkcode/verify")
    public Boolean verify(@RequestParam("key") String key, @RequestParam("code") String code);

}