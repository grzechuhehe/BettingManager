package com.grzechuhehe.SportsBettingManagerApp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Mapowanie URL `/images/profiles/**` na fizyczny katalog `uploads/profiles/`
        // Pozwala to Reactowi wyświetlać zdjęcia przez tag <img src="http://localhost:8443/images/profiles/dowod.jpg" />
        registry.addResourceHandler("/images/profiles/**")
                .addResourceLocations("file:uploads/profiles/");
    }
}
