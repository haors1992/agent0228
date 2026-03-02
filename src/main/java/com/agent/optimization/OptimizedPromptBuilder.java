package com.agent.optimization;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 优化的 Prompt 构建器
 * 
 * 功能：
 * - System Prompt 缓存
 * - 动态精简 Prompt
 * - Token 预算管理
 * - 条件化组件（工具、示例、域指导）
 */
@Service
@Slf4j
public class OptimizedPromptBuilder {

    private static final int MAX_SYSTEM_PROMPT_TOKENS = 800;
    private static final double TOKENS_PER_WORD = 1.3;

    // System Prompt 缓存
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();

    /**
     * Prompt 配置类
     */
    @Data
    public static class PromptConfig {
        private String domain; // 领域（e.g., "编程", "数据分析"）
        private boolean includeTools; // 是否包含工具描述
        private boolean includeExamples; // 是否包含示例
        private int maxTokens; // Token限制
        private String language; // 语言（默认中文）

        public PromptConfig() {
            this.domain = null;
            this.includeTools = true;
            this.includeExamples = true;
            this.maxTokens = MAX_SYSTEM_PROMPT_TOKENS;
            this.language = "zh";
        }
    }

    /**
     * Prompt 级别
     */
    public enum PromptQuality {
        MINIMAL("最小化", 200),
        COMPACT("紧凑", 400),
        STANDARD("标准", 600),
        DETAILED("详细", 800);

        public final String name;
        public final int maxTokens;

        PromptQuality(String name, int maxTokens) {
            this.name = name;
            this.maxTokens = maxTokens;
        }
    }

    /**
     * 生成优化的 System Prompt
     * 
     * 策略：
     * 1. 检查缓存
     * 2. 按照重要性递阶生成
     * 3. 验证Token限制
     * 4. 存储缓存
     */
    public String buildSystemPrompt(PromptConfig config) {
        // 生成缓存键
        String cacheKey = generateCacheKey(config);

        // 检查缓存
        if (promptCache.containsKey(cacheKey)) {
            String cached = promptCache.get(cacheKey);
            log.info("✅ [Cache HIT] System Prompt cached, saved ~{} tokens",
                    estimateTokens(cached));
            return cached;
        }

        log.info("⚙️ [Cache MISS] Building new System Prompt for domain: {}",
                config.getDomain());

        // 构建Prompt
        String prompt = buildFromConfig(config);

        // 验证Token限制
        int tokens = estimateTokens(prompt);
        if (tokens > config.getMaxTokens()) {
            log.warn("⚠️ Prompt exceeds budget: {} > {}, degrading...",
                    tokens, config.getMaxTokens());
            prompt = degradePrompt(prompt, config.getMaxTokens());
        }

        // 缓存结果
        promptCache.put(cacheKey, prompt);

        log.info("💾 [Cache STORE] Prompt cached ({}): {} tokens",
                cacheKey, estimateTokens(prompt));

        return prompt;
    }

    /**
     * 从配置构建Prompt
     */
    private String buildFromConfig(PromptConfig config) {
        StringBuilder sb = new StringBuilder();

        // [强制] 核心身份
        sb.append("你是一个智能AI助手。").append("\n");

        // [强制] 回答原则
        sb.append("回答时请：\n");
        sb.append("1. 直接回答用户问题\n");
        sb.append("2. 给出清晰的解释\n");
        sb.append("3. 避免冗余信息\n\n");

        // [条件] 领域指导
        if (config.getDomain() != null && !config.getDomain().isEmpty()) {
            sb.append("【领域】").append(config.getDomain()).append("\n");
            sb.append(getDomainGuidance(config.getDomain())).append("\n\n");
        }

        // [条件] 工具描述
        if (config.isIncludeTools()) {
            sb.append("【可用工具】\n");
            sb.append("- 代码执行（运行Python/JavaScript）\n");
            sb.append("- 知识查询（搜索文档库）\n");
            sb.append("- 数据处理（JSON、CSV解析）\n\n");
        }

        // [条件] 简化示例（高质量优先）
        if (config.isIncludeExamples()) {
            sb.append("【示例】\n");
            sb.append("Q: 如何优化Python性能?\n");
            sb.append("A: 三个关键方向：\n");
            sb.append("1. 使用NumPy替代纯Python\n");
            sb.append("2. 减少函数调用开销\n");
            sb.append("3. 利用缓存（@lru_cache）\n\n");
        }

        // [强制] 结束提示
        sb.append("现在开始回答用户的问题。");

        return sb.toString();
    }

    /**
     * 降级Prompt - 移除非关键部分
     */
    private String degradePrompt(String prompt, int maxTokens) {
        String degraded = prompt;

        // 第1阶段：移除示例
        if (estimateTokens(degraded) > maxTokens) {
            degraded = degraded.replaceAll("【示例】[\\s\\S]*?\n\n", "");
            log.debug("⬇️ Removed examples: {} tokens", estimateTokens(degraded));
        }

        // 第2阶段：移除工具描述
        if (estimateTokens(degraded) > maxTokens) {
            degraded = degraded.replaceAll("【可用工具】[\\s\\S]*?\n\n", "");
            log.debug("⬇️ Removed tools: {} tokens", estimateTokens(degraded));
        }

        // 第3阶段：简化回答原则
        if (estimateTokens(degraded) > maxTokens) {
            degraded = degraded.replace(
                    "1. 直接回答用户问题\n2. 给出清晰的解释\n3. 避免冗余信息\n\n",
                    "直接回答问题。\n\n");
            log.debug("⬇️ Simplified principles: {} tokens", estimateTokens(degraded));
        }

        // 第4阶段（最后手段）：保留核心指令
        if (estimateTokens(degraded) > maxTokens) {
            degraded = "你是AI助手，请直接回答问题。";
            log.debug("⬇️ Minimal prompt: {} tokens", estimateTokens(degraded));
        }

        return degraded;
    }

    /**
     * 根据领域提供针对性指导
     */
    private String getDomainGuidance(String domain) {
        if (domain == null) {
            return "提供实用、准确、可靠的帮助。";
        }

        String lower = domain.toLowerCase();
        if (lower.contains("编程") || lower.contains("programming") || lower.contains("code")) {
            return "专注于代码质量、性能和最佳实践。提供具体的代码示例。";
        } else if (lower.contains("数据分析") || lower.contains("data") || lower.contains("analytics")) {
            return "强调数据准确性和分析方法论。使用数据驱动的结论。";
        } else if (lower.contains("写作") || lower.contains("writing") || lower.contains("content")) {
            return "关注内容清晰度和可读性。遵循写作规范。";
        } else if (lower.contains("通用") || lower.contains("general") || lower.contains("default")) {
            return "提供实用、准确、可靠的帮助。";
        } else {
            return "在【" + domain + "】领域提供专业帮助。";
        }
    }

    /**
     * 生成缓存键
     */
    private String generateCacheKey(PromptConfig config) {
        return String.format("%s_%d_%b_%b",
                config.getDomain() != null ? config.getDomain() : "default",
                config.getMaxTokens(),
                config.isIncludeTools(),
                config.isIncludeExamples());
    }

    /**
     * 估算Token数（简化版）
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int wordCount = text.split("\\s+").length;
        return (int) Math.ceil(wordCount * TOKENS_PER_WORD);
    }

    /**
     * 清除缓存（调试用）
     */
    public void clearCache() {
        promptCache.clear();
        log.info("🧹 Prompt cache cleared");
    }

    /**
     * 获取缓存统计
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", promptCache.size());

        int totalTokens = promptCache.values().stream()
                .mapToInt(this::estimateTokens)
                .sum();

        stats.put("totalTokens", totalTokens);
        stats.put("cachedPrompts", promptCache.keySet());

        // 计算缓存节省
        int savedTokens = totalTokens * (promptCache.size() - 1);
        stats.put("tokensSaved", savedTokens);

        return stats;
    }

    /**
     * 快速创建DEFAULT配置
     */
    public static PromptConfig createDefaultConfig() {
        return new PromptConfig();
    }

    /**
     * 快速创建精简配置
     */
    public static PromptConfig createCompactConfig() {
        PromptConfig config = new PromptConfig();
        config.setIncludeTools(false);
        config.setIncludeExamples(false);
        config.setMaxTokens(300);
        return config;
    }

    /**
     * 快速创建详细配置
     */
    public static PromptConfig createDetailedConfig(String domain) {
        PromptConfig config = new PromptConfig();
        config.setDomain(domain);
        config.setIncludeTools(true);
        config.setIncludeExamples(true);
        config.setMaxTokens(800);
        return config;
    }
}
