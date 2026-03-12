package com.agent.demo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 配置注入演示 - 方式 2：@Value 注解
 * 
 * @Value 用于注入单个属性值
 *        常用于简单的配置项注入
 * 
 *        用法：
 *        @Value("${配置文件中的属性名}")
 *        private String 字段名;
 * 
 *        支持：
 *        @Value("${llm.deepseek.model}") - 直接注入
 *        @Value("${llm.deepseek.model:default-value}") - 默认值
 *        @Value("${不存在的属性:默认值}") - 当属性不存在时使用默认值
 */
@Slf4j
@Data
@Component
public class ConfigValueDemo {

    // 直接注入单个值
    @Value("${spring.application.name}")
    private String applicationName;

    // 注入嵌套属性
    @Value("${llm.deepseek.model}")
    private String llmModel;

    @Value("${llm.deepseek.api-key}")
    private String apiKey;

    @Value("${llm.deepseek.temperature}")
    private double temperature;

    // 注入整数
    @Value("${llm.deepseek.max-tokens}")
    private int maxTokens;

    // 注入布尔值
    @Value("${llm.deepseek.enabled:true}")
    private boolean enabled;

    // 有默认值的注入（当配置不存在时使用默认值）
    @Value("${custom.feature.enabled:false}")
    private boolean customFeatureEnabled;

    public void logValues() {
        log.info("✅ 使用 @Value 注入的配置:");
        log.info("   应用名: {}", applicationName);
        log.info("   LLM 模型: {}", llmModel);
        log.info("   温度: {}", temperature);
        log.info("   最大 Token: {}", maxTokens);
        log.info("   自定义功能是否启用: {}", customFeatureEnabled);
    }
}
