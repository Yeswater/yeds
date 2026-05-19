package com.yeswater.alb.dataplane;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AlbDataplaneApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlbDataplaneApplication.class, args);
    }
}
