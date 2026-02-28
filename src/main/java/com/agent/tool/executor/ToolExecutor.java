package com.agent.tool.executor;

import com.agent.tool.model.ToolCall;
import com.agent.tool.model.ToolResult;
import com.agent.tool.registry.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Tool Executor
 * 
 * Executes tool methods and returns results
 */
@Slf4j
@Component
public class ToolExecutor {
    
    private final ToolRegistry toolRegistry;
    
    public ToolExecutor(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }
    
    /**
     * Execute a tool by name with input parameter
     */
    public ToolResult execute(String toolName, String input) {
        long startTime = System.currentTimeMillis();
        
        // Validate tool existence
        if (!toolRegistry.hasTool(toolName)) {
            String error = "Tool not found: " + toolName;
            log.warn(error);
            return ToolResult.failure(toolName, error);
        }
        
        try {
            log.debug("Executing tool: {} with input: {}", toolName, input);
            
            // Get the method and bean
            Method method = toolRegistry.getToolMethod(toolName);
            Object bean = toolRegistry.getToolBean(toolName);
            
            if (method == null || bean == null) {
                String error = "Tool method or bean not found: " + toolName;
                log.error(error);
                return ToolResult.failure(toolName, error);
            }
            
            // Execute the method
            Object result;
            try {
                // Handle methods with different parameters
                Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length == 0) {
                    // No parameters
                    result = method.invoke(bean);
                } else if (paramTypes.length == 1 && paramTypes[0] == String.class) {
                    // Single String parameter
                    result = method.invoke(bean, input);
                } else {
                    String error = "Unsupported method signature: " + method.getName();
                    log.error(error);
                    return ToolResult.failure(toolName, error);
                }
            } catch (Exception e) {
                String errorMsg = "Tool execution error: " + e.getMessage();
                log.error(errorMsg, e);
                return ToolResult.failure(toolName, errorMsg);
            }
            
            // Convert result to string
            String resultStr;
            if (result == null) {
                resultStr = "";
            } else if (result instanceof ToolResult) {
                // If method returns ToolResult directly
                return (ToolResult) result;
            } else {
                resultStr = result.toString();
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            return ToolResult.builder()
                .toolName(toolName)
                .result(resultStr)
                .success(true)
                .executionTimeMs(executionTime)
                .build();
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            String error = "Unexpected error executing tool: " + e.getMessage();
            log.error(error, e);
            
            return ToolResult.builder()
                .toolName(toolName)
                .error(error)
                .success(false)
                .executionTimeMs(executionTime)
                .build();
        }
    }
    
    /**
     * Execute a ToolCall
     */
    public ToolResult execute(ToolCall toolCall) {
        ToolResult result = execute(toolCall.getToolName(), toolCall.getInput());
        if (toolCall.getCallId() != null) {
            result.setCallId(toolCall.getCallId());
        }
        return result;
    }
}
