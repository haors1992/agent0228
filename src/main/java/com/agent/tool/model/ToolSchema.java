package com.agent.tool.model;

import com.agent.tool.annotation.Tool;
import com.agent.tool.annotation.ToolParam;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * Tool Schema
 * 
 * Rich schema information for tools, suitable for LLM consumption
 * Supports JSON Schema format for complex parameter definitions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolSchema {

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
     * Parameter schema in JSON Schema format
     */
    @JsonProperty("parameters")
    private Map<String, Object> parameters;

    /**
     * Return value description
     */
    @JsonProperty("return_description")
    private String returnDescription;

    /**
     * Usage examples (input -> output pairs)
     */
    @JsonProperty("examples")
    private List<String> examples;

    /**
     * Max retries
     */
    @JsonProperty("max_retries")
    private Integer maxRetries;

    /**
     * Required permission for this tool
     */
    @JsonProperty("required_permission")
    private String requiredPermission;

    /**
     * Timeout in milliseconds
     */
    @JsonProperty("timeout_ms")
    private Long timeoutMs;

    /**
     * Tags for categorization
     */
    @JsonProperty("tags")
    private List<String> tags;

    /**
     * Create schema from @Tool annotation
     */
    public static ToolSchema fromAnnotation(Tool toolAnnotation) {
        Map<String, Object> parameters = new HashMap<>();

        // Build parameters schema
        if (toolAnnotation.params().length > 0) {
            Map<String, Object> properties = new LinkedHashMap<>();
            List<String> required = new ArrayList<>();

            for (ToolParam param : toolAnnotation.params()) {
                Map<String, Object> paramDef = new LinkedHashMap<>();
                paramDef.put("type", param.type());
                paramDef.put("description", param.description());

                if (!param.defaultValue().isEmpty()) {
                    paramDef.put("default", param.defaultValue());
                }

                if (param.enum_().length > 0) {
                    paramDef.put("enum", Arrays.asList(param.enum_()));
                }

                if (!param.pattern().isEmpty()) {
                    paramDef.put("pattern", param.pattern());
                }

                if (!param.minValue().isEmpty()) {
                    paramDef.put("minimum", param.minValue());
                }

                if (!param.maxValue().isEmpty()) {
                    paramDef.put("maximum", param.maxValue());
                }

                if (!param.itemType().isEmpty()) {
                    paramDef.put("items", Collections.singletonMap("type", param.itemType()));
                }

                if (!param.example().isEmpty()) {
                    paramDef.put("example", param.example());
                }

                properties.put(param.name(), paramDef);

                if (param.required()) {
                    required.add(param.name());
                }
            }

            parameters.put("type", "object");
            parameters.put("properties", properties);

            if (!required.isEmpty()) {
                parameters.put("required", required);
            }
        }

        return ToolSchema.builder()
                .name(toolAnnotation.name())
                .description(toolAnnotation.description())
                .parameters(parameters.isEmpty() ? null : parameters)
                .returnDescription(toolAnnotation.returnDescription())
                .examples(Arrays.asList(toolAnnotation.examples()))
                .maxRetries(toolAnnotation.maxRetries())
                .requiredPermission(
                        toolAnnotation.requiredPermission().isEmpty() ? null : toolAnnotation.requiredPermission())
                .timeoutMs(toolAnnotation.timeoutMs())
                .tags(Arrays.asList(toolAnnotation.tags()))
                .build();
    }

    /**
     * Convert to simplified format for LLM prompt
     * More token-efficient than full JSON schema
     */
    public String toPromptFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(": ").append(description).append("\n");

        if (!parameters.isEmpty()) {
            sb.append("  Parameters:\n");
            Map<String, Object> props = (Map<String, Object>) parameters.get("properties");
            if (props != null) {
                for (Map.Entry<String, Object> entry : props.entrySet()) {
                    Map<String, Object> paramDef = (Map<String, Object>) entry.getValue();
                    boolean required = ((List<String>) parameters.get("required")).contains(entry.getKey());

                    sb.append("    - ").append(entry.getKey())
                            .append(" (").append(paramDef.get("type")).append(")")
                            .append(required ? " *required" : " *optional")
                            .append(": ").append(paramDef.get("description")).append("\n");
                }
            }
        }

        if (!examples.isEmpty()) {
            sb.append("  Examples:\n");
            for (String example : examples) {
                sb.append("    ").append(example).append("\n");
            }
        }

        return sb.toString();
    }
}
