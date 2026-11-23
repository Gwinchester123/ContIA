package com.contia.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    private final List<String> allowedOrigins;

    public CorsConfig(@Value("${CONTIA_CORS_ALLOWED_ORIGINS:http://localhost:5173,http://localhost:5174}") String origins) {
        this.allowedOrigins = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .peek(origin -> {
                    if ("*".equals(origin)) {
                        throw new IllegalArgumentException("CORS origins must be explicit; wildcard is not allowed.");
                    }
                })
                .distinct()
                .collect(Collectors.toList());
        if (this.allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException("At least one explicit CORS origin must be configured.");
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
