package com.agent.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tool Result
 * Represents the result of executing a tool
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult {
    
    /**
     * Tool name that was executed
     */
    @JsonProperty("tool_name")
    private String toolName;
    
    /**
     * Execution result
     */
    @JsonProperty("result")
    private String result;
    
    /**
     * Whether execution was successful
     */
    @JsonProperty("success")
    @Builder.Default
    private Boolean success = true;
    
    /**
     * Error message if execution failed
     */
    @JsonProperty("error")
    private String error;
    
    /**
     * Execution time in milliseconds
     */
    @JsonProperty("execution_time_ms")
    private Long executionTimeMs;
    
    /**
     * Call ID for tracking
     */
    @JsonProperty("call_id")
    private String callId;
    
    /**
     * Create a successful result
     */
    public static ToolResult success(String toolName, String result) {
        return ToolResult.builder()
            .toolName(toolName)
            .result(result)
            .success(true)
            .build();
    }
    
    /**
     * Create a failed result
     */
    public static ToolResult failure(String toolName, String error) {
        return ToolResult.builder()
            .toolName(toolName)
            .error(error)
            .success(false)
            .build();
    }
}
