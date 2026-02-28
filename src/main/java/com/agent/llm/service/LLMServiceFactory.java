package com.agent.llm.service;

import com.agent.llm.model.enums.LLMProvider;
import com.agent.llm.service.impl.DeepSeekService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * LLM Service Factory
 * 
 * Factory pattern to manage different LLM service implementations
 */
@Slf4j
@Component
public class LLMServiceFactory {
    
    private final DeepSeekService deepSeekService;
    
    public LLMServiceFactory(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
    }
    
    /**
     * Get LLM service by provider
     * 
     * @param provider LLM provider
     * @return LLM service instance
     * @throws IllegalArgumentException if provider is not supported
     */
    public LLMService getService(LLMProvider provider) {
        switch (provider) {
            case DEEPSEEK:
                return deepSeekService;
            case GLM:
                log.warn("GLM provider not yet implemented, using DeepSeek as fallback");
                return deepSeekService;
            case OPENAI:
                log.warn("OpenAI provider not yet implemented, using DeepSeek as fallback");
                return deepSeekService;
            default:
                throw new IllegalArgumentException("Unsupported LLM provider: " + provider);
        }
    }
    
    /**
     * Get LLM service by provider name
     * 
     * @param providerName Provider name (e.g., "deepseek")
     * @return LLM service instance
     */
    public LLMService getService(String providerName) {
        LLMProvider provider = LLMProvider.fromString(providerName);
        return getService(provider);
    }
    
    /**
     * Get default service (DeepSeek)
     * 
     * @return DeepSeek service
     */
    public LLMService getDefaultService() {
        return deepSeekService;
    }
}
