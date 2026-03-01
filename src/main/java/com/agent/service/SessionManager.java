package com.agent.service;

import com.agent.model.dto.ChatSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 会话管理器 - 负责会话的存储、加载、删除等
 * 
 * 会话数据存储在 {storage-path}/sessions/ 目录下
 * 每个会话对应一个 JSON 文件
 */
@Slf4j
@Component
public class SessionManager {
    
    @Value("${agent.session.storage-path:./data/sessions}")
    private String storagePath;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, ChatSession> sessionCache = new HashMap<>();
    
    /**
     * 初始化存储目录
     */
    public void init() {
        File dir = new File(storagePath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                log.info("✅ Session storage directory created: {}", storagePath);
            }
        }
        loadAllSessions();
    }
    
    /**
     * 获取或创建会话
     */
    public ChatSession getOrCreateSession(String sessionId) {
        // 先从缓存查找
        if (sessionCache.containsKey(sessionId)) {
            return sessionCache.get(sessionId);
        }
        
        // 从文件加载
        ChatSession session = loadSession(sessionId);
        if (session == null) {
            // 创建新会话
            session = ChatSession.createNew();
            if (sessionId != null && !sessionId.isEmpty()) {
                session.setSessionId(sessionId);
            }
            log.info("📝 Created new session: {}", session.getSessionId());
        }
        
        sessionCache.put(session.getSessionId(), session);
        return session;
    }
    
    /**
     * 保存会话
     */
    public void saveSession(ChatSession session) {
        try {
            File file = new File(storagePath, session.getSessionId() + ".json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, session);
            sessionCache.put(session.getSessionId(), session);
            log.debug("💾 Session saved: {}", session.getSessionId());
        } catch (IOException e) {
            log.error("❌ Failed to save session: {}", session.getSessionId(), e);
        }
    }
    
    /**
     * 从文件加载会话
     */
    public ChatSession loadSession(String sessionId) {
        try {
            File file = new File(storagePath, sessionId + ".json");
            if (file.exists()) {
                ChatSession session = objectMapper.readValue(file, ChatSession.class);
                log.debug("📂 Session loaded from file: {}", sessionId);
                return session;
            }
        } catch (IOException e) {
            log.error("❌ Failed to load session: {}", sessionId, e);
        }
        return null;
    }
    
    /**
     * 加载所有会话到缓存
     */
    public void loadAllSessions() {
        File dir = new File(storagePath);
        if (!dir.exists()) {
            return;
        }
        
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try {
                    ChatSession session = objectMapper.readValue(file, ChatSession.class);
                    sessionCache.put(session.getSessionId(), session);
                    log.debug("✅ Loaded session: {}", session.getSessionId());
                } catch (IOException e) {
                    log.error("❌ Failed to load session file: {}", file.getName(), e);
                }
            }
        }
        log.info("✅ Loaded {} sessions from storage", sessionCache.size());
    }
    
    /**
     * 删除会话
     */
    public boolean deleteSession(String sessionId) {
        try {
            File file = new File(storagePath, sessionId + ".json");
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    sessionCache.remove(sessionId);
                    log.info("🗑️  Session deleted: {}", sessionId);
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("❌ Failed to delete session: {}", sessionId, e);
        }
        return false;
    }
    
    /**
     * 获取所有会话列表
     */
    public List<ChatSession> getAllSessions() {
        return new ArrayList<>(sessionCache.values());
    }
    
    /**
     * 获取会话总数
     */
    public int getSessionCount() {
        return sessionCache.size();
    }
    
    /**
     * 清空所有会话
     */
    public void clearAllSessions() {
        File dir = new File(storagePath);
        if (dir.exists()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
        sessionCache.clear();
        log.info("🗑️  All sessions cleared");
    }
}
