package com.agent.optimization;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 对话历史管理器 - 实现滑动窗口和历史优化
 * 
 * 功能：
 * - 滑动窗口（最近K轮）
 * - Token预算管理
 * - 旧历史压缩
 * - 重要消息保留
 */
@Service
@Slf4j
public class ConversationHistoryManager {

    // 配置参数
    private static final int WINDOW_SIZE = 5; // 保留最近5轮对话
    private static final int MAX_HISTORY_TOKENS = 3000; // 历史最多3K tokens
    private static final int MIN_TURN_TOKENS = 100; // 每轮最少100 tokens保留

    @Autowired
    private TokenUsageService tokenUsageService;

    /**
     * 对话轮次数据
     */
    @Data
    public static class ConversationTurn {
        private String turnId;
        private long timestamp;
        private String role; // user/assistant
        private String content; // 原始内容
        private String summary; // 压缩后的摘要
        private int tokens; // Token数
        private double importance; // 重要性评分（0-1）
        private boolean isFollowUp; // 是否是后续问题

        public ConversationTurn(String role, String content) {
            this.turnId = UUID.randomUUID().toString();
            this.timestamp = System.currentTimeMillis();
            this.role = role;
            this.content = content;
            this.tokens = (int) (content.split("\\s+").length * 1.3);
            this.importance = 0.5; // 默认中等重要性
        }
    }

    /**
     * 构建带内存预算的上下文消息
     * 
     * 策略：
     * 1. 从最新消息开始反向添加
     * 2. 每条消息检查Token预算
     * 3. 超预算时使用摘要替代
     */
    public List<Map<String, String>> buildOptimizedMessages(
            String sessionId,
            List<ConversationTurn> fullHistory) {

        if (fullHistory == null || fullHistory.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, String>> result = new ArrayList<>();
        int tokenBudget = MAX_HISTORY_TOKENS;
        int turnsIncluded = 0;

        log.info("🔨 Building optimized context for session: {}", sessionId);
        log.info("📊 Full history: {} turns, {} tokens",
                fullHistory.size(),
                fullHistory.stream().mapToInt(t -> t.tokens).sum());

        // 从最新消息开始反向迭代
        for (int i = Math.min(fullHistory.size(), WINDOW_SIZE) - 1; i >= 0; i--) {
            ConversationTurn turn = fullHistory.get(i);

            if (turn.getTokens() <= tokenBudget) {
                // 如果有足够的Token预算，添加完整消息
                result.add(0, createMessage(turn.getRole(), turn.getContent()));
                tokenBudget -= turn.getTokens();
                turnsIncluded++;

                log.debug("✅ Included turn {}: {} tokens, {} remaining",
                        turnsIncluded, turn.getTokens(), tokenBudget);
            } else if (turn.getTokens() <= MIN_TURN_TOKENS) {
                // Token少的消息优先保留
                result.add(0, createMessage(turn.getRole(), turn.getContent()));
                tokenBudget -= turn.getTokens();
                turnsIncluded++;
            } else if (turn.getSummary() != null) {
                // 使用摘要替代完整消息
                String summaryContent = "【之前讨论总结】" + turn.getSummary();
                int summaryTokens = (int) (summaryContent.split("\\s+").length * 1.3);

                if (summaryTokens <= tokenBudget) {
                    result.add(0, createMessage(turn.getRole(), summaryContent));
                    tokenBudget -= summaryTokens;
                    turnsIncluded++;
                    log.debug("📝 Used summary for turn {}: {} tokens",
                            (i + 1), summaryTokens);
                }
                break;
            } else {
                // Token预算用尽，停止添加
                log.debug("💰 Token budget exhausted, stopping");
                break;
            }
        }

        log.info("✅ Built optimized context: {} turns, {} tokens remaining",
                turnsIncluded, tokenBudget);

        return result;
    }

    /**
     * 判断对话是否应该启用激进的Token节省模式
     */
    public boolean shouldEnableSavingMode(List<ConversationTurn> history) {
        if (history == null || history.isEmpty()) {
            return false;
        }

        // 条件1：超过5轮对话
        if (history.size() > 5) {
            log.warn("⚠️ Conversation too long ({}), enabling saving mode", history.size());
            return true;
        }

        // 条件2：历史Token数超过限制
        int totalTokens = history.stream().mapToInt(ConversationTurn::getTokens).sum();
        if (totalTokens > MAX_HISTORY_TOKENS) {
            log.warn("⚠️ History tokens too high ({}), enabling saving mode", totalTokens);
            return true;
        }

        return false;
    }

    /**
     * 计算消息的重要性评分
     * 
     * 评分依据：
     * - 消息长度（越长越重要）
     * - 包含的关键词数量
     * - 用户明确提出的问题（role=user时更重要）
     */
    public double calculateImportance(ConversationTurn turn) {
        double score = 0.5; // 基础分

        // 长度因子（0.05-0.3）
        int wordCount = turn.getContent().split("\\s+").length;
        score += Math.min(wordCount / 100.0, 0.3);

        // 角色因子
        if ("user".equals(turn.getRole())) {
            score += 0.1; // 用户消息略重要一些
        }

        // 问号因子（表示问题）
        if (turn.getContent().contains("?") || turn.getContent().contains("？")) {
            score += 0.15;
        }

        // 关键词因子
        String content = turn.getContent().toLowerCase();
        String[] keywords = { "error", "bug", "问题", "错误", "help", "怎样", "如何" };
        for (String keyword : keywords) {
            if (content.contains(keyword)) {
                score += 0.1;
                break;
            }
        }

        return Math.min(score, 1.0);
    }

    /**
     * 压缩历史 - 将多轮对话总结为关键信息
     * 
     * 提取：
     * - 用户的主要问题
     * - 系统的主要答案
     * - 对话的核心目标
     */
    public String compressHistory(List<ConversationTurn> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }

        List<String> keyPoints = new ArrayList<>();

        // 提取用户问题
        List<String> userQuestions = history.stream()
                .filter(t -> "user".equals(t.getRole()))
                .map(t -> extractKeyword(t.getContent(), 30))
                .collect(Collectors.toList());

        if (!userQuestions.isEmpty()) {
            keyPoints.add("用户问: " + String.join("; ", userQuestions));
        }

        // 提取关键回答
        List<String> keyReplies = history.stream()
                .filter(t -> "assistant".equals(t.getRole()))
                .map(t -> extractKeyword(t.getContent(), 50))
                .limit(2) // 只保留最近2个回答
                .collect(Collectors.toList());

        if (!keyReplies.isEmpty()) {
            keyPoints.add("回答: " + String.join("; ", keyReplies));
        }

        return String.join(" | ", keyPoints);
    }

    /**
     * 从文本提取关键词（前N个字符）
     */
    private String extractKeyword(String text, int maxLength) {
        String cleaned = text.replaceAll("[，。！？；：\n]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.length() > maxLength) {
            return cleaned.substring(0, maxLength) + "...";
        }

        return cleaned;
    }

    /**
     * 清理过旧的消息（超过N轮）
     */
    public List<ConversationTurn> pruneOldMessages(
            List<ConversationTurn> history,
            int maxTurns) {

        if (history == null || history.size() <= maxTurns) {
            return history;
        }

        List<ConversationTurn> pruned = history.subList(history.size() - maxTurns, history.size());

        log.info("🧹 Pruned {} old messages, keeping {} recent turns",
                history.size() - maxTurns, pruned.size());

        return new ArrayList<>(pruned);
    }

    /**
     * 消息对象创建辅助方法
     */
    private Map<String, String> createMessage(String role, String content) {
        Map<String, String> msg = new HashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        return msg;
    }

    /**
     * 估计消息列表的总Token数
     */
    public int estimateTotalTokens(List<ConversationTurn> history) {
        return history.stream()
                .mapToInt(ConversationTurn::getTokens)
                .sum();
    }

    /**
     * 获取消息重复率（用于检测冗余）
     */
    public double getRedundancyRate(List<ConversationTurn> history) {
        List<String> contents = history.stream()
                .map(ConversationTurn::getContent)
                .collect(Collectors.toList());

        return tokenUsageService.calculateRedundancyRate(contents);
    }
}
