package com.gzx.gzxpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.gzx.gzxpicturebackend.mapper")
public class GzxPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GzxPictureBackendApplication.class, args);
    }

}
