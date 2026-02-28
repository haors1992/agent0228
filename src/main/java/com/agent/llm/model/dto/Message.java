package com.agent.llm.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message DTO
 * Represents a single message in a conversation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    
    /**
     * Role: "user", "assistant", "system"
     */
    @JsonProperty("role")
    private String role;
    
    /**
     * Message content
     */
    @JsonProperty("content")
    private String content;
}
