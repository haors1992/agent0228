package com.agent.llm.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Chat Request DTO
 * Unified request object for all LLM providers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    
    /**
     * Model name
     */
    @JsonProperty("model")
    private String model;
    
    /**
     * Messages in the conversation
     */
    @JsonProperty("messages")
    private List<Message> messages;
    
    /**
     * Temperature: [0.0, 1.0]
     */
    @JsonProperty("temperature")
    @Builder.Default
    private Double temperature = 0.7;
    
    /**
     * Maximum tokens to generate
     */
    @JsonProperty("max_tokens")
    @Builder.Default
    private Integer maxTokens = 2048;
    
    /**
     * Top P: nucleus sampling
     */
    @JsonProperty("top_p")
    @Builder.Default
    private Double topP = 0.9;
    
    /**
     * Enable streaming
     */
    @JsonProperty("stream")
    @Builder.Default
    private Boolean stream = false;
}
