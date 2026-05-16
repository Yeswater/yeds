package com.yeswater.bids.export.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableConfigurationProperties(ExportProperties.class)
public class ExportConfiguration {

    @Bean(name = "exportTaskExecutor")
    public Executor exportTaskExecutor(ExportProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getAsyncPoolSize());
        executor.setMaxPoolSize(properties.getAsyncPoolSize());
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("export-worker-");
        executor.initialize();
        return executor;
    }
}
