package com.agent.knowledge.service;

import com.agent.knowledge.model.Document;
import com.agent.knowledge.model.TextVector;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库管理器
 * 负责文档的存储、检索和相似度搜索
 */
@Slf4j
@Component
public class KnowledgeBaseManager {

    @Value("${agent.knowledge.storage-path:./data/knowledge}")
    private String storagePath;

    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 内存中的文档和向量索引
    private final Map<String, Document> documentIndex = new HashMap<>();
    private final Map<String, TextVector> vectorIndex = new HashMap<>();

    public KnowledgeBaseManager(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    /**
     * 初始化知识库
     */
    public void init() {
        File dir = new File(storagePath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                log.info("✅ Knowledge base storage directory created: {}", storagePath);
            }
        }
        loadAllDocuments();
    }

    /**
     * 添加文档到知识库
     */
    public void addDocument(Document document) {
        if (document.getDocId() == null) {
            document.setDocId(UUID.randomUUID().toString());
        }
        document.setUpdatedTime(System.currentTimeMillis());

        // 保存到内存
        documentIndex.put(document.getDocId(), document);

        // 生成向量并保存
        TextVector vector = embeddingService.embed(document.getContent());
        vectorIndex.put(document.getDocId(), vector);

        // 保存到文件
        saveDocument(document);

        log.info("📄 Document added: {} ({})", document.getTitle(), document.getDocId());
    }

    /**
     * 删除文档
     */
    public void removeDocument(String docId) {
        documentIndex.remove(docId);
        vectorIndex.remove(docId);

        File file = new File(storagePath, docId + ".json");
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                log.info("🗑️ Document deleted: {}", docId);
            }
        }
    }

    /**
     * 获取文档
     */
    public Document getDocument(String docId) {
        return documentIndex.get(docId);
    }

    /**
     * 获取所有文档
     */
    public List<Document> getAllDocuments() {
        return new ArrayList<>(documentIndex.values());
    }

    /**
     * 按类别获取文档
     */
    public List<Document> getDocumentsByCategory(String category) {
        return documentIndex.values().stream()
                .filter(doc -> category.equals(doc.getCategory()))
                .collect(Collectors.toList());
    }

    /**
     * 语义搜索 - 找到与查询最相似的文档
     * 
     * @param query 查询文本
     * @param topK  返回前K个最相似的文档
     * @return 相似度排序的文档列表
     */
    public List<SearchResult> semanticSearch(String query, int topK) {
        if (documentIndex.isEmpty()) {
            log.warn("⚠️ Knowledge base is empty");
            return Collections.emptyList();
        }

        // 生成查询向量
        TextVector queryVector = embeddingService.embed(query);

        // 计算与所有文档的相似度
        List<SearchResult> results = new ArrayList<>();
        for (Map.Entry<String, TextVector> entry : vectorIndex.entrySet()) {
            String docId = entry.getKey();
            TextVector docVector = entry.getValue();

            double similarity = queryVector.cosineSimilarity(docVector);
            Document doc = documentIndex.get(docId);

            results.add(SearchResult.builder()
                    .docId(docId)
                    .title(doc.getTitle())
                    .content(doc.getContent())
                    .category(doc.getCategory())
                    .similarity(similarity)
                    .summary(doc.getSummary())
                    .build());
        }

        // 按相似度排序并返回前K个
        return results.stream()
                .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 全文搜索 - 按关键字搜索
     */
    public List<Document> keywordSearch(String keyword) {
        String lower = keyword.toLowerCase();
        return documentIndex.values().stream()
                .filter(doc -> doc.getContent().toLowerCase().contains(lower) ||
                        doc.getTitle().toLowerCase().contains(lower))
                .collect(Collectors.toList());
    }

    /**
     * 清空所有文档
     */
    public void clearAll() {
        documentIndex.clear();
        vectorIndex.clear();

        // 删除所有文件
        File dir = new File(storagePath);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".json")) {
                        file.delete();
                    }
                }
            }
        }
        log.info("🧹 Knowledge base cleared");
    }

    /**
     * 获取统计信息
     */
    public KnowledgeBaseStats getStats() {
        int totalDocs = documentIndex.size();
        int totalChars = documentIndex.values().stream()
                .mapToInt(doc -> doc.getContent().length())
                .sum();

        Set<String> categories = documentIndex.values().stream()
                .map(Document::getCategory)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return KnowledgeBaseStats.builder()
                .totalDocuments(totalDocs)
                .totalCharacters(totalChars)
                .categories(categories.size())
                .vectorDimension(embeddingService.getVectorDimension())
                .storagePath(storagePath)
                .build();
    }

    // ===== 私有方法 =====

    private void saveDocument(Document document) {
        try {
            File file = new File(storagePath, document.getDocId() + ".json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, document);
            log.debug("💾 Document saved: {}", document.getDocId());
        } catch (IOException e) {
            log.error("❌ Failed to save document: {}", document.getDocId(), e);
        }
    }

    private void loadAllDocuments() {
        File dir = new File(storagePath);
        if (!dir.exists()) {
            return;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                Document doc = objectMapper.readValue(file, Document.class);
                documentIndex.put(doc.getDocId(), doc);

                // 重新生成向量
                TextVector vector = embeddingService.embed(doc.getContent());
                vectorIndex.put(doc.getDocId(), vector);
            } catch (IOException e) {
                log.error("Failed to load document from file: {}", file.getName(), e);
            }
        }

        log.info("✅ Loaded {} documents from storage", documentIndex.size());
    }

    // ===== 内部类 =====

    @lombok.Data
    @lombok.Builder
    public static class SearchResult {
        private String docId;
        private String title;
        private String content;
        private String category;
        private double similarity;
        private String summary;
    }

    @lombok.Data
    @lombok.Builder
    public static class KnowledgeBaseStats {
        private int totalDocuments;
        private int totalCharacters;
        private int categories;
        private int vectorDimension;
        private String storagePath;
    }
}
