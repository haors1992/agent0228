package com.agent.tool.builtin;

import com.agent.tool.annotation.Tool;
import com.agent.tool.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * Built-in Tools
 * 
 * Collection of built-in tools that the Agent can use
 */
@Slf4j
@Component
public class BuiltInTools {
    
    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    
    /**
     * Calculator tool
     * Evaluates mathematical expressions and returns the result
     * 
     * @param expression Mathematical expression (e.g., "100 + 200 * 2")
     * @return Calculated result
     */
    @Tool(
        name = "calculator",
        description = "Execute mathematical calculations. Input should be a mathematical expression like '100 + 200' or '10 * (5 + 3)'"
    )
    public ToolResult calculator(String expression) {
        try {
            log.debug("Calculating: {}", expression);
            
            // Validate expression contains only allowed characters
            if (!expression.matches("[0-9+\\-*/%().\\s]+")) {
                return ToolResult.failure(
                    "calculator",
                    "Invalid expression: only numbers and operators (+, -, *, /, %, (), .) are allowed"
                );
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
     * 
     * @param operation Operation type (upper, lower, reverse, length, count)
     * @param text The text to operate on
     * @return Operation result
     */
    @Tool(
        name = "string_tools",
        description = "Perform string operations. Input format: 'operation:text' where operation is one of: upper, lower, reverse, length, trim. Example: 'upper:hello world' -> 'HELLO WORLD'"
    )
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
    @Tool(
        name = "get_timestamp",
        description = "Get current system timestamp. No input needed."
    )
    public ToolResult getTimestamp(String input) {
        long timestamp = System.currentTimeMillis();
        return ToolResult.success("get_timestamp", "Timestamp: " + timestamp);
    }
}
