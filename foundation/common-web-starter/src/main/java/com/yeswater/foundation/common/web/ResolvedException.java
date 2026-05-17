package com.yeswater.foundation.common.web;

import org.springframework.http.HttpStatus;

/**
 * 异常映射结果。
 */
public class ResolvedException {

    private final HttpStatus status;
    private final String message;
    private final boolean business;

    public ResolvedException(HttpStatus status, String message, boolean business) {
        this.status = status;
        this.message = message;
        this.business = business;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public boolean isBusiness() {
        return business;
    }
}
