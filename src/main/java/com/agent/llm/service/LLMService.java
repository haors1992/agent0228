package com.agent.llm.service;

import com.agent.llm.model.dto.ChatRequest;
import com.agent.llm.model.dto.ChatResponse;

/**
 * LLM Service Interface
 * Unified interface for all LLM providers
 */
public interface LLMService {
    
    /**
     * Send a chat request to LLM
     * 
     * @param request Chat request
     * @return Chat response
     * @throws Exception if the API call fails
     */
    ChatResponse chat(ChatRequest request) throws Exception;
    
    /**
     * Validate the request before sending
     * 
     * @param request Chat request
     * @throws IllegalArgumentException if validation fails
     */
    void validateRequest(ChatRequest request);
}
