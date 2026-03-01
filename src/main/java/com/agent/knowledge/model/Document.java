package com.agent.knowledge.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 知识库文档对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    /** 文档 ID */
    private String docId;

    /** 文档标题 */
    private String title;

    /** 文档内容 */
    private String content;

    /** 知识库类别/标签 */
    private String category;

    /** 文档来源 */
    private String source;

    /** 创建时间 */
    private long createdTime;

    /** 更新时间 */
    private long updatedTime;

    /** 文档元数据（JSON格式） */
    private String metadata;

    /**
     * 创建新文档
     */
    public static Document create(String title, String content, String category) {
        return Document.builder()
                .docId(UUID.randomUUID().toString())
                .title(title)
                .content(content)
                .category(category)
                .createdTime(System.currentTimeMillis())
                .updatedTime(System.currentTimeMillis())
                .build();
    }

    /**
     * 获取文档摘要（前200字）
     */
    public String getSummary() {
        if (content == null)
            return "";
        return content.length() > 200 ? content.substring(0, 200) + "..." : content;
    }
}
