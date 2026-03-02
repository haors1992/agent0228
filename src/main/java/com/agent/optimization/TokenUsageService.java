package com.agent.optimization;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token 使用追踪和分析服务
 * 
 * 功能：
 * - Token 组件级计数
 * - 成本估算
 * - 重复率分析
 * - 使用统计
 */
@Service
@Slf4j
public class TokenUsageService {

    private static final double TOKENS_PER_WORD = 1.3;
    private static final double MESSAGE_OVERHEAD = 4.0;

    // Token成本配置（以美分为单位，适应不同LLM）
    private final Map<String, TokenCostConfig> costConfigs = new ConcurrentHashMap<>();

    // 使用历史记录
    private final Map<String, List<TokenUsageRecord>> usageHistory = new ConcurrentHashMap<>();

    public TokenUsageService() {
        // DeepSeek API 成本配置
        costConfigs.put("deepseek", new TokenCostConfig("DeepSeek", 0.001, 0.002));
        // 通用LLM配置
        costConfigs.put("default", new TokenCostConfig("Default", 0.005, 0.015));
    }

    /**
     * Token统计数据
     */
    @Data
    public static class TokenStats {
        private int promptTokens; // 输入Token数
        private int completionTokens; // 输出Token数
        private int totalTokens; // 总Token数
        private double costUSD; // 成本（美元）
        private double costCNY; // 成本（人民币，按1:7换算）
        private Map<String, Integer> breakdown; // 各部分Token数 {systemPrompt, history, knowledge, userQuery}
        private double redundancyRate; // 重复率
        private String timestamp;

        public TokenStats() {
            this.breakdown = new HashMap<>();
            this.timestamp = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        public void addCost(String model, int promptTokens, int completionTokens) {
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = promptTokens + completionTokens;

            // 使用DeepSeek费率
            TokenCostConfig config = TokenUsageService.DEFAULT_COST_CONFIG;
            this.costUSD = (promptTokens * config.inputCostPerKToken +
                    completionTokens * config.outputCostPerKToken) / 1000.0;
            this.costCNY = this.costUSD * 7.0; // 人民币换算
        }
    }

    /**
     * 成本配置
     */
    @Data
    public static class TokenCostConfig {
        private String name;
        private double inputCostPerKToken; // 每千个Token的输入成本（美元）
        private double outputCostPerKToken; // 每千个Token的输出成本（美元）

        public TokenCostConfig(String name, double inputCost, double outputCost) {
            this.name = name;
            this.inputCostPerKToken = inputCost;
            this.outputCostPerKToken = outputCost;
        }
    }

    static final TokenCostConfig DEFAULT_COST_CONFIG = new TokenCostConfig("DeepSeek", 0.001, 0.002);

    /**
     * 使用记录
     */
    @Data
    public static class TokenUsageRecord {
        private String sessionId;
        private int totalTokens;
        private double costUSD;
        private String timestamp;

        public TokenUsageRecord(String sessionId, int totalTokens, double costUSD) {
            this.sessionId = sessionId;
            this.totalTokens = totalTokens;
            this.costUSD = costUSD;
            this.timestamp = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }

    /**
     * 估算文本的 Token 数
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // 简化估算：单词数 × 1.3
        int wordCount = text.split("\\s+").length;
        return (int) Math.ceil(wordCount * TOKENS_PER_WORD);
    }

    /**
     * 估算消息列表的总 Token 数
     */
    public int estimateMessagesTokens(List<Object> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }

        int totalTokens = 0;

        for (Object msg : messages) {
            String content = msg.toString();
            int wordCount = content.split("\\s+").length;
            int messageTokens = (int) Math.ceil(wordCount * TOKENS_PER_WORD) + 4;
            totalTokens += messageTokens;
        }

        return totalTokens;
    }

    /**
     * 计算消息重复率（0-1之间）
     */
    public double calculateRedundancyRate(List<String> messages) {
        if (messages == null || messages.size() <= 1) {
            return 0;
        }

        Set<String> uniquePhrases = new HashSet<>();
        Set<String> duplicatePhrases = new HashSet<>();

        // 提取3词短语
        for (String msg : messages) {
            String[] words = msg.split("\\s+");
            for (int i = 0; i < words.length - 2; i++) {
                String phrase = words[i] + " " + words[i + 1] + " " + words[i + 2];

                if (uniquePhrases.contains(phrase)) {
                    duplicatePhrases.add(phrase);
                } else {
                    uniquePhrases.add(phrase);
                }
            }
        }

        if (uniquePhrases.isEmpty()) {
            return 0;
        }

        return (double) duplicatePhrases.size() / uniquePhrases.size();
    }

    /**
     * 记录Token使用
     */
    public void recordUsage(String sessionId, TokenStats stats) {
        double costUSD = stats.getCostUSD();
        usageHistory.computeIfAbsent(sessionId, k -> new ArrayList<>())
                .add(new TokenUsageRecord(sessionId, stats.getTotalTokens(), costUSD));

        log.info("📊 Token Usage - Session: {} | Total: {} tokens | Cost: ¥{:.4f} | Redundancy: {:.1f}%",
                sessionId,
                stats.getTotalTokens(),
                stats.getCostCNY(),
                stats.getRedundancyRate() * 100);
    }

    /**
     * 获取Session的使用统计
     */
    public Map<String, Object> getSessionStats(String sessionId) {
        List<TokenUsageRecord> records = usageHistory.getOrDefault(sessionId, new ArrayList<>());

        if (records.isEmpty()) {
            return new HashMap<>();
        }

        int totalTokens = records.stream().mapToInt(r -> r.totalTokens).sum();
        double totalCost = records.stream().mapToDouble(r -> r.costUSD).sum();
        double avgTokensPerTurn = (double) totalTokens / records.size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("sessionId", sessionId);
        stats.put("totalTokens", totalTokens);
        stats.put("totalCostUSD", String.format("$%.4f", totalCost));
        stats.put("totalCostCNY", String.format("¥%.2f", totalCost * 7.0));
        stats.put("averageTokensPerTurn", (int) avgTokensPerTurn);
        stats.put("turnCount", records.size());
        stats.put("records", records);

        return stats;
    }

    /**
     * 获取全局Token使用统计
     */
    public Map<String, Object> getGlobalStats() {
        int totalTokens = usageHistory.values().stream()
                .flatMap(List::stream)
                .mapToInt(r -> r.totalTokens)
                .sum();

        double totalCost = usageHistory.values().stream()
                .flatMap(List::stream)
                .mapToDouble(r -> r.costUSD)
                .sum();

        int sessionCount = usageHistory.size();
        int totalTurns = usageHistory.values().stream()
                .mapToInt(List::size)
                .sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTokens", totalTokens);
        stats.put("totalCostUSD", String.format("$%.4f", totalCost));
        stats.put("totalCostCNY", String.format("¥%.2f", totalCost * 7.0));
        stats.put("sessionCount", sessionCount);
        stats.put("totalTurns", totalTurns);
        stats.put("averageTokensPerTurn", totalTurns > 0 ? totalTokens / totalTurns : 0);

        return stats;
    }
}
