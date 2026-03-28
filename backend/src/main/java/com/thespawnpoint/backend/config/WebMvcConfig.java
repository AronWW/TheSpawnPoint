package com.thespawnpoint.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/avatars}")
    private String uploadDir;

    @Value("${app.upload.group-avatar.dir:uploads/group-avatars}")
    private String groupAvatarDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path groupAvatarPath = Paths.get(groupAvatarDir).toAbsolutePath().normalize();

        registry.addResourceHandler("/avatars/**")
                .addResourceLocations("file:" + uploadPath + "/");

        registry.addResourceHandler("/avatars/default/**")
                .addResourceLocations("classpath:/static/avatars/default/");

        registry.addResourceHandler("/group-avatars/**")
                .addResourceLocations("file:" + groupAvatarPath + "/");
    }
}