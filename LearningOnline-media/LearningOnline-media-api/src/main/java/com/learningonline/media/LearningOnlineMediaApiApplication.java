package com.learningonline.media;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import springfox.documentation.oas.annotations.EnableOpenApi;

@EnableOpenApi
@SpringBootApplication(scanBasePackages = "com.learningonline")
@EnableAspectJAutoProxy(exposeProxy = true)
public class LearningOnlineMediaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearningOnlineMediaApiApplication.class, args);
    }

}
