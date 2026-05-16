package com.yeswater.bids.export;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BidsExportApplication {
    public static void main(String[] args) {
        SpringApplication.run(BidsExportApplication.class, args);
    }
}
