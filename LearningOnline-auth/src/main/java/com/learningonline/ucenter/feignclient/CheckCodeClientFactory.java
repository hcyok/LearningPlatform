package com.learningonline.ucenter.feignclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @author Mr.M
 * @version 1.0
 * @description 降级逻辑
 * @date 2023/2/23 23:00
 */
@Slf4j
@Component
public class CheckCodeClientFactory implements FallbackFactory<CheckCodeClient> {
 @Override
 public CheckCodeClient create(Throwable throwable) {
  return new CheckCodeClient() {

   @Override
   public Boolean verify(String key, String code) {
    log.debug("调用验证码服务熔断异常:{}", throwable.getMessage());
    return null;
   }
  };
 }
}
