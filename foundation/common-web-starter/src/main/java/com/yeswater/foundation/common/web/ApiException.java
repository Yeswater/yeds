package com.yeswater.foundation.common.web;

import org.springframework.http.HttpStatus;

/**
 * 通用业务异常。
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
