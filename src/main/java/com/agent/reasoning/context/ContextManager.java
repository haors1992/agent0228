package com.agent.reasoning.context;

import com.agent.llm.model.dto.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Context Manager
 * 
 * 统一管理 Agent 的上下文：
 * - 维护对话历史
 * - 管理 token 预算
 * - 自动压缩和摘要
 * - 优化 LLM 输入
 * 
 * 使用场景：
 * 1. 添加消息：addMessage(role, content)
 * 2. 检查容量：shouldCompress()
 * 3. 压缩历史：compress()
 * 4. 获取 LLM 消息：getMessagesForLLM()
 */
@Slf4j
@Component
public class ContextManager {

    /**
     * 对话历史管理器
     */
    private final ConversationHistory history;

    /**
     * 历史压缩器
     */
    private final HistoryCompressor compressor;

    /**
     * 最大 token 数（来自配置）
     */
    @Value("${agent.context.max-tokens:4096}")
    private Integer maxTokens;

    /**
     * 压缩阈值
     */
    @Value("${agent.context.compression-threshold:3000}")
    private Integer compressionThreshold;

    /**
     * 保留的最近消息数
     */
    @Value("${agent.context.keep-recent:5}")
    private Integer keepRecentCount;

    /**
     * 是否启用智能摘要
     */
    @Value("${agent.context.enable-summary:true}")
    private Boolean enableSmartSummary;

    /**
     * 最后一次压缩的结果
     */
    private HistoryCompressor.CompressionResult lastCompressionResult;

    public ContextManager() {
        this.history = new ConversationHistory();
        this.compressor = new HistoryCompressor();
    }

    /**
     * 初始化系统消息
     */
    public void initializeWithSystemPrompt(String systemPrompt) {
        history.addMessage("system", systemPrompt);
        log.info("Context initialized with system prompt ({} tokens)",
                TokenCounter.estimateTokensPrecise(systemPrompt));
    }

    /**
     * 添加用户消息
     */
    public void addUserMessage(String content) {
        history.addMessage("user", content);
        logContextStats();
    }

    /**
     * 添加助手消息
     */
    public void addAssistantMessage(String content) {
        history.addMessage("assistant", content);
        logContextStats();
    }

    /**
     * 添加观察消息
     */
    public void addObservation(String observation) {
        history.addMessage("user", "Observation: " + observation);
        logContextStats();
    }

    /**
     * 添加通用消息
     */
    public void addMessage(String role, String content) {
        history.addMessage(role, content);
        logContextStats();
    }

    /**
     * 添加 Message 对象
     */
    public void addMessage(Message message) {
        history.addMessage(message);
        logContextStats();
    }

    /**
     * 检查是否需要压缩
     */
    public boolean shouldCompress() {
        return compressor.shouldCompress(history);
    }

    /**
     * 获取距离上限的距离（百分比）
     */
    public double getTokenBudgetUsage() {
        return history.getTotalTokens() / (double) maxTokens;
    }

    /**
     * 获取剩余 token 数
     */
    public int getRemainingTokens() {
        return Math.max(0, maxTokens - history.getTotalTokens());
    }

    /**
     * 压缩历史
     */
    public boolean compress() {
        if (!shouldCompress()) {
            log.debug("No compression needed, tokens within limit");
            return false;
        }

        log.info("🔄 Compressing conversation history...");
        log.info("   Before: {} tokens ({} messages)",
                history.getTotalTokens(), history.getMessageCount());

        HistoryCompressor.CompressionConfig config = HistoryCompressor.CompressionConfig.builder()
                .maxTokens(maxTokens)
                .keepRecentCount(keepRecentCount)
                .compressionThreshold(compressionThreshold)
                .enableSmartSummary(enableSmartSummary)
                .build();

        HistoryCompressor compressorWithConfig = new HistoryCompressor(config);
        lastCompressionResult = compressorWithConfig.compress(history);

        if (lastCompressionResult.getWasCompressed()) {
            log.info("✅ Compression successful!");
            log.info("   After: {} tokens ({} messages)",
                    history.getTotalTokens(), history.getMessageCount());
            log.info("   Saved: {} tokens ({:.1f}%)",
                    lastCompressionResult.getTokensSaved(),
                    lastCompressionResult.getComressionRatio() * 100);
            log.info("   Methods: {}", lastCompressionResult.getSummarizedMessages());
            return true;
        }

        return false;
    }

    /**
     * 获取用于发送给 LLM 的消息列表
     * 会自动检查并执行压缩（如果需要）
     */
    public List<Message> getMessagesForLLM() {
        // 在返回前自动检查并压缩
        if (shouldCompress()) {
            compress();
        }

        return history.getMessagesForLLM();
    }

    /**
     * 获取所有消息（包括系统消息）
     */
    public List<Message> getAllMessages() {
        return history.getMessagesForLLM();
    }

    /**
     * 获取最近的 N 条消息
     */
    public List<Message> getRecentMessages(int count) {
        return history.getRecentMessages(count).stream()
                .map(m -> Message.builder()
                        .role(m.getRole())
                        .content(m.getContent())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取历史统计
     */
    public ConversationHistory.HistoryStats getStats() {
        return history.getStats();
    }

    /**
     * 获取上次压缩的结果
     */
    public HistoryCompressor.CompressionResult getLastCompressionResult() {
        return lastCompressionResult;
    }

    /**
     * 清除所有非系统消息
     */
    public void clear() {
        history.clear();
        log.info("Context cleared");
    }

    /**
     * 获取消息数量
     */
    public int getMessageCount() {
        return history.getMessageCount();
    }

    /**
     * 获取总 token 数
     */
    public int getTotalTokens() {
        return history.getTotalTokens();
    }

    /**
     * 获取详细的上下文摘要
     */
    public String getContextSummary() {
        ConversationHistory.HistoryStats stats = getStats();
        double usage = getTokenBudgetUsage() * 100;

        return String.format(
                "📊 Context Summary\n" +
                        "─────────────────\n" +
                        "Messages: %d (user: %d, assistant: %d, system: %d)\n" +
                        "Tokens: %d / %d (%.1f%%)\n" +
                        "Remaining: %d tokens\n" +
                        "Created: %s\n" +
                        "Last Updated: %s\n",
                stats.getTotalMessages(),
                stats.getUserMessages(),
                stats.getAssistantMessages(),
                stats.getSystemMessages(),
                stats.getTotalTokens(),
                maxTokens,
                usage,
                getRemainingTokens(),
                stats.getCreatedAt(),
                stats.getLastUpdated());
    }

    /**
     * 记录上下文统计
     */
    private void logContextStats() {
        double usage = getTokenBudgetUsage();
        String bar = createProgressBar(usage);

        log.debug("📈 Context: {} messages, {} tokens [{}] {:.1f}%",
                history.getMessageCount(),
                history.getTotalTokens(),
                bar,
                usage * 100);

        if (usage > 0.9) {
            log.warn("⚠️  Context approaching limit! ({:.1f}% full)", usage * 100);
        }
    }

    /**
     * 创建进度条
     */
    private String createProgressBar(double ratio) {
        int filled = (int) (ratio * 10);
        int empty = 10 - filled;
        return repeatChar('█', filled) + repeatChar('░', empty);
    }

    private String repeatChar(char c, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
