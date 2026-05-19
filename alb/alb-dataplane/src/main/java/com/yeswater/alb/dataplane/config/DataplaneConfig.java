package com.yeswater.alb.dataplane.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AlbDataplaneProperties.class)
public class DataplaneConfig {
}
