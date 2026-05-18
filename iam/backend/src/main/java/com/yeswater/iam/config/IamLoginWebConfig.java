package com.yeswater.iam.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 托管 yeds-login-web 构建产物（JS/CSS 等）；HTML 入口由 {@link com.yeswater.iam.interfaces.rest.IamLoginSpaController} 提供。
 */
@Configuration
public class IamLoginWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/iam/login/assets/**")
                .addResourceLocations("classpath:/static/yeds-login/assets/");
    }
}
