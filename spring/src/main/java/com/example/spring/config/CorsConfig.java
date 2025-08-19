package com.example.spring.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // 所有接口都允许跨域
                .allowedOriginPatterns("http://localhost:9527")  // 前端当前端口是5182，必须精确匹配
                .allowedMethods("POST", "OPTIONS", "GET")  // 必须包含OPTIONS（浏览器预检请求用）
                .allowedHeaders("*")  // 允许所有请求头
                .allowCredentials(true)  // 允许携带Cookie
                .maxAge(3600);  // 预检请求缓存1小时
    }
}