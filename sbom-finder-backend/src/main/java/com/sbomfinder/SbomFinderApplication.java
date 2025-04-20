package com.sbomfinder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.sbomfinder.repository")
public class SbomFinderApplication {
    public static void main(String[] args) {
        SpringApplication.run(SbomFinderApplication.class, args);
    }
}
