package com.merfonteen.likeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableCaching
@EnableFeignClients
@SpringBootApplication
public class LikeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LikeServiceApplication.class, args);
    }
}
