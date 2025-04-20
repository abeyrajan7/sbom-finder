package com.yourcompany.yourapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Allow all endpoints
                .allowedOrigins("http://localhost:3000") // Allow your Next.js frontend origin
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowCredentials(true); // Optional: If you use cookies/auth
    }
}
