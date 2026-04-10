package com.dujiao.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DujiaoNextApplication {

    public static void main(String[] args) {
        SpringApplication.run(DujiaoNextApplication.class, args);
    }
}
