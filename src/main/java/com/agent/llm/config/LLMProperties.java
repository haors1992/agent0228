package com.agent.llm.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LLM Configuration Properties
 * Binding to application.yml configuration
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm")
public class LLMProperties {
    
    private DeepSeekConfig deepseek;
    private GLMConfig glm;
    private OpenAIConfig openai;
    
    /**
     * DeepSeek Configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeepSeekConfig {
        private Boolean enabled;
        private String apiKey;
        private String baseUrl;
        private String model;
        private Integer maxTokens;
        private Double temperature;
        private Integer timeout;
    }
    
    /**
     * GLM Configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GLMConfig {
        private Boolean enabled;
        private String apiKey;
        private String baseUrl;
        private String model;
        private Integer maxTokens;
        private Double temperature;
        private Integer timeout;
    }
    
    /**
     * OpenAI Configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpenAIConfig {
        private Boolean enabled;
        private String apiKey;
        private String baseUrl;
        private String model;
        private Integer maxTokens;
        private Double temperature;
        private Integer timeout;
    }
}
