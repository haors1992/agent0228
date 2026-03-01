package com.agent.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话消息 - 代表一轮对话中的单条消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessage {

    /** 消息角色：user 或 assistant */
    private String role;

    /** 消息内容 */
    private String content;

    /** 时间戳（毫秒） */
    private long timestamp;

    /** 消息ID（用于删除特定消息） */
    private String messageId;
}
