package com.bitejiuyeke.bitecommonsecurity.handler;

import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommondomain.domain.ResultCode;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 设置http响应码
     *
     * @param response 响应信息
     * @param errcode 响应码
     */
    private void setResponseCode(HttpServletResponse response,Integer errcode){
        int httpCode = Integer.parseInt(String.valueOf(errcode).substring(0,3));
        response.setStatus(httpCode);
    }

    /**
     * 请求方式不支持
     * @param e 异常信息
     * @param request 请求
     * @param response 响应
     * @return 异常结果
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<?> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',不支持'{}'请求", requestURI, e.getMethod());
        setResponseCode(response, ResultCode.REQUEST_METNHOD_NOT_SUPPORTED.getCode());
        return R.fail(ResultCode.REQUEST_METNHOD_NOT_SUPPORTED.getCode(), ResultCode.REQUEST_METNHOD_NOT_SUPPORTED.getMsg());
    }


    /**
     * 类型不匹配异常
     *
     * @param e 异常信息
     * @param response 响应
     * @return 不匹配结果
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    public R<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e,
                                                          HttpServletResponse response) {
        log.error("类型不匹配异常",e);
        setResponseCode(response, ResultCode.PARA_TYPE_MISMATCH.getCode());
        return R.fail(ResultCode.PARA_TYPE_MISMATCH.getCode(), ResultCode.PARA_TYPE_MISMATCH.getMsg());
    }

    /**
     * url未找到异常
     *
     * @param e 异常信息
     * @param response 响应
     * @return 异常结果
     */
    @ExceptionHandler({NoResourceFoundException.class})
    public R<?> handleMethodNoResourceFoundException(NoResourceFoundException e,
                                                     HttpServletResponse response) {
        log.error("url未找到异常",e);
        setResponseCode(response, ResultCode.URL_NOT_FOUND.getCode());
        return R.fail(ResultCode.URL_NOT_FOUND.getCode(), ResultCode.URL_NOT_FOUND.getMsg());
    }

    /**
     * 业务异常
     *
     * @param e 异常信息
     * @param request 请求
     * @param response 响应
     * @return 业务异常结果
     */
    @ExceptionHandler(ServiceException.class)
    public R<?> handleServiceException(ServiceException e, HttpServletRequest request,
                                       HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生业务异常", requestURI,  e);
        setResponseCode(response,e.getCode());
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常
     * @param e 异常信息
     * @param request 请求
     * @param response 响应
     * @return 异常报文
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request,
                                       HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生参数校验异常", requestURI, e);
        setResponseCode(response, ResultCode.INVALID_PARA.getCode());
        String message = joinMessage(e);
        return R.fail(ResultCode.INVALID_PARA.getCode(), message);
    }

    private String joinMessage(MethodArgumentNotValidException e) {
        List<ObjectError> allErrors = e.getAllErrors();
        if (CollectionUtils.isEmpty(allErrors)) {
            return "";
        }
//        List<String> collect = allErrors.stream().map(ObjectError::getDefaultMessage).collect(Collectors.toList());
        return allErrors.stream().map(ObjectError::getDefaultMessage).collect(Collectors.joining(", "));
    }

    /**
     * 参数校验异常
     * @param e 异常信息
     * @param request 请求
     * @param response 响应
     * @return 异常报文
     */
    @ExceptionHandler({ConstraintViolationException.class})
    public R<Void> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request,
                                                      HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}', 发生参数校验异常",requestURI, e);
        setResponseCode(response,ResultCode.INVALID_PARA.getCode());
        String message = e.getMessage();
        return R.fail(ResultCode.INVALID_PARA.getCode(),message);
    }


    /**
     * 拦截运行时异常
     *
     * @param e 异常信息
     * @param request 请求信息
     * @param response 响应信息
     * @return 响应结果
     */
    @ExceptionHandler(RuntimeException.class)
    public R<?> handleRuntimeException(RuntimeException e, HttpServletRequest request,
                                       HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生运行时异常.", requestURI, e);
        setResponseCode(response, ResultCode.ERROR.getCode());
        return R.fail(ResultCode.ERROR.getCode(), ResultCode.ERROR.getMsg());
    }

    /**
     * 系统异常
     * @param e 异常信息
     * @param request 请求
     * @param response 响应
     * @return 响应结果
     */
    @ExceptionHandler(Exception.class)
    public R<?> handleException(Exception e, HttpServletRequest request,
                                HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生异常.", requestURI, e);
        setResponseCode(response, ResultCode.ERROR.getCode());
        return R.fail(ResultCode.ERROR.getCode(), ResultCode.ERROR.getMsg());
    }
}