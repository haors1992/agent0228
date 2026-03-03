package com.agent.reasoning.context;

import lombok.extern.slf4j.Slf4j;

/**
 * Token Counter
 * 
 * 估算文本中的 token 数量
 * 使用与 GPT 类似的算法
 * 
 * Token 计数规则：
 * - 英文单词通常 1-2 tokens
 * - 标点符号通常 1 token
 * - 中文字符通常 1-2 tokens
 * - 特殊字符通常 1 token
 */
@Slf4j
public class TokenCounter {

    // 单词细分模式 - 用于英文 token 计数
    private static final String WORD_PATTERN = "\\w+";

    // 中文字符细分模式
    private static final String CJK_PATTERN = "[\\u2E80-\\u2EFF\\u2F00-\\u2FDF\\u3040-\\u309F\\u30A0-\\u30FF\\u3100-\\u312F\\u3200-\\u32FF\\u3400-\\u4DBF\\u4E00-\\u9FFF]";

    /**
     * 估算文本的 token 数量
     * 这是一个近似值，不同的 LLM 可能有不同的 tokenizer
     * 
     * @param text 要计算的文本
     * @return 估算的 token 数量
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int tokens = 0;

        // 1. 计算英文单词 token 数
        // 规则：大多数单词1个token，较长单词可能是2个
        int englishWords = text.split("\\s+").length;
        tokens += (int) (englishWords * 1.2); // 平均每个词 1.2 tokens

        // 2. 计算中文字符 token 数
        // 规则：每个中文字符约 1-2 tokens
        int chineseChars = countMatches(text, CJK_PATTERN);
        tokens += (int) (chineseChars * 1.5); // 每个中文字符 1.5 tokens

        // 3. 统计特殊字符和标点
        // 规则：特殊字符和标点通常是 1 token，但某些组合可能是多个
        int specialChars = text.length() - englishWords * 5; // 粗略估计
        tokens += (int) (specialChars * 0.3);

        // 4. 新行符号通常是分开的 tokens
        tokens += countMatches(text, "\n");

        return Math.max(tokens, 1); // 最少 1 token
    }

    /**
     * 更精确的 token 计数（适用于中英混合文本）
     * 使用更细致的规则
     * 
     * @param text 要计算的文本
     * @return 更精确的 token 数量
     */
    public static int estimateTokensPrecise(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int tokens = 0;

        // 逐字符扫描
        boolean inWord = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // 检查是否是中文字符
            if (isCJKCharacter(c)) {
                tokens += 2; // 中文字符通常算 2 个 tokens
            }
            // 检查是否是英文字符
            else if (Character.isLetter(c)) {
                if (!inWord) {
                    tokens++; // 新单词开始
                    inWord = true;
                }
                // word 内的字符不额外计算
            }
            // 检查是否是数字
            else if (Character.isDigit(c)) {
                if (!inWord) {
                    tokens++;
                    inWord = true;
                }
            }
            // 其他字符（空格、标点等）
            else {
                if (inWord) {
                    inWord = false;
                }
                if (!Character.isWhitespace(c)) {
                    tokens++; // 非空格的特殊字符
                }
            }
        }

        return Math.max(tokens, 1);
    }

    /**
     * 检查字符是否是 CJK（中日韩）字符
     */
    private static boolean isCJKCharacter(char c) {
        return (c >= 0x2E80 && c <= 0x2EFF) || // CJK Radicals Supplement
                (c >= 0x2F00 && c <= 0x2FDF) || // Kangxi Radicals
                (c >= 0x3040 && c <= 0x309F) || // Hiragana
                (c >= 0x30A0 && c <= 0x30FF) || // Katakana
                (c >= 0x3100 && c <= 0x312F) || // Bopomofo
                (c >= 0x3200 && c <= 0x32FF) || // Enclosed CJK Letters and Months
                (c >= 0x3400 && c <= 0x4DBF) || // CJK Unified Ideographs Extension A
                (c >= 0x4E00 && c <= 0x9FFF) || // CJK Unified Ideographs
                (c >= 0x20000 && c <= 0x2A6DF); // CJK Unified Ideographs Extension B
    }

    /**
     * 统计字符串中匹配模式的出现次数
     */
    private static int countMatches(String text, String pattern) {
        if (text == null || pattern == null || text.isEmpty()) {
            return 0;
        }

        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }

    /**
     * 获取文本前 N 个 tokens 对应的文本
     * 用于裁剪过长的文本
     * 
     * @param text      原文本
     * @param maxTokens 最大 token 数
     * @return 裁剪后的文本
     */
    public static String truncateByTokens(String text, int maxTokens) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        int targetLength = (int) (maxTokens * 4.5); // 粗略估计：1 token ≈ 4.5 字符
        if (text.length() <= targetLength) {
            return text;
        }

        String truncated = text.substring(0, Math.min(targetLength, text.length()));

        // 确保不在中间截断单词
        int lastSpace = truncated.lastIndexOf(' ');
        if (lastSpace > 0 && lastSpace > targetLength * 0.8) {
            return truncated.substring(0, lastSpace);
        }

        return truncated;
    }

    /**
     * 转换 token 数为人类可读的格式
     */
    public static String formatTokenCount(int tokens) {
        if (tokens < 1000) {
            return tokens + " tokens";
        } else if (tokens < 1_000_000) {
            return String.format("%.1f K tokens", tokens / 1000.0);
        } else {
            return String.format("%.1f M tokens", tokens / 1_000_000.0);
        }
    }
}
