package com.agent.tool.annotation;

import java.lang.annotation.*;

/**
 * @Tool Annotation
 * 
 *       Marks a method as a tool that can be called by the Agent
 *       Provides comprehensive schema information for LLM understanding
 * 
 *       Example:
 * 
 *       <pre>
 *       &#64;Tool(name = "calculator", description = "Execute mathematical calculations", params = @ToolParam(name = "expression", type = "string", description = "Math expression like '100 + 200'"), examples = {
 *               "Input: 100 + 200 * 2",
 *               "Output: 500"
 *       })
 *       public ToolResult calculate(String expression) {
 *       }
 *       </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Tool {

    /**
     * Tool name - must be unique and in snake_case
     * Used to identify the tool when LLM requests to call it
     */
    String name();

    /**
     * Tool description - clear and concise (under 200 chars)
     * Should explain what the tool does and typical use cases
     */
    String description();

    /**
     * Parameter definitions using nested @ToolParam array
     * Replaces old paramsDescription for better type safety
     */
    ToolParam[] params() default {};

    /**
     * Return type description
     * Describes what the tool returns
     */
    String returnDescription() default "";

    /**
     * Usage examples - helps LLM understand correct usage
     * Each string should show input/output pair
     * 
     * Example: {"Input: hello", "Output: HELLO"}
     */
    String[] examples() default {};

    /**
     * Maximum retries if tool fails
     * Default is 2 (total attempts = initial + 2 retries)
     */
    int maxRetries() default 2;

    /**
     * Whether this tool requires special permissions
     * For security/audit purposes
     */
    String requiredPermission() default "";

    /**
     * Timeout in milliseconds (0 = no timeout)
     */
    long timeoutMs() default 30000;

    /**
     * Whether output should be verbose or concise
     * Helps save tokens in prompts
     */
    boolean verboseOutput() default false;

    /**
     * Tags for categorizing tools (e.g. "search", "file", "web")
     * Used for dynamic tool loading
     */
    String[] tags() default {};
}
