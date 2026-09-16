package com.zifang.z.config.web.config;//package com.zifang.exception;


import com.zifang.util.core.lang.exception.BaseException;
import com.zifang.util.core.meta.BaseStatusCode;
import com.zifang.util.core.meta.Result;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LogManager.getLogger(GlobalExceptionHandler.class);

//    @Resource
//    private List<WebExceptionHandler>  webExceptionHandlers;

    @ResponseBody
    @ExceptionHandler(value = BaseException.class)
    public Result<?> handle(BaseException e) {
        return Result.fail(e.getMessage()).code(e.getStatusCode().getCode());
    }

    @ResponseBody
    @ExceptionHandler(value = Exception.class)
    public Result<?> handle(Exception e) {
        log.error("Unhandled exception in z-config-web", e);
        return Result.fail(e.getMessage()).code(BaseStatusCode.FAIL.getCode());
    }
}
