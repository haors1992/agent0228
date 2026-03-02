package com.agent.optimization;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能知识库检索服务
 * 
 * 功能：
 * - 多阶段精确检索
 * - 相似度过滤
 * - Token 预算管理
 * - 自适应文档摘要
 */
@Service
@Slf4j
public class SmartKnowledgeRetrieval {

    // 检索配置
    private static final int INITIAL_TOP_K = 10; // 初始候选集大小
    private static final double MIN_SIMILARITY = 0.65; // 相似度阈值
    private static final int MAX_RESULT_TOKENS = 1500; // 结果Token限制
    private static final double TOKENS_PER_WORD = 1.3;

    /**
     * 知识库文档
     */
    @Data
    public static class Document {
        private String id;
        private String title;
        private String content;
        private String summary; // 简化摘要（可选）
        private double similarity; // 与查询的相似度
        private int tokens; // 内容的Token数
        private long timestamp;
        private String source; // 来源

        public Document(String id, String title, String content) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.tokens = estimateTokens(content);
            this.timestamp = System.currentTimeMillis();
        }

        private static int estimateTokens(String text) {
            int wordCount = text.split("\\s+").length;
            return (int) Math.ceil(wordCount * TOKENS_PER_WORD);
        }
    }

    /**
     * 搜索结果
     */
    @Data
    public static class SearchResult {
        private String id;
        private String title;
        private String content; // 完整内容（可能被修剪）
        private String summary; // 摘要版本
        private double similarity;
        private int tokensUsed; // 本文档实际使用的Token数
        private SearchResultType resultType;

        public enum SearchResultType {
            FULL("完整内容"),
            SUMMARY("摘要"),
            SNIPPET("片段");

            public final String description;

            SearchResultType(String description) {
                this.description = description;
            }
        }
    }

    /**
     * 多阶段智能检索
     * 
     * 流程：
     * 1. 初始检索 - 获取候选集
     * 2. 质量过滤 - 按相似度筛选
     * 3. Token控制 - 按预算选择文档
     * 4. 自适应摘要 - 根据Token情况返回完整/摘要
     */
    public List<SearchResult> smartSearch(String query, boolean isFollowUp) {
        log.info("🔍 Smart search initiated - Query: '{}', FollowUp: {}",
                query, isFollowUp);

        // 阶段1：初始检索
        List<Document> candidates = performSemanticSearch(query, INITIAL_TOP_K);
        log.info("📊 Phase 1 - Retrieved {} candidates", candidates.size());

        // 阶段2：相似度过滤
        List<Document> filtered = candidates.stream()
                .filter(doc -> doc.getSimilarity() >= MIN_SIMILARITY)
                .collect(Collectors.toList());
        log.info("📊 Phase 2 - Passed similarity filter: {}/{} (threshold: {})",
                filtered.size(), candidates.size(), MIN_SIMILARITY);

        // 阶段3：Token预算控制
        List<SearchResult> results = new ArrayList<>();
        int tokenBudget = MAX_RESULT_TOKENS;

        for (Document doc : filtered) {
            SearchResult result = new SearchResult();
            result.setId(doc.getId());
            result.setTitle(doc.getTitle());
            result.setSimilarity(doc.getSimilarity());

            // 决定返回类型
            if (doc.getTokens() <= tokenBudget) {
                // Token充足，返回完整内容
                result.setContent(doc.getContent());
                result.setTokensUsed(doc.getTokens());
                result.setResultType(SearchResult.SearchResultType.FULL);

                tokenBudget -= doc.getTokens();
            } else if (doc.getSummary() != null &&
                    estimateTokens(doc.getSummary()) <= tokenBudget) {
                // Token不足但有摘要，返回摘要
                result.setContent(doc.getSummary());
                result.setTokensUsed(estimateTokens(doc.getSummary()));
                result.setResultType(SearchResult.SearchResultType.SUMMARY);

                tokenBudget -= result.getTokensUsed();
            } else {
                // Token严重不足，生成片段
                String snippet = generateSnippet(doc.getContent(), tokenBudget);
                result.setContent(snippet);
                result.setTokensUsed(estimateTokens(snippet));
                result.setResultType(SearchResult.SearchResultType.SNIPPET);

                tokenBudget -= result.getTokensUsed();
            }

            results.add(result);

            // 检查是否还有Token预算
            if (tokenBudget <= 100) {
                log.debug("💰 Token budget nearly exhausted ({} remaining), stopping",
                        tokenBudget);
                break;
            }
        }

        // 阶段4：后续问题的激进优化
        if (isFollowUp && results.size() > 3) {
            results = results.subList(0, 3);
            log.info("📊 Phase 4 - Follow-up question detected, reduced to top 3 docs");
        }

        log.info("✅ Final results: {} documents, {} tokens used",
                results.size(), MAX_RESULT_TOKENS - tokenBudget);

        return results;
    }

    /**
     * 模拟语义搜索（实际应集成向量数据库）
     */
    private List<Document> performSemanticSearch(String query, int topK) {
        // 这里应该调用实际的向量数据库或搜索服务
        // 模拟实现：返回空列表
        List<Document> results = new ArrayList<>();

        log.debug("🔎 Semantic search would retrieve {} documents for: '{}'",
                topK, query);

        return results;
    }

    /**
     * 生成文档片段（对于Token严重不足的情况）
     * 
     * 提取：
     * - 前N个句子
     * - 包含关键词的句子
     */
    private String generateSnippet(String content, int maxTokens) {
        String[] sentences = content.split("[。!?！？\n]");

        int targetTokens = Math.max(50, maxTokens);
        int tokenCount = 0;
        StringBuilder snippet = new StringBuilder();

        for (String sentence : sentences) {
            if (sentence.trim().isEmpty())
                continue;

            int sentenceTokens = estimateTokens(sentence);
            if (tokenCount + sentenceTokens <= targetTokens) {
                snippet.append(sentence).append("。");
                tokenCount += sentenceTokens;
            } else {
                break;
            }
        }

        String result = snippet.toString();
        if (result.length() > 200) {
            result = result.substring(0, 200) + "...";
        }

        return result.isEmpty() ? content.substring(0, Math.min(100, content.length()))
                : result;
    }

    /**
     * 估算Token数
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int wordCount = text.split("\\s+").length;
        return (int) Math.ceil(wordCount * TOKENS_PER_WORD);
    }

    /**
     * 为文档生成摘要（可由LLM调用）
     * 
     * 规则：
     * - 提取前3个关键句
     * - 去重复词汇
     * - 控制在50-100字内
     */
    public String generateDocumentSummary(Document doc) {
        String[] sentences = doc.getContent().split("[。!?！？]");

        List<String> keySentences = new ArrayList<>();

        for (int i = 0; i < Math.min(3, sentences.length); i++) {
            String sentence = sentences[i].trim();
            if (sentence.length() > 20) {
                keySentences.add(sentence);
            }
        }

        String summary = String.join("。", keySentences) + "。";

        // 限制长度
        if (summary.length() > 150) {
            summary = summary.substring(0, 150) + "...";
        }

        log.debug("📝 Generated summary for doc {}: {} chars",
                doc.getId(), summary.length());

        return summary;
    }

    /**
     * 检测查询是否是后续问题
     * 
     * 特征：
     * - 简短（< 30 words）
     * - 包含"帮我"、"继续"等关键词
     * - 没有完整的陈述句
     */
    public boolean isFollowUpQuery(String query, String previousQuery) {
        // 长度检查
        int wordCount = query.split("\\s+").length;
        if (wordCount > 30) {
            return false;
        }

        // 关键词检查
        String[] followUpMarkers = { "帮我", "继续", "再", "还有", "另外", "顺便" };
        for (String marker : followUpMarkers) {
            if (query.contains(marker)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取检索统计
     */
    public Map<String, Object> getRetrievalStats(List<SearchResult> results) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("resultCount", results.size());

        int totalTokens = results.stream()
                .mapToInt(SearchResult::getTokensUsed)
                .sum();
        stats.put("totalTokens", totalTokens);

        long fullCount = results.stream()
                .filter(r -> r.getResultType() == SearchResult.SearchResultType.FULL)
                .count();
        stats.put("fullDocuments", fullCount);

        long summaryCount = results.stream()
                .filter(r -> r.getResultType() == SearchResult.SearchResultType.SUMMARY)
                .count();
        stats.put("summaryDocuments", summaryCount);

        long snippetCount = results.stream()
                .filter(r -> r.getResultType() == SearchResult.SearchResultType.SNIPPET)
                .count();
        stats.put("snippetDocuments", snippetCount);

        double avgSimilarity = results.stream()
                .mapToDouble(SearchResult::getSimilarity)
                .average()
                .orElse(0.0);
        stats.put("averageSimilarity", String.format("%.2f", avgSimilarity));

        return stats;
    }
}
