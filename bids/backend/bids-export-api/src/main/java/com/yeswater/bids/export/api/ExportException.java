package com.yeswater.bids.export.api;

/**
 * 导出服务调用异常。
 */
public class ExportException extends RuntimeException {
    private final int httpStatus;

    public ExportException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
