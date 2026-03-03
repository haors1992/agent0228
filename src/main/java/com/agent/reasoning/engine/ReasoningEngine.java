package com.agent.reasoning.engine;

import com.agent.knowledge.service.KnowledgeBaseManager;
import com.agent.llm.model.dto.ChatRequest;
import com.agent.llm.model.dto.ChatResponse;
import com.agent.llm.model.dto.Message;
import com.agent.llm.service.LLMService;
import com.agent.tool.executor.ToolExecutor;
import com.agent.tool.model.ToolCall;
import com.agent.tool.model.ToolResult;
import com.agent.reasoning.prompt.SystemPromptBuilder;
import com.agent.reasoning.context.ContextManager;
import com.agent.reasoning.context.HistoryCompressor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Reasoning Engine
 * 
 * Core logic for Agent's reasoning loop (ReACT framework)
 * 
 * Integrated with ContextManager for intelligent context management:
 * - Automatic history compression
 * - Token-aware conversation management
 * - Smart summarization of old messages
 */
@Slf4j
@Component
public class ReasoningEngine {

    private final LLMService llmService;
    private final ToolExecutor toolExecutor;
    private final SystemPromptBuilder promptBuilder;
    private final KnowledgeBaseManager knowledgeBaseManager;
    private final ContextManager contextManager;

    @Value("${agent.max-iterations:10}")
    private Integer maxIterations;

    @Value("${agent.timeout:300}")
    private Integer timeout;

    @Value("${agent.knowledge.enabled:true}")
    private Boolean knowledgeEnabled;

    @Value("${agent.knowledge.top-k:3}")
    private Integer knowledgeTopK;

    public ReasoningEngine(LLMService llmService, ToolExecutor toolExecutor,
            SystemPromptBuilder promptBuilder,
            KnowledgeBaseManager knowledgeBaseManager,
            ContextManager contextManager) {
        this.llmService = llmService;
        this.toolExecutor = toolExecutor;
        this.promptBuilder = promptBuilder;
        this.knowledgeBaseManager = knowledgeBaseManager;
        this.contextManager = contextManager;
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
        return execute(userQuery, new ArrayList<>());
    }

    /**
     * Execute the Agent's reasoning loop with conversation context
     * 
     * @param userQuery           The user's question
     * @param conversationHistory Previous conversation messages for context
     * @return ExecutionContext with the final answer and all intermediate steps
     */
    public ExecutionContext execute(String userQuery, List<String> conversationHistory) {
        log.info("Starting agent reasoning for query: {}", userQuery);
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            log.info("📚 Including {} messages in conversation context", conversationHistory.size());
        }

        ExecutionContext context = new ExecutionContext(userQuery);
        long startTime = System.currentTimeMillis();

        try {
            // Build system prompt with domain detection
            String systemPrompt = promptBuilder.buildSystemPromptWithDomainDetection(userQuery);

            // Initialize context manager
            contextManager.initializeWithSystemPrompt(systemPrompt);

            // Add conversation history for context using ContextManager
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                for (String history : conversationHistory) {
                    // Parse role and content from formatted history (ROLE: content)
                    if (history.contains(":")) {
                        String[] parts = history.split(":", 2);
                        String role = parts[0].trim().toLowerCase();
                        String content = parts[1].trim();

                        // Skip the current message if it's the latest user query
                        if ("user".equals(role) && content.equals(userQuery)) {
                            continue;
                        }

                        contextManager.addMessage(role, content);
                    }
                }
                log.info("✅ Context loaded: {} previous messages added", contextManager.getMessageCount());
            }

            // Add knowledge base context if enabled
            if (knowledgeEnabled != null && knowledgeEnabled &&
                    knowledgeBaseManager != null && knowledgeBaseManager.getStats().getTotalDocuments() > 0) {

                List<KnowledgeBaseManager.SearchResult> knowledgeResults = knowledgeBaseManager
                        .semanticSearch(userQuery, knowledgeTopK);

                if (!knowledgeResults.isEmpty()) {
                    StringBuilder knowledgeContext = new StringBuilder();
                    knowledgeContext.append("📚 相关知识库内容：\n");

                    for (KnowledgeBaseManager.SearchResult result : knowledgeResults) {
                        knowledgeContext.append(String.format("[%s] (相似度: %.2f)\n%s\n\n",
                                result.getTitle(),
                                result.getSimilarity() * 100,
                                result.getSummary()));
                    }

                    contextManager.addMessage("system", knowledgeContext.toString());
                    log.info("🧠 Knowledge base context added: {} documents", knowledgeResults.size());
                }
            }

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
                    contextManager.addUserMessage(userQuery);
                } else {
                    // Add previous observation to continue reasoning
                    if (!context.getToolResults().isEmpty()) {
                        ToolResult lastResult = context.getToolResults().get(context.getToolResults().size() - 1);
                        contextManager.addObservation(lastResult.getResult());
                    }
                }

                // Get messages from context manager (will auto-compress if needed)
                List<Message> messages = contextManager.getMessagesForLLM();

                // Log context compression if it happened
                HistoryCompressor.CompressionResult compressionResult = contextManager.getLastCompressionResult();
                if (compressionResult != null && compressionResult.getWasCompressed()) {
                    log.info("🗜️  Context compressed at iteration {}: saved {} tokens",
                            iteration + 1, compressionResult.getTokensSaved());
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
                contextManager.addAssistantMessage(llmResponse);

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
                    log.info(contextManager.getContextSummary());
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

            log.info(contextManager.getContextSummary());
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
                    .model("deepseek-chat") // Explicitly set the model
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
            throw new RuntimeException("LLM call failed: " + e.getMessage(), e);
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
