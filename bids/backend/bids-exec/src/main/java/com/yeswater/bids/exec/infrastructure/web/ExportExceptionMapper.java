package com.yeswater.bids.exec.infrastructure.web;

import com.yeswater.foundation.common.web.ExceptionMapper;
import com.yeswater.foundation.common.web.ResolvedException;
import com.yeswater.bids.export.api.ExportException;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 导出异常映射器。
 */
@Component
public class ExportExceptionMapper implements ExceptionMapper, Ordered {

    @Override
    public boolean supports(Throwable throwable) {
        return throwable instanceof ExportException;
    }

    @Override
    public ResolvedException resolve(Throwable throwable) {
        ExportException exportException = (ExportException) throwable;
        HttpStatus status = HttpStatus.resolve(exportException.getHttpStatus());
        HttpStatus finalStatus = status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
        return new ResolvedException(finalStatus, exportException.getMessage(), true);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
