package com.yeswater.bids.export.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeswater.bids.export.api.ExportClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(ExportClientProperties.class)
public class ExportClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ExportClient exportClient(ExportClientProperties properties, ObjectMapper objectMapper) {
        return new HttpExportClient(properties, objectMapper);
    }
}
