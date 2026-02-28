package com.agent.tool.annotation;

import java.lang.annotation.*;

/**
 * @Tool Annotation
 * 
 * Marks a method as a tool that can be called by the Agent
 * 
 * Example:
 * <pre>
 * @Tool(name = "calculator", description = "Execute mathematical calculations")
 * public ToolResult calculate(String expression) {
 *     // implementation
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Tool {
    
    /**
     * Tool name - must be unique
     * Used to identify the tool when LLM requests to call it
     */
    String name();
    
    /**
     * Tool description
     * Should clearly explain what the tool does and how to use it
     */
    String description();
    
    /**
     * Optional: parameter description in JSON format
     * Helps LLM understand what parameters to provide
     * 
     * Example: {
     *   "expression": {
     *     "type": "string",
     *     "description": "Mathematical expression to calculate"
     *   }
     * }
     */
    String paramsDescription() default "";
}
