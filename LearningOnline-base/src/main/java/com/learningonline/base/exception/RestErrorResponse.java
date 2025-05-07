package com.learningonline.base.exception;

import java.io.Serializable;

/**
 * 异常结果封装
 */
public class RestErrorResponse implements Serializable {
   private String errMessage;
   public RestErrorResponse(String errMessage) {
       this.errMessage = errMessage;
   }
   public String getErrMessage() {
       return errMessage;
   }
   public void setErrMessage(String errMessage) {
       this.errMessage = errMessage;
   }
}
