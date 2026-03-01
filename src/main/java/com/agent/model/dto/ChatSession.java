package com.agent.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 会话对象 - 代表一次用户对话会话
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    /** 会话 ID - 唯一标识 */
    private String sessionId;

    /** 会话创建时间 */
    private long createdTime;

    /** 最后活动时间 */
    private long lastActivityTime;

    /** 会话标题（可选） */
    private String title;

    /** 对话消息列表 */
    private List<ConversationMessage> messages;

    /** 元数据：对话轮次 */
    private int messageCount;

    /**
     * 创建新会话
     */
    public static ChatSession createNew() {
        return ChatSession.builder()
                .sessionId(UUID.randomUUID().toString())
                .createdTime(System.currentTimeMillis())
                .lastActivityTime(System.currentTimeMillis())
                .title("Conversation " + System.currentTimeMillis())
                .messages(new ArrayList<>())
                .messageCount(0)
                .build();
    }

    /**
     * 添加消息
     */
    public void addMessage(String role, String content) {
        ConversationMessage msg = ConversationMessage.builder()
                .role(role)
                .content(content)
                .timestamp(System.currentTimeMillis())
                .messageId(UUID.randomUUID().toString())
                .build();

        this.messages.add(msg);
        this.messageCount++;
        this.lastActivityTime = System.currentTimeMillis();
    }

    /**
     * 删除单条消息
     */
    public boolean removeMessage(String messageId) {
        boolean removed = this.messages.removeIf(msg -> msg.getMessageId().equals(messageId));
        if (removed) {
            this.messageCount--;
            this.lastActivityTime = System.currentTimeMillis();
        }
        return removed;
    }

    /**
     * 清空消息
     */
    public void clearMessages() {
        this.messages.clear();
        this.messageCount = 0;
        this.lastActivityTime = System.currentTimeMillis();
    }
}
