package com.yeswater.foundation.common.web;

/**
 * 统一错误响应。
 */
public class ErrorResponse {

    private final int code;
    private final String message;
    private final String traceId;
    private final String timestamp;

    public ErrorResponse(int code, String message, String traceId, String timestamp) {
        this.code = code;
        this.message = message;
        this.traceId = traceId;
        this.timestamp = timestamp;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
