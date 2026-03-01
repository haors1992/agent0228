package com.agent.controller;

import com.agent.knowledge.model.Document;
import com.agent.knowledge.service.KnowledgeBaseManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库管理 API
 * 支持文档的增删查改和语义搜索
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeBaseController {

    private final KnowledgeBaseManager knowledgeBaseManager;

    public KnowledgeBaseController(KnowledgeBaseManager knowledgeBaseManager) {
        this.knowledgeBaseManager = knowledgeBaseManager;
    }

    /**
     * 添加文档到知识库
     * 
     * @param request 包含标题、内容、类别等信息
     * @return 添加后的文档信息
     */
    @PostMapping("/documents")
    public ResponseEntity<Map<String, Object>> addDocument(@RequestBody AddDocumentRequest request) {
        try {
            if (request.getTitle() == null || request.getTitle().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Title is required");
                return ResponseEntity.badRequest().body(error);
            }

            if (request.getContent() == null || request.getContent().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Content is required");
                return ResponseEntity.badRequest().body(error);
            }

            Document doc = Document.create(
                    request.getTitle(),
                    request.getContent(),
                    request.getCategory() != null ? request.getCategory() : "general");
            doc.setSource(request.getSource());
            doc.setMetadata(request.getMetadata());

            knowledgeBaseManager.addDocument(doc);

            Map<String, Object> response = new HashMap<>();
            response.put("docId", doc.getDocId());
            response.put("title", doc.getTitle());
            response.put("message", "✅ Document added successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error adding document", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to add document: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 获取所有文档
     */
    @GetMapping("/documents")
    public ResponseEntity<Map<String, Object>> getDocuments(
            @RequestParam(required = false) String category) {
        try {
            List<Document> documents;
            if (category != null && !category.isEmpty()) {
                documents = knowledgeBaseManager.getDocumentsByCategory(category);
            } else {
                documents = knowledgeBaseManager.getAllDocuments();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("total", documents.size());
            response.put("documents", documents);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching documents", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch documents");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 获取单个文档
     */
    @GetMapping("/documents/{docId}")
    public ResponseEntity<Map<String, Object>> getDocument(@PathVariable String docId) {
        try {
            Document doc = knowledgeBaseManager.getDocument(docId);
            if (doc == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("document", doc);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching document", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch document");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/documents/{docId}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable String docId) {
        try {
            if (knowledgeBaseManager.getDocument(docId) == null) {
                return ResponseEntity.notFound().build();
            }

            knowledgeBaseManager.removeDocument(docId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "✅ Document deleted successfully");
            response.put("docId", docId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting document", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to delete document");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 语义搜索 - 使用向量相似度搜索相关文档
     * 
     * @param query 查询文本
     * @param topK  返回前K个结果（默认5）
     * @return 按相似度排序的文档列表
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> semanticSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK) {
        try {
            if (query == null || query.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Query is required");
                return ResponseEntity.badRequest().body(error);
            }

            List<KnowledgeBaseManager.SearchResult> results = knowledgeBaseManager.semanticSearch(query, topK);

            Map<String, Object> response = new HashMap<>();
            response.put("query", query);
            response.put("results", results);
            response.put("count", results.size());

            log.info("🔍 Semantic search for '{}' returned {} results", query, results.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error performing semantic search", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to perform search: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 关键字搜索
     */
    @GetMapping("/search/keyword")
    public ResponseEntity<Map<String, Object>> keywordSearch(@RequestParam String keyword) {
        try {
            if (keyword == null || keyword.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Keyword is required");
                return ResponseEntity.badRequest().body(error);
            }

            List<Document> results = knowledgeBaseManager.keywordSearch(keyword);

            Map<String, Object> response = new HashMap<>();
            response.put("keyword", keyword);
            response.put("results", results);
            response.put("count", results.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error performing keyword search", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to perform search");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 清空知识库（谨慎使用）
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearKnowledgeBase() {
        try {
            knowledgeBaseManager.clearAll();
            Map<String, Object> response = new HashMap<>();
            response.put("message", "✅ Knowledge base cleared");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error clearing knowledge base", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to clear knowledge base");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 获取知识库统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            KnowledgeBaseManager.KnowledgeBaseStats stats = knowledgeBaseManager.getStats();

            Map<String, Object> response = new HashMap<>();
            response.put("stats", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting knowledge base stats", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get stats");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // ===== 请求对象 =====

    public static class AddDocumentRequest {
        private String title;
        private String content;
        private String category;
        private String source;
        private String metadata;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getMetadata() {
            return metadata;
        }

        public void setMetadata(String metadata) {
            this.metadata = metadata;
        }
    }
}
