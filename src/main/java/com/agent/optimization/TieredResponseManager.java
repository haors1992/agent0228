package com.agent.optimization;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 分级响应管理器
 * 
 * 功能：
 * - 根据Token预算选择响应级别
 * - 生成对应的回答指导
 * - 自适应深度控制
 */
@Service
@Slf4j
public class TieredResponseManager {

    @Autowired(required = false)
    private TokenUsageService tokenUsageService;

    /**
     * 响应级别定义
     */
    public enum ResponseTier {
        BRIEF("简洁回答", 200, "直接、快速、核心信息"),
        NORMAL("标准回答", 500, "清晰、适度、完整逻辑"),
        DETAILED("详细回答", 1000, "深入、全面、包含例子"),
        VERBOSE("详尽回答", 2000, "完整、论证充分、多维度");

        public final String name;
        public final int maxTokens;
        public final String description;

        ResponseTier(String name, int maxTokens, String description) {
            this.name = name;
            this.maxTokens = maxTokens;
            this.description = description;
        }
    }

    /**
     * 响应预算
     */
    @Data
    public static class ResponseBudget {
        private int totalTokenBudget; // 总Token预算
        private int promptTokens; // System Prompt消耗
        private int contextTokens; // 上下文消耗
        private int knowledgeTokens; // 知识库消耗
        private int availableTokens; // 剩余可用Token

        public void calculateAvailable() {
            this.availableTokens = totalTokenBudget - promptTokens - contextTokens - knowledgeTokens;
        }
    }

    /**
     * 响应指导
     */
    @Data
    public static class ResponseGuidance {
        private ResponseTier tier; // 推荐级别
        private String instruction; // 给LLM的指导
        private int maxOutputTokens; // 输出Token限制
        private double confidenceScore; // 推荐置信度（0-1）
        private String explanation; // 推荐理由
    }

    /**
     * 根据Token预算选择响应级别
     * 
     * 考虑因素：
     * 1. Token剩余量
     * 2. 对话深度（轮数）
     * 3. 查询复杂度
     */
    public ResponseTier selectTier(
            ResponseBudget budget,
            int conversationDepth,
            String queryComplexity) {

        // 对话越深越激进
        if (conversationDepth > 5) {
            log.info("⚠️ Deep conversation (depth={}), using BRIEF tier", conversationDepth);
            return ResponseTier.BRIEF;
        }

        // 根据可用Token选择
        if (budget.getAvailableTokens() > 1500) {
            return ResponseTier.DETAILED;
        } else if (budget.getAvailableTokens() > 800) {
            return ResponseTier.NORMAL;
        } else if (budget.getAvailableTokens() > 300) {
            return ResponseTier.BRIEF;
        } else {
            log.warn("🚨 Critical token shortage: {} remaining", budget.getAvailableTokens());
            return ResponseTier.BRIEF;
        }
    }

    /**
     * 自动决策（结合多个因素）
     */
    public ResponseGuidance makeGuidanceDecision(
            ResponseBudget budget,
            int conversationDepth,
            String queryComplexity,
            boolean isFollowUp) {

        ResponseGuidance guidance = new ResponseGuidance();

        log.info("🤔 Making response guidance decision:");
        log.info("  - Budget: {} tokens available", budget.getAvailableTokens());
        log.info("  - Depth: {} turns", conversationDepth);
        log.info("  - Complexity: {}", queryComplexity);
        log.info("  - FollowUp: {}", isFollowUp);

        // 第1层：硬约束（Token严重不足）
        if (budget.getAvailableTokens() < 150) {
            guidance.setTier(ResponseTier.BRIEF);
            guidance.setConfidenceScore(1.0);
            guidance.setExplanation("Token严重不足，强制使用BRIEF模式");
            log.warn("🔴 CRITICAL: Token < 150, forcing BRIEF");
        }

        // 第2层：对话深度约束
        else if (conversationDepth > 7) {
            guidance.setTier(ResponseTier.BRIEF);
            guidance.setConfidenceScore(0.95);
            guidance.setExplanation("对话过深(7+轮)，节省Token");
            log.warn("🟠 DEEP: Conversation > 7 turns, using BRIEF");
        }

        // 第3层：后续问题优化
        else if (isFollowUp) {
            guidance.setTier(ResponseTier.NORMAL);
            guidance.setConfidenceScore(0.9);
            guidance.setExplanation("后续问题通常更聚焦");
            log.info("🔵 FOLLOWUP: Using NORMAL tier");
        }

        // 第4层：查询复杂度权衡
        else if ("complex".equalsIgnoreCase(queryComplexity)) {
            if (budget.getAvailableTokens() > 1200) {
                guidance.setTier(ResponseTier.DETAILED);
                guidance.setConfidenceScore(0.85);
                guidance.setExplanation("复杂查询，充足Token，使用DETAILED");
                log.info("🟢 COMPLEX: Using DETAILED tier");
            } else {
                guidance.setTier(ResponseTier.NORMAL);
                guidance.setConfidenceScore(0.8);
                guidance.setExplanation("复杂查询但Token有限，使用NORMAL");
                log.info("🟡 COMPLEX+LIMITED: Using NORMAL tier");
            }
        }

        // 第5层：缺省决策
        else {
            guidance.setTier(selectTier(budget, conversationDepth, queryComplexity));
            guidance.setConfidenceScore(0.75);
            guidance.setExplanation("标准决策流程");
            log.info("🔵 DEFAULT: Using {} tier", guidance.getTier().name);
        }

        // 生成指导内容
        guidance.setInstruction(buildResponseInstruction(guidance.getTier()));
        guidance.setMaxOutputTokens(guidance.getTier().maxTokens);

        return guidance;
    }

    /**
     * 给LLM的回答指导
     */
    public String buildResponseInstruction(ResponseTier tier) {
        if (tier == ResponseTier.BRIEF) {
            return "请用不超过50个字的简洁方式回答:\n" +
                    "- 提供核心答案\n" +
                    "- 省略细节和例子\n" +
                    "- 用要点列表";
        } else if (tier == ResponseTier.NORMAL) {
            return "请用150-200个字的标准方式回答:\n" +
                    "- 明确表述主要观点\n" +
                    "- 给出简短解释\n" +
                    "- 一个简短例子（可选）";
        } else if (tier == ResponseTier.DETAILED) {
            return "请用300-500个字的详细方式回答:\n" +
                    "- 完整阐述观点\n" +
                    "- 包含具体例子\n" +
                    "- 解释原理或背景\n" +
                    "- 适当的段落分割";
        } else {
            // VERBOSE
            return "请用完整的方式回答（500+字课对）:\n" +
                    "- 完中论述核心观点\n" +
                    "- 多个具体例子\n" +
                    "- 相关背景和原理\n" +
                    "- 对比分析\n" +
                    "- 实际应用建议";
        }
    }

    /**
     * 估算响应的Token预算
     */
    public ResponseBudget estimateBudget(
            int maxTotalTokens,
            int systemPromptTokens,
            int contextTokens,
            int knowledgeTokens) {

        ResponseBudget budget = new ResponseBudget();
        budget.setTotalTokenBudget(maxTotalTokens);
        budget.setPromptTokens(systemPromptTokens);
        budget.setContextTokens(contextTokens);
        budget.setKnowledgeTokens(knowledgeTokens);
        budget.calculateAvailable();

        return budget;
    }

    /**
     * 获取推荐级别列表（按推荐度排序）
     */
    public List<Map<String, Object>> getRankedTierRecommendations(ResponseBudget budget) {
        List<Map<String, Object>> recommendations = new ArrayList<>();

        for (ResponseTier tier : ResponseTier.values()) {
            if (tier.maxTokens <= budget.getAvailableTokens()) {
                Map<String, Object> rec = new HashMap<>();
                rec.put("tier", tier.name);
                rec.put("maxTokens", tier.maxTokens);
                rec.put("description", tier.description);
                rec.put("suitability",
                        tier == ResponseTier.NORMAL ? "最优平衡"
                                : tier == ResponseTier.DETAILED ? "信息充分"
                                        : tier == ResponseTier.VERBOSE ? "信息完整" : "快速节省");
                recommendations.add(rec);
            }
        }

        return recommendations;
    }

    /**
     * Token预算可视化
     */
    public String visualizeBudget(ResponseBudget budget) {
        int total = budget.getTotalTokenBudget();
        int prompt = budget.getPromptTokens();
        int context = budget.getContextTokens();
        int knowledge = budget.getKnowledgeTokens();
        int available = budget.getAvailableTokens();

        // 计算比例
        int promptBar = calculateBar(prompt, total);
        int contextBar = calculateBar(context, total);
        int knowledgeBar = calculateBar(knowledge, total);
        int availableBar = calculateBar(available, total);

        StringBuilder vis = new StringBuilder();
        vis.append("\n╔════════════════════════════════════════╗\n");
        vis.append("║       Token 预算分配可视化              ║\n");
        vis.append("├────────────────────────────────────────┤\n");
        vis.append(String.format("║ Prompt:    %s  %d/%d (%.1f%%) ║\n",
                repeatChar("█", promptBar), prompt, total, (prompt * 100.0 / total)));
        vis.append(String.format("║ Context:   %s  %d/%d (%.1f%%) ║\n",
                repeatChar("▓", contextBar), context, total, (context * 100.0 / total)));
        vis.append(String.format("║ Knowledge: %s  %d/%d (%.1f%%) ║\n",
                repeatChar("▒", knowledgeBar), knowledge, total, (knowledge * 100.0 / total)));
        vis.append(String.format("║ Available: %s  %d/%d (%.1f%%) ║\n",
                repeatChar("░", availableBar), available, total, (available * 100.0 / total)));
        vis.append("╚════════════════════════════════════════╝\n");

        return vis.toString();
    }

    private int calculateBar(int value, int total) {
        return Math.max(1, (value * 30) / total);
    }

    private String repeatChar(String ch, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.max(0, count); i++) {
            sb.append(ch);
        }
        return sb.toString();
    }
}
