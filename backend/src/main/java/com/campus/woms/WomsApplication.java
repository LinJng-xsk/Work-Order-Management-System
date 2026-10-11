package com.campus.woms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.campus.woms.mapper")
@SpringBootApplication
public class WomsApplication {

    public static void main(String[] args) {
        SpringApplication.run(WomsApplication.class, args);
    }
}
