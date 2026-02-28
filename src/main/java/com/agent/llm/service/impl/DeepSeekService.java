package com.agent.llm.service.impl;

import com.agent.llm.config.LLMProperties;
import com.agent.llm.model.dto.ChatRequest;
import com.agent.llm.model.dto.ChatResponse;
import com.agent.llm.model.dto.Message;
import com.agent.llm.service.LLMService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * DeepSeek LLM Service Implementation
 * 
 * Implements the LLMService interface for DeepSeek API
 */
@Slf4j
@Service
public class DeepSeekService implements LLMService {
    
    private final LLMProperties llmProperties;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    
    public DeepSeekService(LLMProperties llmProperties, ObjectMapper objectMapper) {
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
        
        // Initialize HTTP client with timeout
        long timeout = llmProperties.getDeepseek().getTimeout() != null 
            ? llmProperties.getDeepseek().getTimeout() 
            : 30;
        
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)
            .build();
    }
    
    @Override
    public ChatResponse chat(ChatRequest request) throws Exception {
        log.debug("Sending chat request to DeepSeek: {}", request);
        
        validateRequest(request);
        
        // Build the request
        LLMProperties.DeepSeekConfig config = llmProperties.getDeepseek();
        String url = config.getBaseUrl() + "/chat/completions";
        
        // Prepare request body
        ChatRequest deepseekRequest = ChatRequest.builder()
            .model(request.getModel() != null ? request.getModel() : config.getModel())
            .messages(request.getMessages())
            .temperature(request.getTemperature() != null ? request.getTemperature() : config.getTemperature())
            .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : config.getMaxTokens())
            .topP(request.getTopP())
            .stream(false)
            .build();
        
        String requestBody = objectMapper.writeValueAsString(deepseekRequest);
        log.debug("DeepSeek request body: {}", requestBody);
        
        // Create HTTP request
        RequestBody body = RequestBody.create(
            requestBody,
            MediaType.get("application/json; charset=utf-8")
        );
        
        Request httpRequest = new Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Authorization", "Bearer " + config.getApiKey())
            .addHeader("Content-Type", "application/json")
            .build();
        
        // Send request and get response
        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error details";
                log.error("DeepSeek API error: {} - {}", response.code(), errorBody);
                throw new IOException("DeepSeek API error: " + response.code() + " - " + errorBody);
            }
            
            String responseBody = response.body().string();
            log.debug("DeepSeek response: {}", responseBody);
            
            ChatResponse chatResponse = objectMapper.readValue(responseBody, ChatResponse.class);
            log.debug("Parsed response: {}", chatResponse);
            
            return chatResponse;
        }
    }
    
    @Override
    public void validateRequest(ChatRequest request) {
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new IllegalArgumentException("Messages cannot be empty");
        }
        
        for (Message message : request.getMessages()) {
            if (message.getRole() == null || message.getRole().isEmpty()) {
                throw new IllegalArgumentException("Message role cannot be empty");
            }
            if (message.getContent() == null || message.getContent().isEmpty()) {
                throw new IllegalArgumentException("Message content cannot be empty");
            }
        }
        
        if (llmProperties.getDeepseek() == null || !llmProperties.getDeepseek().getEnabled()) {
            throw new IllegalArgumentException("DeepSeek is not enabled");
        }
        
        if (llmProperties.getDeepseek().getApiKey() == null || llmProperties.getDeepseek().getApiKey().isEmpty()) {
            throw new IllegalArgumentException("DeepSeek API key is not configured");
        }
    }
}
