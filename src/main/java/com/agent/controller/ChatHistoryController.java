package com.agent.controller;

import com.agent.model.dto.ChatSession;
import com.agent.service.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话历史 REST API 控制器
 * 
 * 提供对话历史的查看、导出、删除等功能
 */
@Slf4j
@RestController
@RequestMapping("/api/chat/history")
public class ChatHistoryController {

    private final SessionManager sessionManager;

    public ChatHistoryController(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * 获取会话列表
     * GET /api/chat/history/sessions
     */
    @GetMapping("/sessions")
    public Map<String, Object> getSessions() {
        List<ChatSession> sessions = sessionManager.getAllSessions();
        Map<String, Object> response = new HashMap<>();
        response.put("total", sessions.size());
        response.put("sessions", sessions);
        return response;
    }

    /**
     * 获取指定会话的对话历史
     * GET /api/chat/history/sessions/{sessionId}
     */
    @GetMapping("/sessions/{sessionId}")
    public Map<String, Object> getSessionHistory(@PathVariable String sessionId) {
        ChatSession session = sessionManager.getOrCreateSession(sessionId);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getSessionId());
        response.put("title", session.getTitle());
        response.put("createdTime", session.getCreatedTime());
        response.put("lastActivityTime", session.getLastActivityTime());
        response.put("messageCount", session.getMessageCount());
        response.put("messages", session.getMessages());

        return response;
    }

    /**
     * 创建新会话
     * POST /api/chat/history/sessions
     */
    @PostMapping("/sessions")
    public Map<String, String> createSession(@RequestParam(required = false) String title) {
        ChatSession session = ChatSession.createNew();
        if (title != null && !title.isEmpty()) {
            session.setTitle(title);
        }
        sessionManager.saveSession(session);

        Map<String, String> response = new HashMap<>();
        response.put("sessionId", session.getSessionId());
        response.put("title", session.getTitle());
        response.put("message", "✅ Session created successfully");
        return response;
    }

    /**
     * 删除指定会话
     * DELETE /api/chat/history/sessions/{sessionId}
     */
    @DeleteMapping("/sessions/{sessionId}")
    public Map<String, String> deleteSession(@PathVariable String sessionId) {
        boolean success = sessionManager.deleteSession(sessionId);

        Map<String, String> response = new HashMap<>();
        if (success) {
            response.put("message", "✅ Session deleted successfully");
            response.put("sessionId", sessionId);
        } else {
            response.put("message", "❌ Session not found");
            response.put("sessionId", sessionId);
        }
        return response;
    }

    /**
     * 清空指定会话的消息
     * DELETE /api/chat/history/sessions/{sessionId}/messages
     */
    @DeleteMapping("/sessions/{sessionId}/messages")
    public Map<String, String> clearSessionMessages(@PathVariable String sessionId) {
        ChatSession session = sessionManager.getOrCreateSession(sessionId);
        session.clearMessages();
        sessionManager.saveSession(session);

        Map<String, String> response = new HashMap<>();
        response.put("message", "✅ Session messages cleared");
        response.put("sessionId", sessionId);
        return response;
    }

    /**
     * 删除单条消息
     * DELETE /api/chat/history/sessions/{sessionId}/messages/{messageId}
     */
    @DeleteMapping("/sessions/{sessionId}/messages/{messageId}")
    public Map<String, Object> deleteMessage(
            @PathVariable String sessionId,
            @PathVariable String messageId) {

        ChatSession session = sessionManager.getOrCreateSession(sessionId);
        boolean success = session.removeMessage(messageId);
        sessionManager.saveSession(session);

        Map<String, Object> response = new HashMap<>();
        if (success) {
            response.put("message", "✅ Message deleted successfully");
            response.put("sessionId", sessionId);
            response.put("messageId", messageId);
            response.put("remainingMessages", session.getMessageCount());
        } else {
            response.put("message", "❌ Message not found");
            response.put("sessionId", sessionId);
            response.put("messageId", messageId);
        }
        return response;
    }

    /**
     * 导出会话为 JSON
     * GET /api/chat/history/sessions/{sessionId}/export
     */
    @GetMapping("/sessions/{sessionId}/export")
    public ChatSession exportSession(@PathVariable String sessionId) {
        return sessionManager.getOrCreateSession(sessionId);
    }

    /**
     * 获取统计信息
     * GET /api/chat/history/stats
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        List<ChatSession> sessions = sessionManager.getAllSessions();

        int totalMessages = 0;
        int totalSessions = sessions.size();

        for (ChatSession session : sessions) {
            totalMessages += session.getMessageCount();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("totalSessions", totalSessions);
        response.put("totalMessages", totalMessages);
        response.put("averageMessagesPerSession",
                totalSessions > 0 ? (double) totalMessages / totalSessions : 0);

        return response;
    }
}
