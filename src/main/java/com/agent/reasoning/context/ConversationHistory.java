package com.agent.reasoning.context;

import com.agent.llm.model.dto.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Conversation Message
 * 
 * 封装对话中的单条消息，包含内容和元数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ConversationMessage {

    /**
     * 消息角色：user, assistant, system
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息时间戳
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * 消息的 token 数量
     */
    @Builder.Default
    private Integer tokenCount = 0;

    /**
     * 消息是否已被摘要
     */
    @Builder.Default
    private Boolean isSummarized = false;

    /**
     * 原始内容（如果被摘要）
     */
    private String originalContent;

    /**
     * 消息的轮次索引
     */
    private Integer turnIndex;

    /**
     * 消息的唯一 ID
     */
    @Builder.Default
    private String messageId = UUID.randomUUID().toString();

    /**
     * 转换为 Message DTO
     */
    public Message toMessage() {
        return Message.builder()
                .role(role)
                .content(content)
                .build();
    }

    /**
     * 更新 token 计数
     */
    public void updateTokenCount() {
        this.tokenCount = TokenCounter.estimateTokensPrecise(content);
    }
}

/**
 * Conversation History
 * 
 * 管理完整的对话历史：
 * - 存储所有消息
 * - 跟踪 token 使用
 * - 支持历史查询和管理
 * - 准备摘要生成
 */
@Slf4j
public class ConversationHistory {

    /**
     * 所有消息列表，按时间顺序
     */
    private final List<ConversationMessage> messages = new ArrayList<>();

    /**
     * 消息 ID 到消息的映射（快速查找）
     */
    private final Map<String, ConversationMessage> messageMap = new HashMap<>();

    /**
     * 系统提示词（总是保留）
     */
    private ConversationMessage systemPrompt;

    /**
     * 总 token 成本
     */
    private Integer totalTokens = 0;

    /**
     * 创建时间
     */
    private final LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdated = LocalDateTime.now();

    /**
     * 增加一条消息
     */
    public void addMessage(String role, String content) {
        ConversationMessage msg = ConversationMessage.builder()
                .role(role)
                .content(content)
                .turnIndex(messages.size())
                .build();

        msg.updateTokenCount();
        messages.add(msg);
        messageMap.put(msg.getMessageId(), msg);

        // 保留系统消息
        if ("system".equals(role)) {
            systemPrompt = msg;
        }

        totalTokens += msg.getTokenCount();
        lastUpdated = LocalDateTime.now();

        log.debug("Added message: {} (tokens: {})", role, msg.getTokenCount());
    }

    /**
     * 增加一条 Message 对象
     */
    public void addMessage(Message message) {
        addMessage(message.getRole(), message.getContent());
    }

    /**
     * 获取所有消息
     */
    public List<ConversationMessage> getAllMessages() {
        return new ArrayList<>(messages);
    }

    /**
     * 获取最近 N 条消息
     */
    public List<ConversationMessage> getRecentMessages(int count) {
        int start = Math.max(0, messages.size() - count);
        return new ArrayList<>(messages.subList(start, messages.size()));
    }

    /**
     * 获取所有 Message DTO（用于发送给 LLM）
     */
    public List<Message> getMessagesForLLM() {
        List<Message> result = new ArrayList<>();

        // 总是包含系统消息
        if (systemPrompt != null) {
            result.add(systemPrompt.toMessage());
        }

        // 加入其他消息
        for (ConversationMessage msg : messages) {
            if (msg != systemPrompt) {
                result.add(msg.toMessage());
            }
        }

        return result;
    }

    /**
     * 获取消息数量
     */
    public int getMessageCount() {
        return messages.size();
    }

    /**
     * 获取总 token 数
     */
    public int getTotalTokens() {
        return totalTokens;
    }

    /**
     * 获取非系统消息的 token 总数（用于计算压缩空间）
     */
    public int getNonSystemTokens() {
        return messages.stream()
                .filter(m -> m != systemPrompt)
                .mapToInt(ConversationMessage::getTokenCount)
                .sum();
    }

    /**
     * 清除所有非系统消息
     */
    public void clear() {
        messages.clear();
        messageMap.clear();
        if (systemPrompt != null) {
            messages.add(systemPrompt);
        }
        totalTokens = systemPrompt != null ? systemPrompt.getTokenCount() : 0;
        lastUpdated = LocalDateTime.now();
        log.info("Conversation history cleared");
    }

    /**
     * 获取某个轮次的消息（思考-行动-观察为一个轮次）
     */
    public List<ConversationMessage> getTurnMessages(int turnIndex) {
        return messages.stream()
                .filter(m -> m.getTurnIndex() != null && m.getTurnIndex() == turnIndex)
                .collect(Collectors.toList());
    }

    /**
     * 获取消息 ID 到消息的映射
     */
    public Map<String, ConversationMessage> getMessageMap() {
        return messageMap;
    }

    /**
     * 获取历史统计信息
     */
    public HistoryStats getStats() {
        return HistoryStats.builder()
                .totalMessages(messages.size())
                .totalTokens(totalTokens)
                .userMessages((int) messages.stream().filter(m -> "user".equals(m.getRole())).count())
                .assistantMessages((int) messages.stream().filter(m -> "assistant".equals(m.getRole())).count())
                .systemMessages((int) messages.stream().filter(m -> "system".equals(m.getRole())).count())
                .createdAt(createdAt)
                .lastUpdated(lastUpdated)
                .build();
    }

    /**
     * 历史统计数据
     */
    @Data
    @Builder
    public static class HistoryStats {
        private Integer totalMessages;
        private Integer totalTokens;
        private Integer userMessages;
        private Integer assistantMessages;
        private Integer systemMessages;
        private LocalDateTime createdAt;
        private LocalDateTime lastUpdated;
    }
}
