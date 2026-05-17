package com.yeswater.foundation.common.web;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(YedsWebErrorProperties.class)
public class CommonWebErrorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TraceIdFilter traceIdFilter(YedsWebErrorProperties properties) {
        return new TraceIdFilter(properties);
    }

    @Bean
    @ConditionalOnMissingBean(DefaultGlobalExceptionHandler.class)
    public DefaultGlobalExceptionHandler defaultGlobalExceptionHandler(
            ObjectProvider<ExceptionMapper> exceptionMapperProvider,
            YedsWebErrorProperties properties
    ) {
        List<ExceptionMapper> exceptionMappers = exceptionMapperProvider.orderedStream().toList();
        return new DefaultGlobalExceptionHandler(exceptionMappers, properties);
    }
}
