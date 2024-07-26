package com.lingjoin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 非法参数异常类。
 * 当传入的参数不符合要求时，抛出该异常。
 */
public class IllegalArgException extends ResponseStatusException {

    /**
     * 构造方法，用于创建一个表示非法参数异常的对象，并指定错误状态码为400（Bad Request）。
     * @param message 错误信息，描述异常的原因或具体原因。
     * @param cause 导致该异常的根源异常对象，可为null。
     */
    public IllegalArgException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, message, cause);
    }

    /**
     * 构造方法，用于创建一个表示非法参数异常的对象，并指定错误状态码为400（Bad Request）。
     * @param message 错误信息，描述异常的原因或具体原因。
     */
    public IllegalArgException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
