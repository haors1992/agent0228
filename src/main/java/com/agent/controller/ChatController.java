package com.agent.controller;

import com.agent.reasoning.engine.ExecutionContext;
import com.agent.reasoning.engine.ReasoningEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Chat Controller
 * 
 * REST API endpoints for agent interactions
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
public class ChatController {
    
    private final ReasoningEngine reasoningEngine;
    
    public ChatController(ReasoningEngine reasoningEngine) {
        this.reasoningEngine = reasoningEngine;
    }
    
    /**
     * Chat endpoint - Execute agent reasoning for a user query
     * 
     * @param request Request body containing the user query
     * @return Agent response with result, steps, and duration
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
            long startTime = System.currentTimeMillis();
            
            // Execute the agent reasoning
            ExecutionContext context = reasoningEngine.execute(request.getQuery());
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("result", context.getFinalAnswer());
            response.put("iterations", context.getCurrentIteration());
            response.put("duration_ms", duration);
            response.put("is_complete", context.getIsComplete());
            
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
            
            log.info("Chat request completed in {}ms with {} iterations", duration, context.getCurrentIteration());
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
     */
    public static class ChatRequest {
        private String query;
        private boolean includeDetails = false;
        
        public ChatRequest() {}
        
        public ChatRequest(String query) {
            this.query = query;
        }
        
        public ChatRequest(String query, boolean includeDetails) {
            this.query = query;
            this.includeDetails = includeDetails;
        }
        
        public String getQuery() {
            return query;
        }
        
        public void setQuery(String query) {
            this.query = query;
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
                ", includeDetails=" + includeDetails +
                '}';
        }
    }
}
