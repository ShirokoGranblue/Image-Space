package com.picmgmt;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.picmgmt.mapper")
public class PictureManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(PictureManagementApplication.class, args);
    }
}
