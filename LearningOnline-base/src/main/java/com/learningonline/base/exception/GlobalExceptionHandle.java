package com.learningonline.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 全局异常处理器
 * @version 1.0
 * @author yhc
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandle {
    //处理系统异常
    @ExceptionHandler(value = Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public RestErrorResponse handleException(Exception e) {
        log.error("【系统异常】{}",e.getMessage(), e);
        return new RestErrorResponse(CommonError.UNKNOWN_ERROR.getErrMessage());
    }
    //处理自定义异常
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = LearningPlatformException.class)
    public RestErrorResponse handleLearningPlatformException(LearningPlatformException e) {
        log.error("【系统异常】{}",e.getMessage(), e);
        return new RestErrorResponse(e.getErrMessage());
    }
}
