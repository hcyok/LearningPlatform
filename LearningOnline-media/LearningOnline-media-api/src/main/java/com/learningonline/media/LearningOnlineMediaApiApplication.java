package com.learningonline.media;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.learningonline")
public class LearningOnlineMediaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearningOnlineMediaApiApplication.class, args);
    }

}
