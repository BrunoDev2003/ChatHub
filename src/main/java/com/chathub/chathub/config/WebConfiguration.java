package com.chathub.chathub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/**/*.css", "/**/*.html", "/**/*.js", "/**/*.js.map", "/**/*.css.map",
                        "/**/*.jsx", "/**/*.png", "/**/*.jpg", "/**/*.ttf", "/**/*.woff", "/**/*.woff2", "/**/*.ico")
                .setCachePeriod(0)
                .addResourceLocations("classpath:/client/build/");
        registry.addResourceHandler("/index.html").addResourceLocations("classpath:/client/build/index.html");
    }
}
