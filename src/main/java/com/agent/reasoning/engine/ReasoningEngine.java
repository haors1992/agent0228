package com.agent.reasoning.engine;

import com.agent.llm.model.dto.ChatRequest;
import com.agent.llm.model.dto.ChatResponse;
import com.agent.llm.model.dto.Message;
import com.agent.llm.service.LLMService;
import com.agent.tool.executor.ToolExecutor;
import com.agent.tool.model.ToolCall;
import com.agent.tool.model.ToolResult;
import com.agent.reasoning.prompt.SystemPromptBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reasoning Engine
 * 
 * Core logic for Agent's reasoning loop (ReACT framework)
 */
@Slf4j
@Component
public class ReasoningEngine {
    
    private final LLMService llmService;
    private final ToolExecutor toolExecutor;
    private final SystemPromptBuilder promptBuilder;
    
    @Value("${agent.max-iterations:10}")
    private Integer maxIterations;
    
    @Value("${agent.timeout:300}")
    private Integer timeout;
    
    public ReasoningEngine(LLMService llmService, ToolExecutor toolExecutor, SystemPromptBuilder promptBuilder) {
        this.llmService = llmService;
        this.toolExecutor = toolExecutor;
        this.promptBuilder = promptBuilder;
    }
    
    /**
     * Execute the Agent's reasoning loop
     * 
     * Main method that runs the ReACT cycle:
     * Thought → Action → Observation → Reflection → repeat
     * 
     * @param userQuery The user's question
     * @return ExecutionContext with the final answer and all intermediate steps
     */
    public ExecutionContext execute(String userQuery) {
        log.info("Starting agent reasoning for query: {}", userQuery);
        
        ExecutionContext context = new ExecutionContext(userQuery);
        long startTime = System.currentTimeMillis();
        
        try {
            // Build system prompt with tools
            String systemPrompt = promptBuilder.buildSystemPrompt();
            
            // Initialize conversation messages
            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder()
                .role("system")
                .content(systemPrompt)
                .build());
            
            // Main reasoning loop
            for (int iteration = 0; iteration < maxIterations; iteration++) {
                context.setCurrentIteration(iteration + 1);
                
                // Check timeout
                if (System.currentTimeMillis() - startTime > timeout * 1000L) {
                    log.warn("Agent reasoning timeout after {} iterations", iteration);
                    context.finish("Reasoning timeout after " + iteration + " iterations");
                    return context;
                }
                
                log.debug("Iteration {}/{}", iteration + 1, maxIterations);
                
                // Add user query on first iteration
                if (iteration == 0) {
                    messages.add(Message.builder()
                        .role("user")
                        .content(userQuery)
                        .build());
                } else {
                    // Add previous observation to continue reasoning
                    if (!context.getToolResults().isEmpty()) {
                        ToolResult lastResult = context.getToolResults().get(context.getToolResults().size() - 1);
                        messages.add(Message.builder()
                            .role("user")
                            .content("Observation: " + lastResult.getResult())
                            .build());
                    }
                }
                
                // Call LLM to get thought and action
                ChatResponse response = callLLM(messages);
                if (response == null || response.getContent() == null) {
                    log.error("Failed to get response from LLM");
                    context.finish("Failed to get response from LLM");
                    return context;
                }
                
                String llmResponse = response.getContent();
                log.debug("LLM response: {}", llmResponse);
                messages.add(Message.builder()
                    .role("assistant")
                    .content(llmResponse)
                    .build());
                
                // Parse the response
                ThoughtAction thoughtAction = parseResponse(llmResponse);
                context.addThoughtAction(thoughtAction);
                
                // Check if agent decided to finish
                if ("finish".equalsIgnoreCase(thoughtAction.getAction()) || thoughtAction.getIsFinished()) {
                    String finalAnswer = thoughtAction.getFinalAnswer() != null 
                        ? thoughtAction.getFinalAnswer() 
                        : llmResponse;
                    context.finish(finalAnswer);
                    log.info("Agent reasoning completed after {} iterations", iteration + 1);
                    return context;
                }
                
                // Execute the tool
                ToolResult toolResult = toolExecutor.execute(thoughtAction.getAction(), thoughtAction.getActionInput());
                context.addToolResult(toolResult);
                
                if (!toolResult.getSuccess()) {
                    log.warn("Tool execution failed: {}", toolResult.getError());
                }
            }
            
            // Max iterations reached
            log.warn("Max iterations ({}) reached", maxIterations);
            if (!context.getToolResults().isEmpty()) {
                ToolResult lastResult = context.getToolResults().get(context.getToolResults().size() - 1);
                context.finish("Max iterations reached. Last observation: " + lastResult.getResult());
            } else {
                context.finish("Max iterations reached without finding answer.");
            }
            
            return context;
            
        } catch (Exception e) {
            log.error("Error during agent reasoning", e);
            context.finish("Error during reasoning: " + e.getMessage());
            return context;
        }
    }
    
    /**
     * Call LLM with the current conversation
     */
    private ChatResponse callLLM(List<Message> messages) {
        try {
            ChatRequest request = ChatRequest.builder()
                .model("deepseek-chat")  // Explicitly set the model
                .messages(messages)
                .temperature(0.7)
                .maxTokens(2048)
                .topP(0.9)
                .build();
            
            log.debug("CallLLM with request: model={}, messages={}", request.getModel(), request.getMessages().size());
            ChatResponse response = llmService.chat(request);
            log.debug("Received response: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Error calling LLM: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Parse LLM response to extract Thought, Action, and Action Input
     */
    private ThoughtAction parseResponse(String response) {
        ThoughtAction thoughtAction = ThoughtAction.builder()
            .timestamp(System.currentTimeMillis())
            .build();
        
        // Extract Thought
        Pattern thoughtPattern = Pattern.compile("Thought:\\s*(.+?)(?=Action:|Final Answer:|$)", Pattern.DOTALL);
        Matcher thoughtMatcher = thoughtPattern.matcher(response);
        if (thoughtMatcher.find()) {
            thoughtAction.setThought(thoughtMatcher.group(1).trim());
        }
        
        // Check for Final Answer
        Pattern finalAnswerPattern = Pattern.compile("Final Answer:\\s*(.+?)$", Pattern.DOTALL);
        Matcher finalAnswerMatcher = finalAnswerPattern.matcher(response);
        if (finalAnswerMatcher.find()) {
            thoughtAction.setFinalAnswer(finalAnswerMatcher.group(1).trim());
            thoughtAction.setAction("finish");
            thoughtAction.setIsFinished(true);
            return thoughtAction;
        }
        
        // Extract Action
        Pattern actionPattern = Pattern.compile("Action:\\s*(.+?)(?=Action Input:|Observation:|$)", Pattern.DOTALL);
        Matcher actionMatcher = actionPattern.matcher(response);
        if (actionMatcher.find()) {
            thoughtAction.setAction(actionMatcher.group(1).trim());
        }
        
        // Extract Action Input
        Pattern actionInputPattern = Pattern.compile("Action Input:\\s*(.+?)(?=Observation:|$)", Pattern.DOTALL);
        Matcher actionInputMatcher = actionInputPattern.matcher(response);
        if (actionInputMatcher.find()) {
            thoughtAction.setActionInput(actionInputMatcher.group(1).trim());
        } else {
            thoughtAction.setActionInput("");
        }
        
        // Default action if not parsed
        if (thoughtAction.getAction() == null || thoughtAction.getAction().isEmpty()) {
            thoughtAction.setAction("finish");
            thoughtAction.setFinalAnswer(response);
            thoughtAction.setIsFinished(true);
        }
        
        thoughtAction.setIsFinished("finish".equalsIgnoreCase(thoughtAction.getAction()));
        
        return thoughtAction;
    }
}
