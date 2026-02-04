package com.aiagent.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Spring Boot 应用主类
 * @author jiangtao.shu
 */
@SpringBootApplication(scanBasePackages = "com.aiagent")
public class SpringBootApp {

    /**
     * 应用入口方法
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }

    /**
     * Web配置类
     */
    @Configuration
    public static class WebConfig implements WebMvcConfigurer {

        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**")
                    .allowedOrigins("*")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .maxAge(3600);
        }

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/**")
                    .addResourceLocations("classpath:/static/")
                    .setCachePeriod(3600)
                    .resourceChain(true)
                    .addResolver(new PathResourceResolver() {
                        @Override
                        protected Resource getResource(String resourcePath, Resource location) throws IOException {
                            Resource resource = location.createRelative(resourcePath);
                            if (resource.exists() && resource.isReadable()) {
                                return resource;
                            }
                            // 如果资源不存在，返回index.html（支持前端路由）
                            return new ClassPathResource("/static/index.html");
                        }
                    });
        }
    }
}
