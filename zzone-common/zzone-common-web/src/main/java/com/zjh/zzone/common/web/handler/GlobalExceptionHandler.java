package com.zjh.zzone.common.web.handler;

import com.zjh.zzone.common.core.base.R;
import com.zjh.zzone.common.web.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * TODO
 *
 * @author zjh
 * @date 2025/7/13 16:12
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理业务校验过程中碰到的非法参数异常 该异常基本由{@link org.springframework.util.Assert}抛出
     * @param exception 参数校验异常
     * @return API返回结果对象包装后的错误输出结果
     * @see Assert#hasLength(String, String)
     * @see Assert#hasText(String, String)
     * @see Assert#isTrue(boolean, String)
     * @see Assert#isNull(Object, String)
     * @see Assert#notNull(Object, String)
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.OK)
    public R handleNotFoundException(NotFoundException exception) {
        log.error("资源未找到,ex = {}", exception.getMessage(), exception);
        return R.fail(exception.getMessage());
    }

}
