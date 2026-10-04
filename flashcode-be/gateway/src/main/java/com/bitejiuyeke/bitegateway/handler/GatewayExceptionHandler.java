package com.bitejiuyeke.bitegateway.handler;

import com.bitejiuyeke.bitecommoncore.utils.ServletUtil;
import com.bitejiuyeke.bitecommondomain.domain.ResultCode;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


/**
 * 网关统一异常处理
 */
@Order(-1)
@Configuration
@Slf4j
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    /**
     * 处理器
     *
     * @param exchange ServerWebExchange
     * @param ex 异常信息
     * @return 无
     */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        //响应已经提交到客户端，无法再对这个响应进行常规的异常处理修改了，直接返回一个包含原始异常ex的Mono.error(ex)
        if (response.isCommitted()) {
            return Mono.error(ex);
        }
        int retCode = ResultCode.ERROR.getCode();
        String retMsg = ResultCode.ERROR.getMsg();
        if (ex instanceof NoResourceFoundException) {
            retCode = ResultCode.SERVICE_NOT_FOUND.getCode();
            retMsg = ResultCode.SERVICE_NOT_FOUND.getMsg();
        } else if (ex instanceof ServiceException) {
            retMsg = ex.getMessage();
            retCode = ((ServiceException) ex).getCode();
        }

        int httpCode = Integer.parseInt(String.valueOf(retCode).substring(0,3));

        log.error("[网关异常处理]请求路径:{},异常信息:{}", exchange.getRequest().
                getPath(), ex.getMessage());

        return ServletUtil.webFluxResponseWriter(response, HttpStatus.valueOf(httpCode),retMsg, retCode);
    }
}