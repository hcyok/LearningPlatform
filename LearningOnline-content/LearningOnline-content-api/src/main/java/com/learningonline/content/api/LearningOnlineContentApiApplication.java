package com.learningonline.content.api;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.oas.annotations.EnableOpenApi;


@EnableOpenApi
@SpringBootApplication(scanBasePackages = "com.learningonline")
public class LearningOnlineContentApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearningOnlineContentApiApplication.class, args);
    }

}
