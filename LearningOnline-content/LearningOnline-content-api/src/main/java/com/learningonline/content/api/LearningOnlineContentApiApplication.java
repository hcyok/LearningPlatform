package com.learningonline.content.api;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import springfox.documentation.oas.annotations.EnableOpenApi;


@EnableOpenApi
@SpringBootApplication(scanBasePackages = "com.learningonline")
@EnableFeignClients(basePackages = {"com.learningonline.content.feignclient"})
public class LearningOnlineContentApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearningOnlineContentApiApplication.class, args);
    }

}
