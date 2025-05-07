package com.learningonline.base.exception;

/**
 * 自定义异常类
 *
 * @author yhc
 * @version 1.0
 */
public class LearningPlatformException extends RuntimeException {
    private String errMessage;

    public LearningPlatformException() {
        super();
    }

    public LearningPlatformException(String errMessage) {
        super(errMessage);
        this.errMessage = errMessage;
    }

    public String getErrMessage() {
        return errMessage;
    }
    //抛出自定义的异常
    public static void cast(CommonError error) {
        throw new LearningPlatformException(error.getErrMessage());
    }
    //抛出自由格式的错误信息
    public static void cast( String errMessage) {
        throw new LearningPlatformException(errMessage);
    }
}
