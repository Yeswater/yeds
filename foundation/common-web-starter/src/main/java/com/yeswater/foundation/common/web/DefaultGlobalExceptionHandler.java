package com.yeswater.foundation.common.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
public class DefaultGlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultGlobalExceptionHandler.class);

    private final List<ExceptionMapper> exceptionMappers;
    private final YedsWebErrorProperties properties;

    public DefaultGlobalExceptionHandler(List<ExceptionMapper> exceptionMappers, YedsWebErrorProperties properties) {
        this.exceptionMappers = exceptionMappers;
        this.properties = properties;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
        ResolvedException resolvedException = new ResolvedException(
                exception.getStatus(),
                exception.getMessage(),
                true
        );
        return buildAndLog(exception, resolvedException);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = fieldError == null ? "请求参数不合法" : fieldError.getField() + " " + fieldError.getDefaultMessage();
        ResolvedException resolvedException = new ResolvedException(HttpStatus.BAD_REQUEST, message, true);
        return buildAndLog(exception, resolvedException);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        ResolvedException resolvedException = resolveByMapper(exception);
        return buildAndLog(exception, resolvedException);
    }

    private ResolvedException resolveByMapper(Throwable throwable) {
        for (ExceptionMapper exceptionMapper : exceptionMappers) {
            if (exceptionMapper.supports(throwable)) {
                return exceptionMapper.resolve(throwable);
            }
        }
        return new ResolvedException(HttpStatus.INTERNAL_SERVER_ERROR, properties.getInternalMessage(), false);
    }

    private ResponseEntity<ErrorResponse> buildAndLog(Throwable throwable, ResolvedException resolvedException) {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_KEY);
        if (resolvedException.isBusiness()) {
            if (properties.isBusinessStackTraceEnabled()) {
                LOGGER.warn("业务异常, traceId={}, status={}, message={}",
                        traceId,
                        resolvedException.getStatus().value(),
                        resolvedException.getMessage(),
                        throwable);
            } else {
                LOGGER.warn("业务异常, traceId={}, status={}, message={}",
                        traceId,
                        resolvedException.getStatus().value(),
                        resolvedException.getMessage());
            }
        } else {
            LOGGER.error("系统异常, traceId={}, status={}, message={}",
                    traceId,
                    resolvedException.getStatus().value(),
                    throwable.getMessage(),
                    throwable);
        }
        ErrorResponse errorResponse = new ErrorResponse(
                resolvedException.getStatus().value(),
                resolvedException.getMessage(),
                traceId,
                OffsetDateTime.now().toString()
        );
        return ResponseEntity.status(resolvedException.getStatus()).body(errorResponse);
    }
}
