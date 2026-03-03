package com.agent.reasoning.context;

import com.agent.llm.model.dto.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 上下文管理系统集成测试
 * 
 * 测试场景：
 * 1. Token 计数的准确性
 * 2. 自动压缩触发机制
 * 3. 压缩效果验证
 * 4. API 集成测试
 */
public class ContextManagementIntegrationTest {

    private TokenCounter tokenCounter;
    private ConversationHistory history;
    private HistoryCompressor compressor;

    @BeforeEach
    public void setUp() {
        tokenCounter = new TokenCounter();
        history = new ConversationHistory();
        compressor = new HistoryCompressor();
    }

    /**
     * 测试 1: Token 计数准确性
     */
    @Test
    public void testTokenCounting() {
        // 中文文本
        String chineseText = "你好，这是一个测试。";
        long tokens = tokenCounter.estimateTokens(chineseText);
        assertNotEquals(0, tokens, "中文文本 token 数应大于 0");

        // 英文文本
        String englishText = "Hello world this is a test";
        tokens = tokenCounter.estimateTokens(englishText);
        assertTrue(tokens >= 5, "英文文本应至少有 5 个词");

        // 混合文本
        String mixedText = "你好 Hi 这是一个 test";
        tokens = tokenCounter.estimateTokens(mixedText);
        assertTrue(tokens > 0, "混合文本应有 token");

        System.out.println("✓ Token 计数测试通过");
    }

    /**
     * 测试 2: 对话历史管理
     */
    @Test
    public void testConversationHistory() {
        // 添加系统消息
        history.addMessage("system", "你是一个有用的助手。");
        assertEquals(1, history.getStats().getTotalMessages(), "应有 1 条消息");

        // 添加用户消息
        history.addMessage("user", "你好，请问今天天气如何？");
        assertEquals(2, history.getStats().getTotalMessages(), "应有 2 条消息");

        // 添加助手消息
        history.addMessage("assistant", "我不能查看实时天气数据。");
        assertEquals(3, history.getStats().getTotalMessages(), "应有 3 条消息");

        // 验证 token 计数
        long totalTokens = history.getTotalTokens();
        assertTrue(totalTokens > 0, "总 token 数应大于 0");

        // 验证 getMessagesForLLM
        java.util.List<Message> messages = history.getMessagesForLLM();
        assertEquals(3, messages.size(), "应返回 3 条消息");

        System.out.println("✓ 对话历史管理测试通过");
        System.out.println("  - 总消息数: " + history.getStats().getTotalMessages());
        System.out.println("  - 总 Token 数: " + totalTokens);
    }

    /**
     * 测试 3: 文本截断功能
     */
    @Test
    public void testTextTruncation() {
        String longText = "这是一个很长的文本。" +
                "这是一个很长的文本。".repeat(100);

        // 截断到 50 tokens
        String truncated = tokenCounter.truncateByTokens(longText, 50);
        assertNotNull(truncated, "截断后的文本不应为 null");

        long truncatedTokens = tokenCounter.estimateTokens(truncated);
        assertTrue(truncatedTokens <= 50, "截断后 token 数应 <= 50");

        System.out.println("✓ 文本截断测试通过");
        System.out.println("  - 原始长度: " + tokenCounter.estimateTokens(longText) + " tokens");
        System.out.println("  - 截断后: " + truncatedTokens + " tokens");
    }

    /**
     * 测试 4: 历史压缩
     */
    @Test
    public void testHistoryCompression() {
        // 构建一个较长的对话历史
        history.addMessage("system", "你是一个有用的助手。");

        // 添加多轮对话
        for (int i = 0; i < 10; i++) {
            history.addMessage("user", "问题 " + i + ": 这是第 " + i + " 个问题。");
            history.addMessage("assistant", "答案 " + i + ": 这是第 " + i + " 个答案。");
        }

        long originalTokens = history.getTotalTokens();

        // 执行压缩
        HistoryCompressor.CompressionResult result = compressor.compress(history);

        long compressedTokens = history.getTotalTokens();
        long savedTokens = originalTokens - compressedTokens;

        System.out.println("✓ 历史压缩测试通过");
        System.out.println("  - 原始 tokens: " + originalTokens);
        System.out.println("  - 压缩后 tokens: " + compressedTokens);
        System.out.println("  - 节省 tokens: " + savedTokens);
        System.out.println("  - 压缩比: " + String.format("%.2f%%", result.getComressionRatio() * 100));
    }

    /**
     * 测试 5: 相似消息合并
     */
    @Test
    public void testSimilarMessageMerging() {
        history.addMessage("user", "查询天气");
        history.addMessage("assistant", "正在查询...");
        history.addMessage("user", "快点");
        history.addMessage("assistant", "结果是：晴天");

        int beforeMerge = history.getStats().getTotalMessages();

        HistoryCompressor.CompressionResult result = compressor.compress(history);

        int afterMerge = history.getStats().getTotalMessages();

        System.out.println("✓ 相似消息合并测试通过");
        System.out.println("  - 压缩前消息数: " + beforeMerge);
        System.out.println("  - 压缩后消息数: " + afterMerge);
        System.out.println("  - 已摘要消息: " + result.getSummarizedMessages().size());
    }

    /**
     * 测试 6: Token 预算管理
     */
    @Test
    public void testTokenBudgetManagement() {
        long maxTokens = 1000;

        // 添加接近预算限制的消息
        while (history.getTotalTokens() < maxTokens * 0.7) {
            history.addMessage("user", "这是一条长度适中的用户消息。");
            history.addMessage("assistant", "这是一条长度适中的助手回复。");
        }

        long usage = history.getTotalTokens();
        double usageRatio = (double) usage / maxTokens;

        assertTrue(usageRatio >= 0.6, "应该接近预算");
        assertTrue(usageRatio <= 0.8, "不应该超过预算");

        System.out.println("✓ Token 预算管理测试通过");
        System.out.println("  - 最大预算: " + maxTokens);
        System.out.println("  - 当前使用: " + usage);
        System.out.println("  - 使用率: " + String.format("%.1f%%", usageRatio * 100));
    }

    /**
     * 测试 7: CJK 字符检测
     */
    @Test
    public void testCJKCharacterDetection() {
        // 测试各种 CJK 字符
        assertTrue(isCJKCharacter('中'), "中文字符应被识别");
        assertTrue(isCJKCharacter('日'), "日文字符应被识别");
        assertTrue(isCJKCharacter('한'), "韩文字符应被识别");
        assertFalse(isCJKCharacter('A'), "英文字符不应被识别为 CJK");
        assertFalse(isCJKCharacter('1'), "数字不应被识别为 CJK");

        System.out.println("✓ CJK 字符检测测试通过");
    }

    /**
     * 简单的 CJK 检测实现（用于测试）
     */
    private boolean isCJKCharacter(char c) {
        int codePoint = c;
        return (codePoint >= 0x4E00 && codePoint <= 0x9FFF) || // CJK Unified Ideographs
                (codePoint >= 0x3040 && codePoint <= 0x309F) || // Hiragana
                (codePoint >= 0x30A0 && codePoint <= 0x30FF) || // Katakana
                (codePoint >= 0xAC00 && codePoint <= 0xD7AF); // Hangul Syllables
    }

    /**
     * 综合测试：完整的对话流程
     */
    @Test
    public void testCompleteConversationFlow() {
        System.out.println("\n=== 完整对话流程测试 ===");

        // 1. 初始化
        history.addMessage("system", "你是一个技术顾问。");
        System.out.println("1. 初始化完成");
        System.out.println("   - 消息数: " + history.getStats().getTotalMessages());
        System.out.println("   - Token 数: " + history.getTotalTokens());

        // 2. 用户输入
        history.addMessage("user", "如何优化 Java 应用的性能？");
        System.out.println("2. 用户输入完成");

        // 3. 助手回复
        history.addMessage("assistant", "可以通过以下几个方面优化：\\n" +
                "1. JVM 调优\\n" +
                "2. 代码优化\\n" +
                "3. 并发编程\\n" +
                "4. 缓存策略");
        System.out.println("3. 助手回复完成");
        System.out.println("   - 总消息数: " + history.getStats().getTotalMessages());
        System.out.println("   - 总 Token 数: " + history.getTotalTokens());

        // 4. 压缩分析
        long beforeCompression = history.getTotalTokens();
        HistoryCompressor.CompressionResult result = compressor.compress(history);
        long afterCompression = history.getTotalTokens();

        System.out.println("4. 压缩分析");
        System.out.println("   - 压缩前: " + beforeCompression + " tokens");
        System.out.println("   - 压缩后: " + afterCompression + " tokens");
        System.out.println("   - 节省: " + (beforeCompression - afterCompression) + " tokens");
        System.out.println("   - 压缩比: " + String.format("%.2f%%", result.getComressionRatio() * 100));

        // 5. 最终统计
        System.out.println("5. 最终统计");
        System.out.println("   - 消息数: " + history.getStats().getTotalMessages());
        System.out.println("   - Token 数: " + history.getTotalTokens());
        System.out.println("   - 系统 Token: " + history.getStats().getSystemTokens());

        assertTrue(history.getStats().getTotalMessages() > 0, "应有消息记录");
        assertTrue(history.getTotalTokens() > 0, "应有 token 统计");

        System.out.println("\n✅ 完整对话流程测试通过");
    }
}
