package com.agent.llm.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Chat Response DTO
 * Unified response object from LLM
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatResponse {
    
    /**
     * Unique identifier for this response
     */
    @JsonProperty("id")
    private String id;
    
    /**
     * Model used
     */
    @JsonProperty("model")
    private String model;
    
    /**
     * Unix timestamp of creation
     */
    @JsonProperty("created")
    private Long created;
    
    /**
     * Choices - typically a single choice for completion
     */
    @JsonProperty("choices")
    private List<Choice> choices;
    
    /**
     * Token usage information
     */
    @JsonProperty("usage")
    private Usage usage;
    
    /**
     * Get the first choice's message content
     */
    public String getContent() {
        if (choices != null && !choices.isEmpty()) {
            return choices.get(0).getMessage().getContent();
        }
        return null;
    }
    
    /**
     * Choice DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        
        @JsonProperty("index")
        private Integer index;
        
        @JsonProperty("message")
        private Message message;
        
        @JsonProperty("finish_reason")
        private String finishReason;
    }
    
    /**
     * Usage DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        
        @JsonProperty("completion_tokens")
        private Integer completionTokens;
        
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }
}
