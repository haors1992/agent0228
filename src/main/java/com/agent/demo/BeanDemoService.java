package com.agent.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Bean 演示服务
 * 
 * 这个类演示了：
 * 1. @Component 注解 → Spring 会创建它的 Bean
 * 2. @Value 注解 → Spring 会从 application.yml 注入值
 * 3. @PostConstruct 注解 → Bean 创建后自动调用初始化方法
 */
@Slf4j
@Component
public class BeanDemoService {

    /**
     * 从 application.yml 中注入应用名称
     */
    @Value("${spring.application.name:default-app}")
    private String applicationName;

    /**
     * 从 application.yml 中注入 LLM 模型名称
     */
    @Value("${llm.deepseek.model:unknown}")
    private String llmModel;

    /**
     * Bean 的创建时间
     */
    private LocalDateTime createdAt;

    /**
     * Bean 的初始化方法
     * 
     * 生命周期: 当 Spring 创建这个 Bean 并注入所有依赖后，
     * 就会自动调用这个方法
     */
    @PostConstruct
    public void init() {
        this.createdAt = LocalDateTime.now();
        log.info("🚀 BeanDemoService Bean 已创建！");
        log.info("   • 注入的应用名称: {}", applicationName);
        log.info("   • 注入的 LLM 模型: {}", llmModel);
        log.info("   • 创建时间: {}", createdAt);
    }

    /**
     * 获取 Bean 的信息
     * 这个方法会被 Controller 调用
     */
    public Map<String, Object> getBeanInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("beanClassName", this.getClass().getName());
        info.put("beanSimpleName", this.getClass().getSimpleName());
        info.put("beanHashCode", this.hashCode()); // Bean 的唯一标识

        info.put("injectedApplicationName", applicationName);
        info.put("injectedLLMModel", llmModel);

        info.put("createdAt", createdAt.toString());
        info.put("currentTime", LocalDateTime.now().toString());

        info.put("message", "✅ 这个 Map 里的所有数据都来自 BeanDemoService Bean！");

        return info;
    }

    /**
     * 演示 Bean 的单例特性
     * 每次获取同一个 Bean 时，HashCode 都相同（说明是同一个对象）
     */
    public String getHashCode() {
        return String.format("BeanDemoService 的 HashCode: %d （相同 HashCode 表示相同的 Bean 对象）",
                this.hashCode());
    }
}
