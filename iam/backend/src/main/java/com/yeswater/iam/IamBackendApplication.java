package com.yeswater.iam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class IamBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(IamBackendApplication.class, args);
    }
}
