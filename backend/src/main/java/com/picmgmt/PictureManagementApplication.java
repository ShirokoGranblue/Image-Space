package com.picmgmt;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan({"com.picmgmt.mapper", "com.picmgmt.auth"})
public class PictureManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(PictureManagementApplication.class, args);
    }
}
