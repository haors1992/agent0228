package com.agent.tool.builtin;

import com.agent.tool.annotation.Tool;
import com.agent.tool.annotation.ToolParam;
import com.agent.tool.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * Built-in Tools
 * 
 * Collection of built-in tools that the Agent can use
 * Demonstrates rich schema definitions with parameters, examples, and retry
 * logic
 */
@Slf4j
@Component
public class BuiltInTools {

    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

    /**
     * Calculator tool
     * Evaluates mathematical expressions and returns the result
     */
    @Tool(name = "calculator", description = "Execute mathematical calculations with basic operators", params = @ToolParam(name = "expression", type = "string", description = "Mathematical expression using +, -, *, /, %, parentheses. Example: '100 + 200 * 2'", example = "10 * (5 + 3)", pattern = "[0-9+\\-*/%().\\s]+"), returnDescription = "Calculated numeric result as string", examples = {
            "Input: 100 + 200 * 2",
            "Output: 500"
    }, maxRetries = 2, timeoutMs = 5000, tags = { "math", "calculation" })
    public ToolResult calculator(String expression) {
        try {
            log.debug("Calculating: {}", expression);

            // Validate expression contains only allowed characters
            if (!expression.matches("[0-9+\\-*/%().\\s]+")) {
                return ToolResult.failure(
                        "calculator",
                        "Invalid expression: only numbers and operators (+, -, *, /, %, (), .) are allowed");
            }

            // Use JavaScript engine to evaluate
            ScriptEngine engine = scriptEngineManager.getEngineByName("JavaScript");
            if (engine == null) {
                // Fallback to simple calculation
                return simpleCal(expression);
            }

            Object result = engine.eval(expression);
            String resultStr = String.valueOf(result);

            log.debug("Calculation result: {}", resultStr);
            return ToolResult.success("calculator", resultStr);

        } catch (Exception e) {
            String error = "Calculation error: " + e.getMessage();
            log.error(error, e);
            return ToolResult.failure("calculator", error);
        }
    }

    /**
     * Simple calculation fallback for basic operations
     */
    private ToolResult simpleCal(String expression) {
        try {
            // Very basic evaluation for backup
            Double result = Double.parseDouble(expression);
            return ToolResult.success("calculator", String.valueOf(result));
        } catch (Exception e) {
            return ToolResult.failure("calculator", "Parse error: " + e.getMessage());
        }
    }

    /**
     * String operations tool
     * Performs various string operations like uppercase, lowercase, reverse
     */
    @Tool(name = "string_tools", description = "Perform string manipulation operations (uppercase, lowercase, reverse, length, trim)", params = @ToolParam(name = "input", type = "string", description = "Input format: 'operation:text'. Supported operations: upper, lower, reverse, length, trim", example = "upper:hello world", required = true), returnDescription = "Operation result as string", examples = {
            "Input: upper:hello world",
            "Output: HELLO WORLD",
            "Input: reverse:hello",
            "Output: olleh"
    }, maxRetries = 1, timeoutMs = 5000, tags = { "text", "string" })
    public ToolResult stringTools(String input) {
        try {
            if (!input.contains(":")) {
                return ToolResult.failure("string_tools", "Invalid format. Expected 'operation:text'");
            }

            String[] parts = input.split(":", 2);
            String operation = parts[0].trim().toLowerCase();
            String text = parts[1];

            String result;
            switch (operation) {
                case "upper":
                    result = text.toUpperCase();
                    break;
                case "lower":
                    result = text.toLowerCase();
                    break;
                case "reverse":
                    result = new StringBuilder(text).reverse().toString();
                    break;
                case "length":
                    result = String.valueOf(text.length());
                    break;
                case "trim":
                    result = text.trim();
                    break;
                default:
                    return ToolResult.failure("string_tools",
                            "Unknown operation: " + operation + ". Supported: upper, lower, reverse, length, trim");
            }

            return ToolResult.success("string_tools", result);

        } catch (Exception e) {
            String error = "String operation error: " + e.getMessage();
            log.error(error, e);
            return ToolResult.failure("string_tools", error);
        }
    }

    /**
     * Get current timestamp tool
     * Returns the current system time
     */
    @Tool(name = "get_timestamp", description = "Get current system timestamp in milliseconds", params = {}, returnDescription = "Current system timestamp as milliseconds since epoch", examples = {
            "Input: (no parameters)",
            "Output: 1741920000000"
    }, maxRetries = 1, timeoutMs = 2000, tags = { "time", "utility" })
    public ToolResult getTimestamp(String input) {
        long timestamp = System.currentTimeMillis();
        return ToolResult.success("get_timestamp", "Timestamp: " + timestamp);
    }
}
