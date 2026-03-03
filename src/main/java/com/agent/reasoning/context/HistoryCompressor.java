package com.agent.reasoning.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * History Compressor
 * 
 * 压缩和优化对话历史：
 * 1. 识别关键信息
 * 2. 生成摘要
 * 3. 自动裁剪过长的历史
 * 4. 保留重要的上下文
 * 
 * 压缩策略：
 * - 保留：系统消息、最近的 K 条消息、关键决策
 * - 压缩：多轮相似对话、冗长的中间步骤
 * - 丢弃：重复信息、过时的观察
 */
@Slf4j
public class HistoryCompressor {

    /**
     * 压缩配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompressionConfig {
        /**
         * 最大 token 数
         */
        @Builder.Default
        private Integer maxTokens = 4096;

        /**
         * 保留最近 N 条消息（总是保留，不压缩）
         */
        @Builder.Default
        private Integer keepRecentCount = 5;

        /**
         * 触发压缩的 token 阈值
         */
        @Builder.Default
        private Integer compressionThreshold = 3000;

        /**
         * 每次压缩删除的历史消息数
         */
        @Builder.Default
        private Integer messagesToRemove = 2;

        /**
         * 是否启用智能摘要
         */
        @Builder.Default
        private Boolean enableSmartSummary = true;

        /**
         * 摘要缩减比例（0-1）
         */
        @Builder.Default
        private Double summaryReductionRatio = 0.4;
    }

    /**
     * 压缩结果
     */
    @Data
    @Builder
    public static class CompressionResult {
        /**
         * 是否进行了压缩
         */
        private Boolean wasCompressed;

        /**
         * 原始 token 数
         */
        private Integer originalTokens;

        /**
         * 压缩后 token 数
         */
        private Integer compressedTokens;

        /**
         * 节省的 token 数
         */
        private Integer tokensSaved;

        /**
         * 节省的百分比
         */
        private Double comressionRatio;

        /**
         * 压缩时间戳
         */
        private LocalDateTime compressedAt;

        /**
         * 压缩原因
         */
        private String reason;

        /**
         * 被摘要的消息
         */
        private List<String> summarizedMessages;
    }

    private final CompressionConfig config;

    public HistoryCompressor() {
        this(CompressionConfig.builder().build());
    }

    public HistoryCompressor(CompressionConfig config) {
        this.config = config;
    }

    /**
     * 检查是否需要压缩
     */
    public boolean shouldCompress(ConversationHistory history) {
        int tokens = history.getTotalTokens();
        return tokens > config.getCompressionThreshold();
    }

    /**
     * 执行历史压缩
     * 
     * @param history 对话历史
     * @return 压缩结果
     */
    public CompressionResult compress(ConversationHistory history) {
        int originalTokens = history.getTotalTokens();

        if (originalTokens <= config.getMaxTokens()) {
            log.debug("History within limit, no compression needed");
            return CompressionResult.builder()
                    .wasCompressed(false)
                    .originalTokens(originalTokens)
                    .compressedTokens(originalTokens)
                    .tokensSaved(0)
                    .comressionRatio(0.0)
                    .reason("Within token limit")
                    .build();
        }

        List<String> summarizedMessages = new ArrayList<>();

        // 执行压缩步骤
        while (history.getTotalTokens() > config.getMaxTokens()) {
            // 1. 尝试合并相邻的相似消息
            if (mergeSimilarMessages(history)) {
                summarizedMessages.add("Merged similar consecutive messages");
                continue;
            }

            // 2. 删除冗余的观察结果
            if (removeRedundantObservations(history)) {
                summarizedMessages.add("Removed redundant observations");
                continue;
            }

            // 3. 压缩旧的消息
            if (compressOldMessages(history)) {
                summarizedMessages.add("Compressed old message exchanges");
                continue;
            }

            // 4. 最后手段：删除最老的非系统消息
            if (removeOldestMessages(history, config.getMessagesToRemove())) {
                summarizedMessages.add("Removed " + config.getMessagesToRemove() + " oldest messages");
                continue;
            }

            // 无法进一步压缩，跳出循环
            break;
        }

        int compressedTokens = history.getTotalTokens();

        return CompressionResult.builder()
                .wasCompressed(true)
                .originalTokens(originalTokens)
                .compressedTokens(compressedTokens)
                .tokensSaved(originalTokens - compressedTokens)
                .comressionRatio((originalTokens - compressedTokens) / (double) originalTokens)
                .compressedAt(LocalDateTime.now())
                .reason("Exceeded token threshold: " + config.getCompressionThreshold())
                .summarizedMessages(summarizedMessages)
                .build();
    }

    /**
     * 合并相邻的相似消息
     */
    private boolean mergeSimilarMessages(ConversationHistory history) {
        List<ConversationMessage> messages = history.getAllMessages();

        for (int i = 0; i < messages.size() - 1; i++) {
            ConversationMessage msg1 = messages.get(i);
            ConversationMessage msg2 = messages.get(i + 1);

            // 如果两条消息角色相同且内容相似，合并它们
            if (msg1.getRole().equals(msg2.getRole()) &&
                    areSimilar(msg1.getContent(), msg2.getContent())) {

                String merged = msg1.getContent() + "\n" + msg2.getContent();
                msg1.setContent(merged);
                msg1.updateTokenCount();

                // 删除第二条消息
                messages.remove(i + 1);
                history.getMessageMap().remove(msg2.getMessageId());

                log.debug("Merged similar messages at index {}", i);
                return true;
            }
        }

        return false;
    }

    /**
     * 删除冗余的观察结果
     * 如果多条观察包含相同的关键信息，只保留第一条
     */
    private boolean removeRedundantObservations(ConversationHistory history) {
        List<ConversationMessage> messages = history.getAllMessages();
        Set<String> seenObservations = new HashSet<>();

        for (int i = messages.size() - 1; i >= 1; i--) {
            ConversationMessage msg = messages.get(i);

            if (msg.getContent().startsWith("Observation:")) {
                String observation = msg.getContent().substring("Observation:".length()).trim();
                String keyInfo = extractKeyInfo(observation);

                if (seenObservations.contains(keyInfo)) {
                    // 重复的观察，删除它
                    messages.remove(i);
                    history.getMessageMap().remove(msg.getMessageId());
                    log.debug("Removed redundant observation at index {}", i);
                    return true;
                }

                seenObservations.add(keyInfo);
            }
        }

        return false;
    }

    /**
     * 压缩旧的消息交换
     * 将多条旧的 user-assistant 对话替换为一条摘要
     */
    private boolean compressOldMessages(ConversationHistory history) {
        List<ConversationMessage> messages = history.getAllMessages();
        int recentCount = config.getKeepRecentCount();

        if (messages.size() <= recentCount + 4) {
            return false; // 消息不够多，无法压缩
        }

        // 找到最老的非系统消息对（应该在 recent 之前）
        int compressStart = 1; // 跳过系统消息
        int compressEnd = messages.size() - recentCount - 2;

        if (compressEnd <= compressStart + 1) {
            return false;
        }

        // 收集要压缩的消息
        List<ConversationMessage> toCompress = messages.subList(compressStart, compressEnd);

        // 生成摘要
        String summary = generateSummary(toCompress);

        // 用摘要替换这些消息
        ConversationMessage summaryMessage = ConversationMessage.builder()
                .role("assistant")
                .content("Previous context summary: " + summary)
                .isSummarized(true)
                .build();

        summaryMessage.updateTokenCount();

        // 删除旧消息
        for (int i = compressEnd - 1; i >= compressStart; i--) {
            ConversationMessage msg = messages.remove(i);
            history.getMessageMap().remove(msg.getMessageId());
        }

        // 插入摘要
        messages.add(compressStart, summaryMessage);

        log.debug("Compressed {} old messages into one summary", toCompress.size());
        return true;
    }

    /**
     * 删除最老的消息
     */
    private boolean removeOldestMessages(ConversationHistory history, int count) {
        List<ConversationMessage> messages = history.getAllMessages();

        // 保留系统消息和最近的消息
        int recentCount = config.getKeepRecentCount();

        if (messages.size() <= recentCount + 1) {
            return false;
        }

        int removeCount = 0;
        for (int i = 1; i < messages.size() - recentCount && removeCount < count; i++) {
            ConversationMessage msg = messages.remove(i);
            history.getMessageMap().remove(msg.getMessageId());
            removeCount++;
        }

        log.debug("Removed {} oldest messages", removeCount);
        return removeCount > 0;
    }

    /**
     * 生成消息摘要
     * 
     * @param messages 要摘要的消息列表
     * @return 摘要文本
     */
    private String generateSummary(List<ConversationMessage> messages) {
        if (messages.isEmpty()) {
            return "No relevant context";
        }

        // 1. 提取关键信息
        Set<String> keyPoints = new HashSet<>();

        for (ConversationMessage msg : messages) {
            if ("user".equals(msg.getRole())) {
                // 从用户消息提取问题或需求
                String keyInfo = extractKeyQuestionsAndNeeds(msg.getContent());
                if (!keyInfo.isEmpty()) {
                    keyPoints.add(keyInfo);
                }
            } else if ("assistant".equals(msg.getRole())) {
                // 从助手消息提取核心观点
                String keyInfo = extractCorePoints(msg.getContent());
                if (!keyInfo.isEmpty()) {
                    keyPoints.add(keyInfo);
                }
            }
        }

        // 2. 限制摘要大小
        int targetSize = (int) (messages.stream()
                .mapToInt(m -> m.getTokenCount() != null ? m.getTokenCount() : 0)
                .sum() * config.getSummaryReductionRatio());

        // 3. 组织摘要
        StringBuilder summary = new StringBuilder();
        summary.append("Key points from ").append(messages.size()).append(" previous messages: ");

        int count = 0;
        for (String point : keyPoints) {
            if (summary.length() + point.length() > targetSize && count > 1) {
                summary.append("...");
                break;
            }
            if (count > 0) {
                summary.append("; ");
            }
            summary.append(point);
            count++;
        }

        return summary.toString();
    }

    /**
     * 提取题目和需求
     */
    private String extractKeyQuestionsAndNeeds(String content) {
        // 简单实现：取前 100 个字符
        if (content.length() > 100) {
            return content.substring(0, 100) + "...";
        }
        return content;
    }

    /**
     * 提取核心观点
     */
    private String extractCorePoints(String content) {
        // 简单实现：找到包含关键词的句子
        String[] sentences = content.split("\\.\\.?");

        for (String sentence : sentences) {
            if (sentence.contains("answer") || sentence.contains("result") ||
                    sentence.contains("found") || sentence.contains("显示") ||
                    sentence.contains("指") || sentence.contains("是")) {
                return sentence.trim();
            }
        }

        // 如果没有找到，返回第一个句子
        if (sentences.length > 0) {
            return sentences[0].trim();
        }

        return "";
    }

    /**
     * 提取消息的关键信息（用于去重）
     */
    private String extractKeyInfo(String content) {
        // 取前 50 个字符作为关键信息
        if (content.length() > 50) {
            return content.substring(0, 50);
        }
        return content;
    }

    /**
     * 判断两个文本是否相似
     */
    private boolean areSimilar(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return false;
        }

        // 简单的相似度算法：计算共同单词比例
        String[] words1 = text1.toLowerCase().split("\\s+");
        String[] words2 = text2.toLowerCase().split("\\s+");

        Set<String> set1 = Arrays.stream(words1).collect(Collectors.toSet());
        Set<String> set2 = Arrays.stream(words2).collect(Collectors.toSet());

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        double similarity = intersection.size() / (double) Math.max(set1.size(), set2.size());

        return similarity > 0.6; // 相似度超过 60% 则认为相似
    }
}
