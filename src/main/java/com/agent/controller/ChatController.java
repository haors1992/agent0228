package com.agent.controller;

import com.agent.model.dto.ChatSession;
import com.agent.reasoning.engine.ExecutionContext;
import com.agent.reasoning.engine.ReasoningEngine;
import com.agent.service.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
            
            // Execute the agent reasoning
            ExecutionContext context = reasoningEngine.execute(request.getQuery());
            
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
     */
    public static class ChatRequest {
        private String query;
        private String sessionId;
        private boolean includeDetails = false;
        
        public ChatRequest() {}
        
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
                ", includeDetails=" + includeDetails +
                '}';
        }
    }
}
