package com.agent.llm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LLM Spring Configuration
 * 
 * Defines beans for LLM services
 */
@Configuration
public class LLMConfig {
    
    /**
     * ObjectMapper bean for JSON serialization
     * 
     * @return ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
