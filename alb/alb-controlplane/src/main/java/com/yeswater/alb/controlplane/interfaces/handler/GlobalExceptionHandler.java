package com.yeswater.alb.controlplane.interfaces.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException ex) {
        LOGGER.warn("ALB 控制面业务拒绝, status={}, message={}", ex.getStatusCode().value(), ex.getReason());
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("message", ex.getReason() == null ? ex.getStatusCode().toString() : ex.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("参数校验失败");
        LOGGER.warn("ALB 控制面参数校验失败, message={}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        LOGGER.error("ALB 控制面系统异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "服务内部异常"));
    }
}
