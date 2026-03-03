package com.agent.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Tool Definition
 * Describes a tool that the Agent can use
 * 
 * Now supports rich schema information via ToolSchema
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDefinition {

    /**
     * Tool name (unique identifier)
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
     * DEPRECATED: use schema.parameters instead
     */
    @JsonProperty("parameters")
    private String parameters;

    /**
     * Complete schema (recommended)
     */
    @JsonProperty("schema")
    private ToolSchema schema;

    /**
     * Method reference for invoking this tool
     */
    @JsonProperty("method_reference")
    private String methodReference;

    /**
     * Max retries for this tool
     */
    @JsonProperty("max_retries")
    private Integer maxRetries;

    /**
     * Timeout in milliseconds
     */
    @JsonProperty("timeout_ms")
    private Long timeoutMs;

    /**
     * Required permission (if any)
     */
    @JsonProperty("required_permission")
    private String requiredPermission;

    /**
     * Usage examples
     */
    @JsonProperty("examples")
    private List<String> examples;

    /**
     * Tags for categorization
     */
    @JsonProperty("tags")
    private List<String> tags;

    @Override
    public String toString() {
        return "Tool: " + name + " - " + description +
                (maxRetries != null ? " [retries: " + maxRetries + "]" : "");
    }
}
