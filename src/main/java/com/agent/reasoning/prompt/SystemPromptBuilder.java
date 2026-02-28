package com.agent.reasoning.prompt;

import com.agent.tool.registry.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * System Prompt Builder
 * 
 * Builds the system prompt for the Agent
 * Instructs the LLM how to think and use tools
 */
@Slf4j
@Component
public class SystemPromptBuilder {
    
    private final ToolRegistry toolRegistry;
    
    public SystemPromptBuilder(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }
    
    /**
     * Build the system prompt with tool definitions
     */
    public String buildSystemPrompt() {
        return buildSystemPrompt(null);
    }
    
    /**
     * Build the system prompt with optional custom instructions
     */
    public String buildSystemPrompt(String customInstructions) {
        StringBuilder prompt = new StringBuilder();
        
        // Basic system role
        prompt.append("You are an AI Assistant with the ability to use tools to help answer questions and solve problems.\n\n");
        
        // Tool instructions
        prompt.append("=== AVAILABLE TOOLS ===\n");
        if (toolRegistry.size() == 0) {
            prompt.append("No tools available.\n");
        } else {
            prompt.append(toolRegistry.getToolsDescription());
        }
        prompt.append("\n\n");
        
        // Reasoning format instruction
        prompt.append("=== HOW TO RESPOND ===\n");
        prompt.append("Follow this exact format for your responses:\n\n");
        prompt.append("Thought: [Your reasoning about what you need to do to answer the user's question]\n");
        prompt.append("Action: [The name of the tool to use, or 'finish' if you have the answer]\n");
        prompt.append("Action Input: [The input to pass to the tool, or empty if using 'finish']\n\n");
        prompt.append("When you use a tool, you will receive:\n");
        prompt.append("Observation: [The result of the tool]\n\n");
        prompt.append("You can repeat Thought/Action/Observation cycles as many times as needed.\n");
        prompt.append("When you can answer the user's question directly, respond with:\n");
        prompt.append("Final Answer: [Your final answer to the user]\n\n");
        
        // Custom instructions
        if (customInstructions != null && !customInstructions.isEmpty()) {
            prompt.append("=== ADDITIONAL INSTRUCTIONS ===\n");
            prompt.append(customInstructions).append("\n\n");
        }
        
        // Important notes
        prompt.append("=== IMPORTANT NOTES ===\n");
        prompt.append("- Only use tools that are listed in AVAILABLE TOOLS section\n");
        prompt.append("- Provide clear reasoning in Thought before taking action\n");
        prompt.append("- Use tools to get information you don't know\n");
        prompt.append("- Always end with 'Final Answer:' when you're done\n");
        
        log.debug("System prompt built with {} tools", toolRegistry.size());
        
        return prompt.toString();
    }
}
