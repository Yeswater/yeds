package com.yeswater.bids.export.infrastructure.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ExportExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handle(ApiException exception) {
        if (exception.getStatus().is5xxServerError()) {
            LOGGER.error("导出服务异常, status={}, message={}", exception.getStatus().value(), exception.getMessage(), exception);
        } else {
            LOGGER.warn("导出服务业务拒绝, status={}, message={}", exception.getStatus().value(), exception.getMessage());
        }
        return ResponseEntity.status(exception.getStatus())
                .body(Map.of("message", exception.getMessage()));
    }
}
