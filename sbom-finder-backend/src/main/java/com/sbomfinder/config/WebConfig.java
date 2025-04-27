package com.sbomfinder.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // (Generated with assistance from OpenAI ChatGPT, April 2025 Version)
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins(
                "https://sbom-finder-clean.vercel.app",
                "https://sbom-finder-clean-git-main-abeys-projects.vercel.app",
                "http://localhost:3000"
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("*")
            .allowCredentials(true);
    }
}
