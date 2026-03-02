package com.agent.controller;

import com.agent.model.dto.ChatSession;
import com.agent.model.dto.ConversationMessage;
import com.agent.reasoning.engine.ExecutionContext;
import com.agent.reasoning.engine.ReasoningEngine;
import com.agent.service.SessionManager;
import com.agent.streaming.StreamingResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Chat Controller
 * 
 * REST API endpoints for agent interactions
 * 支持多轮对话历史存储
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
public class ChatController {

    private final ReasoningEngine reasoningEngine;
    private final SessionManager sessionManager;

    public ChatController(ReasoningEngine reasoningEngine, SessionManager sessionManager) {
        this.reasoningEngine = reasoningEngine;
        this.sessionManager = sessionManager;
    }

    /**
     * Chat endpoint - Execute agent reasoning for a user query
     * 支持会话历史存储
     * 
     * @param request Request body containing the user query and optional sessionId
     * @return Agent response with result, steps, duration and sessionId
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        log.info("Received chat request: {}", request.getQuery());

        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Query cannot be empty");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            // 获取或创建会话
            String sessionId = request.getSessionId();
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = UUID.randomUUID().toString();
            }
            ChatSession session = sessionManager.getOrCreateSession(sessionId);

            long startTime = System.currentTimeMillis();

            // 添加用户消息到历史
            session.addMessage("user", request.getQuery());

            // 构建对话历史上下文
            List<String> conversationHistory = session.getMessages().stream()
                    .map(msg -> msg.getRole().toUpperCase() + ": " + msg.getContent())
                    .collect(Collectors.toList());

            // Execute the agent reasoning with conversation context
            ExecutionContext context = reasoningEngine.execute(request.getQuery(), conversationHistory);

            // 添加助手回复到历史
            session.addMessage("assistant", context.getFinalAnswer());

            // 保存会话
            sessionManager.saveSession(session);

            long duration = System.currentTimeMillis() - startTime;

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("result", context.getFinalAnswer());
            response.put("iterations", context.getCurrentIteration());
            response.put("duration_ms", duration);
            response.put("is_complete", context.getIsComplete());
            response.put("messageCount", session.getMessageCount());

            // Add detailed steps if requested
            if (request.isIncludeDetails()) {
                response.put("steps", context.getThoughtActions().stream()
                        .map(ta -> {
                            Map<String, Object> step = new HashMap<>();
                            step.put("thought", ta.getThought() != null ? ta.getThought() : "");
                            step.put("action", ta.getAction() != null ? ta.getAction() : "");
                            step.put("action_input", ta.getActionInput() != null ? ta.getActionInput() : "");
                            return step;
                        })
                        .collect(Collectors.toList()));

                response.put("tool_results", context.getToolResults().stream()
                        .map(tr -> {
                            Map<String, Object> result = new HashMap<>();
                            result.put("tool_name", tr.getToolName());
                            result.put("result", tr.getResult());
                            result.put("success", tr.getSuccess());
                            result.put("execution_time_ms", tr.getExecutionTimeMs());
                            result.put("error", tr.getError() != null ? tr.getError() : "");
                            return result;
                        })
                        .collect(Collectors.toList()));
            }

            log.info("Chat request completed in {}ms with {} iterations, sessionId: {}",
                    duration, context.getCurrentIteration(), sessionId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing chat request", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 流式响应端点 - Server-Sent Events (SSE)
     * 实时流式传输 AI 响应
     * 
     * @param request 用户查询和会话 ID
     * @return SSE 流式响应，消息实时推送给客户端
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
        StreamingResponseHandler handler = new StreamingResponseHandler(emitter);

        // 在后台线程中异步处理请求
        handler.executeAsync(() -> {
            try {
                if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                    handler.sendError("Query cannot be empty");
                    try {
                        handler.close();
                    } catch (Exception e) {
                        log.error("❌ Error closing handler", e);
                    }
                    return;
                }

                log.info("🔄 Received streaming chat request: {}", request.getQuery());

                // 获取或创建会话
                String sessionId = request.getSessionId();
                if (sessionId == null || sessionId.isEmpty()) {
                    sessionId = UUID.randomUUID().toString();
                }
                ChatSession session = sessionManager.getOrCreateSession(sessionId);

                long startTime = System.currentTimeMillis();

                // 添加用户消息到历史
                session.addMessage("user", request.getQuery());

                // 发送会话 ID
                Map<String, String> sessionInfo = new HashMap<>();
                sessionInfo.put("sessionId", sessionId);
                handler.sendChunk("Session: " + sessionId + "\n");

                // 构建对话历史上下文
                List<String> conversationHistory = session.getMessages().stream()
                        .map(msg -> msg.getRole().toUpperCase() + ": " + msg.getContent())
                        .collect(Collectors.toList());

                // 发送开始信息
                handler.sendChunk("🤔 Reasoning...\n");

                // 执行推理引擎（可以逐步发送步骤信息）
                ExecutionContext context = reasoningEngine.execute(request.getQuery(), conversationHistory);

                // 发送逐个单词的流式响应
                String finalAnswer = context.getFinalAnswer();
                handler.sendChunk("\n📝 Response:\n");

                // 模拟流式传输：按句子分割返回
                String[] sentences = finalAnswer.split("(?<=[。！？；])|(?<=[.!?;])");
                for (String sentence : sentences) {
                    if (handler.isActive() && !sentence.trim().isEmpty()) {
                        handler.sendChunk(sentence.trim() + " ");
                        // 模拟延迟，使流式传输更明显
                        Thread.sleep(50);
                    }
                }

                // 添加助手回复到历史
                session.addMessage("assistant", finalAnswer);

                // 保存会话
                sessionManager.saveSession(session);

                long duration = System.currentTimeMillis() - startTime;

                // 构建最终结果
                Map<String, Object> finalResult = new HashMap<>();
                finalResult.put("sessionId", sessionId);
                finalResult.put("messageCount", session.getMessageCount());
                finalResult.put("duration_ms", duration);
                finalResult.put("iterations", context.getCurrentIteration());
                finalResult.put("is_complete", context.getIsComplete());

                // 如果请求了详细信息
                if (request.isIncludeDetails()) {
                    finalResult.put("steps", context.getThoughtActions().stream()
                            .map(ta -> {
                                Map<String, Object> step = new HashMap<>();
                                step.put("thought", ta.getThought() != null ? ta.getThought() : "");
                                step.put("action", ta.getAction() != null ? ta.getAction() : "");
                                step.put("action_input", ta.getActionInput() != null ? ta.getActionInput() : "");
                                return step;
                            })
                            .collect(Collectors.toList()));
                }

                // 发送完成标记
                handler.sendComplete(finalResult);

                log.info("✅ Streaming response completed in {}ms, sessionId: {}", duration, sessionId);

            } catch (Exception e) {
                log.error("❌ Error in streaming chat", e);
                handler.sendError("Error: " + e.getMessage());
            } finally {
                try {
                    handler.close();
                } catch (Exception e) {
                    log.error("❌ 关闭流式处理器时出错: {}", e.getMessage());
                }
            }
        });

        return emitter;
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        return ResponseEntity.ok(response);
    }

    /**
     * Request body for chat endpoint
     * 支持 sessionId 用于多轮对话
     * 支持 conversationHistory 用于提供上下文
     */
    public static class ChatRequest {
        private String query;
        private String sessionId;
        private List<String> conversationHistory;
        private boolean includeDetails = false;

        public ChatRequest() {
        }

        public ChatRequest(String query) {
            this.query = query;
        }

        public ChatRequest(String query, String sessionId) {
            this.query = query;
            this.sessionId = sessionId;
        }

        public ChatRequest(String query, String sessionId, boolean includeDetails) {
            this.query = query;
            this.sessionId = sessionId;
            this.includeDetails = includeDetails;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public List<String> getConversationHistory() {
            return conversationHistory;
        }

        public void setConversationHistory(List<String> conversationHistory) {
            this.conversationHistory = conversationHistory;
        }

        public boolean isIncludeDetails() {
            return includeDetails;
        }

        public void setIncludeDetails(boolean includeDetails) {
            this.includeDetails = includeDetails;
        }

        @Override
        public String toString() {
            return "ChatRequest{" +
                    "query='" + query + '\'' +
                    ", sessionId='" + sessionId + '\'' +
                    ", conversationHistory=" + (conversationHistory != null ? conversationHistory.size() : 0) +
                    ", includeDetails=" + includeDetails +
                    '}';
        }
    }
}
