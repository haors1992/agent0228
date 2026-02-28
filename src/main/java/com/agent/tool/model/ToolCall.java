package com.agent.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tool Call
 * Represents a request to call a particular tool
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCall {
    
    /**
     * Tool name to invoke
     */
    @JsonProperty("tool_name")
    private String toolName;
    
    /**
     * Input parameter(s) for the tool
     * Can be a simple string or JSON string
     */
    @JsonProperty("input")
    private String input;
    
    /**
     * Call ID for tracking
     */
    @JsonProperty("call_id")
    private String callId;
}
