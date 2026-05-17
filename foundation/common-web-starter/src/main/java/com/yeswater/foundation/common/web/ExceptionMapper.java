package com.yeswater.foundation.common.web;

/**
 * 异常扩展映射器。
 */
public interface ExceptionMapper {

    /**
     * 是否支持处理该异常。
     */
    boolean supports(Throwable throwable);

    /**
     * 将异常映射为统一响应语义。
     */
    ResolvedException resolve(Throwable throwable);
}
