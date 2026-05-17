package com.yeswater.iam.interfaces.handler;

import com.yeswater.foundation.common.web.ExceptionMapper;
import com.yeswater.foundation.common.web.ResolvedException;
import com.yeswater.iam.domain.exception.BusinessException;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * IAM 业务异常映射。
 */
@Component
public class BusinessExceptionMapper implements ExceptionMapper, Ordered {

    @Override
    public boolean supports(Throwable throwable) {
        return throwable instanceof BusinessException;
    }

    @Override
    public ResolvedException resolve(Throwable throwable) {
        return new ResolvedException(HttpStatus.BAD_REQUEST, throwable.getMessage(), true);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
