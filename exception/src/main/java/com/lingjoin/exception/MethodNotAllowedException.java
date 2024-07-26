package com.lingjoin.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 该类表示操作不被允许的异常。
 * MethodNotAllowedException 类表示当请求的操作不允许时抛出的异常。
 * 例如，资源不存在或请求的方法不受支持时会抛出此异常。
 */
public class MethodNotAllowedException extends ResponseStatusException {
    /**
     * 构造一个新的事件。
     * 该方法创建一个新的 MethodNotAllowedException 实例，表示操作不被允许的异常。
     * 错误状态码为 HttpStatus.METHOD_NOT_ALLOWED（405），表示请求的方法不允许。
     * 错误消息为 "操作不被允许, 资源不存在或不可被操作"，用于描述错误的原因。
     */
    public MethodNotAllowedException() {
        super(HttpStatus.METHOD_NOT_ALLOWED, "操作不被允许, 资源不存在或不可被操作", null);
    }
}
