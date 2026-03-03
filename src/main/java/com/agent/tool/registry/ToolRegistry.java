package com.agent.tool.registry;

import com.agent.tool.annotation.Tool;
import com.agent.tool.model.ToolDefinition;
import com.agent.tool.model.ToolSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tool Registry
 * 
 * Scans and registers all tools marked with @Tool annotation
 * Provides tool definitions for the LLM to use
 * 
 * Features:
 * - Automatic schema extraction from @Tool annotation
 * - Support for complex parameter definitions
 * - Retry and timeout configuration
 * - Permission-based access control preparation
 */
@Slf4j
@Component
public class ToolRegistry {

    private final ApplicationContext applicationContext;
    private final Map<String, ToolDefinition> tools = new HashMap<>();
    private final Map<String, ToolSchema> schemas = new HashMap<>();
    private final Map<String, Method> toolMethods = new HashMap<>();
    private final Map<String, Object> toolBeans = new HashMap<>();
    private volatile boolean initialized = false;

    public ToolRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Lazy initialization - scans for tools on first use
     */
    private synchronized void lazyInit() {
        if (initialized) {
            return;
        }

        log.info("Initializing Tool Registry...");

        // Scan all beans in the application context
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            try {
                Object bean = applicationContext.getBean(beanName);
                scanToolsInBean(bean);
            } catch (Exception e) {
                // Skip beans that can't be instantiated
                log.debug("Skipping bean: {}", beanName, e);
            }
        }

        log.info("Tool Registry initialized with {} tools", tools.size());
        logToolsInfo();
        initialized = true;
    }

    private void logToolsInfo() {
        if (log.isDebugEnabled()) {
            for (Map.Entry<String, ToolDefinition> entry : tools.entrySet()) {
                String toolName = entry.getKey();
                ToolSchema schema = schemas.get(toolName);
                if (schema != null && schema.getExamples() != null && !schema.getExamples().isEmpty()) {
                    log.debug("Tool: {} has {} examples", toolName, schema.getExamples().size());
                }
            }
        }
    }

    private void scanToolsInBean(Object bean) {
        Class<?> beanClass = bean.getClass();

        // Scan all methods in the bean
        Method[] methods = beanClass.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Tool.class)) {
                registerTool(bean, method);
            }
        }
    }

    /**
     * Register a single tool
     */
    private void registerTool(Object bean, Method method) {
        Tool toolAnnotation = method.getAnnotation(Tool.class);
        String toolName = toolAnnotation.name();

        // Validate tool name format (should be snake_case)
        if (!toolName.matches("^[a-z_][a-z0-9_]*$")) {
            log.warn("Tool name '{}' does not follow snake_case convention", toolName);
        }

        // Check for duplicate
        if (tools.containsKey(toolName)) {
            log.warn("Tool '{}' already registered, skipping", toolName);
            return;
        }

        // Make method accessible
        method.setAccessible(true);

        // Create schema from annotation
        ToolSchema schema = ToolSchema.fromAnnotation(toolAnnotation);
        schemas.put(toolName, schema);

        // Create tool definition with rich schema
        ToolDefinition definition = ToolDefinition.builder()
                .name(toolName)
                .description(toolAnnotation.description())
                .schema(schema)
                .methodReference(bean.getClass().getName() + "." + method.getName())
                .maxRetries(toolAnnotation.maxRetries())
                .timeoutMs(toolAnnotation.timeoutMs())
                .requiredPermission(
                        toolAnnotation.requiredPermission().isEmpty() ? null : toolAnnotation.requiredPermission())
                .examples(Arrays.asList(toolAnnotation.examples()))
                .tags(Arrays.asList(toolAnnotation.tags()))
                .build();

        // Register
        tools.put(toolName, definition);
        toolMethods.put(toolName, method);
        toolBeans.put(toolName, bean);

        log.debug("Registered tool: {} - {} (retries: {}, timeout: {}ms)",
                toolName, toolAnnotation.description(), toolAnnotation.maxRetries(), toolAnnotation.timeoutMs());
    }

    /**
     * Get all registered tools
     */
    public Collection<ToolDefinition> getAllTools() {
        lazyInit();
        return tools.values();
    }

    /**
     * Get tool by name
     */
    public ToolDefinition getTool(String toolName) {
        lazyInit();
        return tools.get(toolName);
    }

    /**
     * Get tool schema by name
     */
    public ToolSchema getToolSchema(String toolName) {
        lazyInit();
        return schemas.get(toolName);
    }

    /**
     * Check if tool exists
     */
    public boolean hasTool(String toolName) {
        lazyInit();
        return tools.containsKey(toolName);
    }

    /**
     * Get all tool names
     */
    public Set<String> getToolNames() {
        lazyInit();
        return tools.keySet();
    }

    /**
     * Get tools by tag (for dynamic tool loading)
     */
    public Collection<ToolDefinition> getToolsByTag(String tag) {
        lazyInit();
        return tools.values().stream()
                .filter(tool -> tool.getTags() != null && tool.getTags().contains(tag))
                .collect(Collectors.toList());
    }

    /**
     * Get tool definition list as JSON-like string for LLM
     * Used to construct the system prompt
     * Concise format to save tokens
     */
    public String getToolsDescription() {
        lazyInit();
        return tools.values().stream()
                .map(tool -> String.format(
                        "- %s: %s",
                        tool.getName(),
                        tool.getDescription()))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Get detailed tools description with parameters and examples
     * More verbose, suitable for system prompts with ample token budget
     */
    public String getDetailedToolsDescription() {
        lazyInit();
        StringBuilder sb = new StringBuilder();

        for (ToolDefinition tool : tools.values()) {
            ToolSchema schema = schemas.get(tool.getName());
            if (schema != null) {
                sb.append(schema.toPromptFormat()).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Get method for a tool
     */
    public Method getToolMethod(String toolName) {
        lazyInit();
        return toolMethods.get(toolName);
    }

    /**
     * Get bean for a tool
     */
    public Object getToolBean(String toolName) {
        lazyInit();
        return toolBeans.get(toolName);
    }

    /**
     * Size of the registry
     */
    public int size() {
        return tools.size();
    }
}
