package com.aiagent.app;

import lombok.Getter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Spring Boot 应用类，负责初始化 Spring 上下文
 * @author jiangtao.shu
 */
@SpringBootApplication(scanBasePackages = "com.aiagent")
public class SpringBootApp {
    /**
     * -- GETTER --
     *  获取 Spring 上下文
     *
     */
    @Getter
    private static ConfigurableApplicationContext context;
    
    /**
     * 启动 Spring Boot 应用
     * @param args 命令行参数
     */
    public static void start(String[] args) {
        context = SpringApplication.run(SpringBootApp.class, args);
    }
    
    /**
     * 关闭 Spring Boot 应用
     */
    public static void stop() {
        if (context != null) {
            context.close();
        }
    }
}