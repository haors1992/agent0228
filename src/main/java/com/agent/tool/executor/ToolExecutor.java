package com.agent.tool.executor;

import com.agent.tool.model.ToolCall;
import com.agent.tool.model.ToolDefinition;
import com.agent.tool.model.ToolResult;
import com.agent.tool.registry.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.*;

/**
 * Tool Executor
 * 
 * Executes tool methods with enhanced features:
 * - Automatic retry logic with exponential backoff
 * - Timeout control and handling
 * - Detailed error reporting
 * - Execution metrics
 */
@Slf4j
@Component
public class ToolExecutor {

    private final ToolRegistry toolRegistry;
    private final ExecutorService executorService;
    public static final int DEFAULT_MAX_RETRIES = 2;
    public static final long DEFAULT_TIMEOUT_MS = 30000;

    public ToolExecutor(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
        this.executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                r -> {
                    Thread t = new Thread(r, "ToolExecutor-" + System.nanoTime());
                    t.setDaemon(false);
                    return t;
                });
    }

    /**
     * Execute a tool with automatic retry logic
     * 
     * @param toolName the tool to execute
     * @param input    the input parameter
     * @return tool execution result
     */
    public ToolResult execute(String toolName, String input) {
        // Validate tool existence
        if (!toolRegistry.hasTool(toolName)) {
            String error = "Tool not found: " + toolName;
            log.warn(error);
            return ToolResult.failure(toolName, error);
        }

        ToolDefinition toolDef = toolRegistry.getTool(toolName);
        int maxRetries = toolDef != null && toolDef.getMaxRetries() != null ? toolDef.getMaxRetries()
                : DEFAULT_MAX_RETRIES;
        long timeoutMs = toolDef != null && toolDef.getTimeoutMs() != null ? toolDef.getTimeoutMs()
                : DEFAULT_TIMEOUT_MS;

        return executeWithRetry(toolName, input, maxRetries, timeoutMs);
    }

    /**
     * Execute a tool with retry logic
     */
    private ToolResult executeWithRetry(String toolName, String input, int maxRetries, long timeoutMs) {
        int attemptCount = 0;
        ToolResult lastError = null;

        while (attemptCount <= maxRetries) {
            attemptCount++;
            log.debug("Executing tool '{}' (attempt {}/{})", toolName, attemptCount, maxRetries + 1);

            try {
                // Execute with timeout
                ToolResult result = executeWithTimeout(toolName, input, timeoutMs);

                // If successful, return immediately
                if (result.getSuccess()) {
                    log.debug("Tool '{}' succeeded on attempt {}", toolName, attemptCount);
                    return result;
                }

                // If failed, store for retry
                lastError = result;

                // Don't retry on last attempt
                if (attemptCount > maxRetries) {
                    log.warn("Tool '{}' failed after {} attempts. Final error: {}",
                            toolName, attemptCount, result.getError());
                    return result;
                }

                // Wait before retry with exponential backoff: 100ms, 200ms, 400ms...
                long waitMs = 100 * (long) Math.pow(2, attemptCount - 1);
                log.debug("Tool '{}' failed, retrying after {}ms", toolName, waitMs);
                Thread.sleep(waitMs);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Tool execution interrupted: {}", toolName);
                return ToolResult.failure(toolName, "Tool execution was interrupted: " + e.getMessage());
            } catch (Exception e) {
                lastError = ToolResult.failure(toolName, "Execution error: " + e.getMessage());
                log.error("Unexpected error executing tool: {}", toolName, e);

                if (attemptCount > maxRetries) {
                    return lastError;
                }
            }
        }

        // Return the last error if all retries exhausted
        return lastError != null ? lastError
                : ToolResult.failure(toolName, "Tool execution failed after " + maxRetries + " retries");
    }

    /**
     * Execute a tool with timeout control
     */
    private ToolResult executeWithTimeout(String toolName, String input, long timeoutMs)
            throws TimeoutException, InterruptedException {

        long startTime = System.currentTimeMillis();

        try {
            // Execute in thread pool with timeout
            Future<ToolResult> future = executorService.submit(() -> executeTool(toolName, input));

            long remainingTime = timeoutMs > 0 ? timeoutMs : Long.MAX_VALUE;
            ToolResult result = future.get(remainingTime, TimeUnit.MILLISECONDS);

            long executionTime = System.currentTimeMillis() - startTime;
            if (result != null) {
                result.setExecutionTimeMs(executionTime);
            }

            return result;

        } catch (TimeoutException e) {
            log.warn("Tool '{}' execution timed out after {}ms", toolName, timeoutMs);
            return ToolResult.failure(toolName,
                    String.format("Tool execution timed out after %dms", timeoutMs));
        } catch (ExecutionException e) {
            log.error("Tool '{}' execution failed", toolName, e.getCause());
            Throwable cause = e.getCause();
            return ToolResult.failure(toolName,
                    "Tool execution error: " + (cause != null ? cause.getMessage() : e.getMessage()));
        }
    }

    /**
     * Core tool execution logic
     */
    private ToolResult executeTool(String toolName, String input) {
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

            return ToolResult.builder()
                    .toolName(toolName)
                    .result(resultStr)
                    .success(true)
                    .build();

        } catch (Exception e) {
            String error = "Unexpected error executing tool: " + e.getMessage();
            log.error(error, e);

            return ToolResult.builder()
                    .toolName(toolName)
                    .error(error)
                    .success(false)
                    .build();
        }
    }

    /**
     * Execute a ToolCall with retry logic
     */
    public ToolResult execute(ToolCall toolCall) {
        ToolResult result = execute(toolCall.getToolName(), toolCall.getInput());
        if (toolCall.getCallId() != null) {
            result.setCallId(toolCall.getCallId());
        }
        return result;
    }

    /**
     * Shutdown the executor service
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
