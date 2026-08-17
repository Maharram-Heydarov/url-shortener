package com.example.urlshortener.config;

import com.example.urlshortener.ratelimit.CreateUrlRateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final CreateUrlRateLimitInterceptor createUrlRateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry
                .addInterceptor(createUrlRateLimitInterceptor)
                .addPathPatterns("/api/v1/urls");
    }
}
