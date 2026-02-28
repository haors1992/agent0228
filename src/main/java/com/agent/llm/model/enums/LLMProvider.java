package com.agent.llm.model.enums;

import lombok.Getter;

/**
 * LLM Provider Enumeration
 */
@Getter
public enum LLMProvider {
    DEEPSEEK("deepseek", "https://api.deepseek.com"),
    GLM("glm", "https://open.bigmodel.cn/api/paas/v4"),
    OPENAI("openai", "https://api.openai.com/v1");
    
    private final String name;
    private final String baseUrl;
    
    LLMProvider(String name, String baseUrl) {
        this.name = name;
        this.baseUrl = baseUrl;
    }
    
    public static LLMProvider fromString(String name) {
        for (LLMProvider provider : LLMProvider.values()) {
            if (provider.name.equalsIgnoreCase(name)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown LLM provider: " + name);
    }
}
