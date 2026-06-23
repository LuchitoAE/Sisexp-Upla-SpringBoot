package com.upla.sisexp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // SPA fallback: cualquier ruta que no sea /api/** ni un recurso real
        // sirve index.html para que React maneje el routing
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource resource = location.createRelative(resourcePath);
                        if (resource.exists() && resource.isReadable()
                                && !resourcePath.startsWith("api/")) {
                            return resource;
                        }
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }
                        Resource fallback = new ClassPathResource("/static/index.html");
                        if (fallback.exists() && fallback.isReadable()) {
                            return fallback;
                        }
                        return null;
                    }
                });
    }
}
