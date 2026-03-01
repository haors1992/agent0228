package com.agent.reasoning.prompt;

import com.agent.config.DomainPromptConfig;
import com.agent.tool.registry.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * System Prompt Builder
 * 
 * Builds the system prompt for the Agent
 * Instructs the LLM how to think and use tools
 * 
 * 支持多领域专业化配置
 */
@Slf4j
@Component
public class SystemPromptBuilder {

    private final ToolRegistry toolRegistry;
    private final DomainPromptConfig domainPromptConfig;

    public SystemPromptBuilder(ToolRegistry toolRegistry, DomainPromptConfig domainPromptConfig) {
        this.toolRegistry = toolRegistry;
        this.domainPromptConfig = domainPromptConfig;
    }

    /**
     * Build the system prompt with tool definitions
     */
    public String buildSystemPrompt() {
        return buildSystemPrompt(null, null);
    }

    /**
     * Build the system prompt with optional custom instructions
     */
    public String buildSystemPrompt(String customInstructions) {
        return buildSystemPrompt(customInstructions, null);
    }

    /**
     * Build the system prompt with domain detection
     * 根据查询自动检测领域并应用相应的专业提示
     */
    public String buildSystemPromptWithDomainDetection(String query) {
        String domainPrompt = domainPromptConfig.detectAndGetDomainPrompt(query);
        log.info("🔍 Domain detection - Query: {}", query);
        log.info("🔍 Domain prompt returned (first 100 chars): {}",
                domainPrompt.substring(0, Math.min(100, domainPrompt.length())));
        return buildSystemPrompt(domainPrompt, query);
    }

    /**
     * Build the system prompt with custom instructions and domain information
     */
    private String buildSystemPrompt(String customInstructions, String query) {
        StringBuilder prompt = new StringBuilder();

        // 如果有自定义指令（来自领域配置），直接使用
        if (customInstructions != null && !customInstructions.isEmpty() && customInstructions.contains("===")) {
            // 这是一个完整的领域提示
            log.info("✅ Using domain-specific prompt (contains ===)");
            log.info("✅ Domain prompt first 80 chars: {}",
                    customInstructions.substring(0, Math.min(80, customInstructions.length())));
            prompt.append(customInstructions).append("\n\n");
        } else {
            // 基础系统角色
            log.warn("⚠️  Using general system prompt (domain detection may have failed)");
            log.warn("⚠️  customInstructions is: null={}, empty={}, noEquals={}",
                    customInstructions == null,
                    customInstructions != null && customInstructions.isEmpty(),
                    customInstructions != null && !customInstructions.contains("==="));
            prompt.append(
                    "You are an AI Assistant with the ability to use tools to help answer questions and solve problems.\n\n");

            // 如果有自定义指令，追加到这后面
            if (customInstructions != null && !customInstructions.isEmpty()) {
                prompt.append(customInstructions).append("\n\n");
            }
        }

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

        // Important notes
        prompt.append("=== IMPORTANT NOTES ===\n");
        prompt.append("- Only use tools that are listed in AVAILABLE TOOLS section\n");
        prompt.append("- Provide clear reasoning in Thought before taking action\n");
        prompt.append("- Use tools to get information you don't know\n");
        prompt.append("- Always end with 'Final Answer:' when you're done\n");

        log.debug("System prompt built with {} tools", toolRegistry.size());
        log.debug("Final prompt size: {} chars", prompt.length());

        return prompt.toString();
    }
}
