package com.lingjoin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 自定义 ForbiddenException 类，继承自 ResponseException。
 * 该异常用于表示请求被服务器理解，但是拒绝执行。通常是因为未获得授权或禁止访问资源。
 * 构造方法接收两个参数：错误信息（msg）和原始原因（cause）。
 * 通过调用父类的构造方法，设置 HTTP 状态码为 FORBIDDEN，并将传入的错误信息和原因传递给父类处理。
 */
public class ForbiddenException extends ResponseStatusException {
    @SuppressWarnings("MissingJavadoc")
    public ForbiddenException(String msg, Throwable cause) {
        super(HttpStatus.FORBIDDEN, msg, cause);
    }
}
