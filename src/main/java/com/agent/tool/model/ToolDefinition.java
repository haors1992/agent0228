package com.agent.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tool Definition
 * Describes a tool that the Agent can use
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDefinition {
    
    /**
     * Tool name
     */
    @JsonProperty("name")
    private String name;
    
    /**
     * Tool description
     */
    @JsonProperty("description")
    private String description;
    
    /**
     * Parameter specification in JSON schema format
     */
    @JsonProperty("parameters")
    private String parameters;
    
    /**
     * Method reference for invoking this tool
     */
    @JsonProperty("method_reference")
    private String methodReference;
    
    @Override
    public String toString() {
        return "Tool: " + name + " - " + description;
    }
}
