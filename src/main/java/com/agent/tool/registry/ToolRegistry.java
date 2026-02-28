package com.agent.tool.registry;

import com.agent.tool.annotation.Tool;
import com.agent.tool.model.ToolDefinition;
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
 */
@Slf4j
@Component
public class ToolRegistry {
    
    private final ApplicationContext applicationContext;
    private final Map<String, ToolDefinition> tools = new HashMap<>();
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
        initialized = true;
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
        
        // Check for duplicate
        if (tools.containsKey(toolName)) {
            log.warn("Tool '{}' already registered, skipping", toolName);
            return;
        }
        
        // Make method accessible
        method.setAccessible(true);
        
        // Create tool definition
        ToolDefinition definition = ToolDefinition.builder()
            .name(toolName)
            .description(toolAnnotation.description())
            .parameters(toolAnnotation.paramsDescription())
            .methodReference(bean.getClass().getName() + "." + method.getName())
            .build();
        
        // Register
        tools.put(toolName, definition);
        toolMethods.put(toolName, method);
        toolBeans.put(toolName, bean);
        
        log.debug("Registered tool: {} - {}", toolName, toolAnnotation.description());
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
     * Get tool definition list as JSON-like string for LLM
     * Used to construct the system prompt
     */
    public String getToolsDescription() {
        lazyInit();
        return tools.values().stream()
            .map(tool -> String.format(
                "- %s: %s",
                tool.getName(),
                tool.getDescription()
            ))
            .collect(Collectors.joining("\n"));
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
