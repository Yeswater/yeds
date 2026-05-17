package com.yeswater.foundation.common.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "yeds.web.error")
public class YedsWebErrorProperties {

    /**
     * 未预期异常返回给前端的统一文案。
     */
    private String internalMessage = "服务内部异常";

    /**
     * 透传给客户端的 traceId 响应头。
     */
    private String traceHeaderName = "X-Trace-Id";

    /**
     * 业务异常是否打印堆栈。
     */
    private boolean businessStackTraceEnabled = false;

    public String getInternalMessage() {
        return internalMessage;
    }

    public void setInternalMessage(String internalMessage) {
        this.internalMessage = internalMessage;
    }

    public String getTraceHeaderName() {
        return traceHeaderName;
    }

    public void setTraceHeaderName(String traceHeaderName) {
        this.traceHeaderName = traceHeaderName;
    }

    public boolean isBusinessStackTraceEnabled() {
        return businessStackTraceEnabled;
    }

    public void setBusinessStackTraceEnabled(boolean businessStackTraceEnabled) {
        this.businessStackTraceEnabled = businessStackTraceEnabled;
    }
}
