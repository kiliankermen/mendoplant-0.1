package com.mendoplant.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MendoPlantApplication {

    public static void main(String[] args) {
        SpringApplication.run(MendoPlantApplication.class, args);
    }

}
