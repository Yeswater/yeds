package com.yeswater.foundation.common.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 为引入 common-web-starter 的应用注入统一日志默认值（可被 application.yml 覆盖）。
 */
public class YedsLoggingEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "yedsLoggingDefaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> defaults = new LinkedHashMap<>();
        putIfMissing(environment, defaults, "logging.pattern.console",
                "%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [%15.15t] %-40.40logger{39} traceId=%X{traceId} : %m%n");
        putIfMissing(environment, defaults, "logging.level.root", "INFO");
        putIfMissing(environment, defaults, "logging.level.com.yeswater", "INFO");
        putIfMissing(environment, defaults, "logging.level.com.yeswater.alb", "INFO");
        putIfMissing(environment, defaults, "logging.level.com.apig", "INFO");
        putIfMissing(environment, defaults, "logging.level.org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver", "WARN");
        if (!defaults.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
        }
    }

    private static void putIfMissing(ConfigurableEnvironment environment, Map<String, Object> target, String key, String value) {
        if (environment.getProperty(key) == null) {
            target.put(key, value);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
