package com.barangay.contentmanagement.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class CmsWebConfig implements WebMvcConfigurer {

    @Value("${upload.path:uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadDir.toUri().toString() + "/");
    }
}
