package com.agent.demo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.stereotype.Component;

/**
 * 配置属性类演示 - 方式 1：@ConfigurationProperties
 * 
 * 这是 Spring Boot 管理配置的核心机制
 * 将 YAML 配置自动绑定到 Java Bean
 * 
 * 配置文件中的：
 * app:
 * name: demo
 * version: 1.0
 * features:
 * caching: true
 * 
 * 会自动注入到这个类的对应字段
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppConfigProperties {

    private String name;
    private String version;

    private Features features = new Features();

    @Data
    public static class Features {
        private boolean caching;
        private boolean monitoring;
        private int maxConnections = 10;
    }

    public void logConfig() {
        log.info("✅ 加载应用配置:");
        log.info("   应用名: {}", name);
        log.info("   版本: {}", version);
        log.info("   缓存功能: {}", features.caching);
        log.info("   监控功能: {}", features.monitoring);
        log.info("   最大连接数: {}", features.maxConnections);
    }
}
