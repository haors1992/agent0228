# 🔗 SmartKnowledgeRetrieval 完整集成指南

## 修复内容回顾

SmartKnowledgeRetrieval 已完全修复并连接到真实的 KnowledgeBaseManager。现在需要在 ReasoningEngine 中正确使用它。

---

## 集成步骤

### 步骤1：在ReasoningEngine中注入服务

**文件**: `src/main/java/com/agent/reasoning/engine/ReasoningEngine.java`

```java
@Service
@Slf4j
public class ReasoningEngine {
    
    // 原有的注入
    private final ChatService chatService;
    private final KnowledgeBaseManager knowledgeBaseManager;
    
    // ✅ 添加新的优化服务注入
    @Autowired(required = false)
    private SmartKnowledgeRetrieval smartRetrieval;
    
    @Autowired(required = false)
    private TokenUsageService tokenUsageService;
    
    @Autowired(required = false)
    private ConversationHistoryManager historyManager;
    
    @Autowired(required = false)
    private OptimizedPromptBuilder promptBuilder;
    
    @Autowired(required = false)
    private TieredResponseManager responseManager;
}
```

---

### 步骤2：修改chat()方法使用SmartKnowledgeRetrieval

**找到现有的知识库查询代码**:

```java
// ❌ 旧方式：直接调用KnowledgeBaseManager
if (knowledgeBaseManager != null && knowledgeBaseManager.getStats().getTotalDocuments() > 0) {
    List<KnowledgeBaseManager.SearchResult> knowledgeResults = 
        knowledgeBaseManager.semanticSearch(userQuery, knowledgeTopK);
    
    for (KnowledgeBaseManager.SearchResult result : knowledgeResults) {
        // ... 处理结果
    }
}
```

**改为新方式**:

```java
// ✅ 新方式：使用SmartKnowledgeRetrieval
if (knowledgeBaseManager != null && knowledgeBaseManager.getStats().getTotalDocuments() > 0) {
    
    // 检测是否是后续问题
    boolean isFollowUp = false;
    if (conversationHistory != null && conversationHistory.size() > 1) {
        // 后续问题通常更简短
        isFollowUp = userQuery.length() < 50;
    }
    
    // 使用优化的智能检索
    List<SmartKnowledgeRetrieval.SearchResult> searchResults = 
        smartRetrieval.smartSearch(userQuery, isFollowUp);
    
    log.info("📚 Knowledge retrieval: {} results found", searchResults.size());
    
    // 添加到消息上下文
    for (SmartKnowledgeRetrieval.SearchResult result : searchResults) {
        String kbContext = String.format(
            "【知识库】%s\n内容：%s\n(匹配度: %.2f)",
            result.getTitle(),
            result.getContent(),
            result.getSimilarity() * 100
        );
        systemMessages.add(createMessage("assistant", kbContext));
    }
}
```

---

### 步骤3：完整的改造示例

这是一个完整的 chat() 方法示例，展示如何集成所有优化服务：

```java
public ChatResponse chat(ChatRequest request) throws Exception {
    String sessionId = request.getSessionId();
    String userQuery = request.getUserQuery();
    
    log.info("💬 Chat request: session={}, query={}", sessionId, userQuery);
    
    // ==================== 1. 获取对话历史 ====================
    List<ConversationHistoryManager.ConversationTurn> fullHistory = 
        getOrCreateHistory(sessionId);
    
    // ==================== 2. 准备消息列表 ====================
    List<Map<String, String>> messages = new ArrayList<>();
    
    // 2.1 优化System Prompt
    OptimizedPromptBuilder.PromptConfig promptConfig = 
        OptimizedPromptBuilder.createDefaultConfig();
    promptConfig.setDomain("通用");
    promptConfig.setMaxTokens(600);
    
    String systemPrompt = (promptBuilder != null) ? 
        promptBuilder.buildSystemPrompt(promptConfig) : 
        getDefaultSystemPrompt();
    
    messages.add(createMessage("system", systemPrompt));
    
    // 2.2 优化对话历史（滑动窗口）
    if (historyManager != null && !fullHistory.isEmpty()) {
        List<Map<String, String>> contextMessages = 
            historyManager.buildOptimizedMessages(sessionId, fullHistory);
        messages.addAll(contextMessages);
    } else {
        // fallback：使用原始历史
        for (ConversationHistoryManager.ConversationTurn turn : fullHistory) {
            messages.add(createMessage(turn.getRole(), turn.getContent()));
        }
    }
    
    // 2.3 智能知识库检索
    List<Map<String, String>> kbMessages = new ArrayList<>();
    
    if (knowledgeBaseManager != null && 
        knowledgeBaseManager.getStats().getTotalDocuments() > 0 &&
        smartRetrieval != null) {
        
        // 检测是否是后续问题
        boolean isFollowUp = (fullHistory.size() > 1 && userQuery.length() < 50);
        
        // 执行智能搜索
        List<SmartKnowledgeRetrieval.SearchResult> kbResults = 
            smartRetrieval.smartSearch(userQuery, isFollowUp);
        
        log.info("📚 Found {} knowledge base results", kbResults.size());
        
        // 添加知识库结果
        for (SmartKnowledgeRetrieval.SearchResult result : kbResults) {
            String kbContent = String.format(
                "【相关知识】\n标题：%s\n内容：%s\n相关度：%.1f%%",
                result.getTitle(),
                result.getContent(),
                result.getSimilarity() * 100
            );
            kbMessages.add(createMessage("assistant", kbContent));
        }
    }
    
    // 只添加有内容的知识库消息
    if (!kbMessages.isEmpty()) {
        messages.addAll(kbMessages);
    }
    
    // 2.4 添加用户查询
    messages.add(createMessage("user", userQuery));
    
    // ==================== 3. Token计数和预算管理 ====================
    int promptTokens = 0;
    int contextTokens = 0;
    int knowledgeTokens = 0;
    
    if (tokenUsageService != null) {
        promptTokens = tokenUsageService.estimateTokens(systemPrompt);
        contextTokens = tokenUsageService.estimateTokens(
            messages.stream()
                .filter(m -> !"system".equals(m.get("role")))
                .map(m -> m.get("content"))
                .toString()
        );
        knowledgeTokens = kbMessages.stream()
            .mapToInt(m -> tokenUsageService.estimateTokens(m.get("content")))
            .sum();
        
        log.info("📊 Token breakdown - Prompt: {}, Context: {}, Knowledge: {}",
            promptTokens, contextTokens, knowledgeTokens);
    }
    
    // 3.2 选择响应级别
    String responseInstruction = "";
    if (responseManager != null && tokenUsageService != null) {
        TieredResponseManager.ResponseBudget budget = 
            responseManager.estimateBudget(
                4000,  // 总预算
                promptTokens,
                contextTokens, 
                knowledgeTokens
            );
        
        TieredResponseManager.ResponseGuidance guidance = 
            responseManager.makeGuidanceDecision(
                budget, 
                fullHistory.size(),
                "normal",
                false
            );
        
        responseInstruction = guidance.getInstruction();
        log.info("🎯 Response tier: {} (confidence: {:.0f}%)", 
            guidance.getTier().name,
            guidance.getConfidenceScore() * 100);
    }
    
    if (!responseInstruction.isEmpty()) {
        messages.add(createMessage("system", responseInstruction));
    }
    
    // ==================== 4. 调用LLM ====================
    log.info("🚀 Calling LLM with {} messages", messages.size());
    
    String response = chatService.chat(messages);
    
    // ==================== 5. 记录Token使用 ====================
    if (tokenUsageService != null) {
        TokenUsageService.TokenStats stats = new TokenUsageService.TokenStats();
        stats.addCost("deepseek", 
            promptTokens + contextTokens + knowledgeTokens,
            tokenUsageService.estimateTokens(response)
        );
        tokenUsageService.recordUsage(sessionId, stats);
        
        log.info("💰 Session {} Token cost: {}", sessionId, stats.getCostCNY());
    }
    
    // ==================== 6. 保存对话历史 ====================
    fullHistory.add(new ConversationHistoryManager.ConversationTurn("user", userQuery));
    fullHistory.add(new ConversationHistoryManager.ConversationTurn("assistant", response));
    
    return ChatResponse.builder()
        .sessionId(sessionId)
        .response(response)
        .build();
}
```

---

## 验证集成

### 编译检查

```bash
# 清理旧编译
mvn clean

# 重新编译
mvn compile

# 应该看到: BUILD SUCCESS
```

### 日志验证

运行应用时，应该看到这样的日志：

```
✅ 启动
KnowledgeBaseManager: ✅ Loaded 5 documents from storage
SmartKnowledgeRetrieval initialized

🔍 Chat request: session=test, query=Python并发编程
📚 Found 3 knowledge base results
📊 Token breakdown - Prompt: 450, Context: 800, Knowledge: 600
🎯 Response tier: DETAILED (confidence: 85%)
🚀 Calling LLM with 10 messages
✅ Converted 3 KB results to Document objects
📊 Phase 1 - Retrieved 3 candidates
✅ Final results: 2 documents, 1200 tokens used
💰 Session test Token cost: ¥0.025
```

### 功能验证

```bash
# 1. 启动应用
mvn spring-boot:run &

# 2. 添加测试文档
curl -X POST http://localhost:8080/api/knowledge/documents \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Python并发编程",
    "content": "Python中常见的并发编程方法...",
    "category": "编程"
  }'

# 3. 进行Chat查询
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"test","userQuery":"Python并发编程的方法有哪些?"}'

# 4. 观察日志
tail -f nohup.out | grep -E "Smart|Found|Token|Response|Final"
```

---

## 故障排除

### 问题1：SmartKnowledgeRetrieval为null

**症状**: 日志中没有"Smart search"相关消息

**原因**: 不是错误！如果SmartKnowledgeRetrieval未注入，系统会fallback到原始知识库查询

**验证**:
```java
if (smartRetrieval == null) {
    log.warn("⚠️ SmartKnowledgeRetrieval not available, using standard KB query");
    // fallback逻辑
}
```

### 问题2：知识库为空

**症状**: 
```
📚 Found 0 knowledge base results
```

**解决**: 添加文档到知识库（见上面的验证步骤）

### 问题3：Token计数显示为0

**原因**: TokenUsageService未注入

**解决**: 确保已在ReasoningEngine中添加注入

---

## 部署清单

- [ ] SmartKnowledgeRetrieval已修复（✅ 已完成）
- [ ] 编译成功（✅ 已验证）
- [ ] ReasoningEngine注入了所有优化服务
- [ ] chat()方法已改造使用SmartKnowledgeRetrieval
- [ ] 知识库中已添加相关文档
- [ ] 启动应用进行端到端测试
- [ ] 观察日志确认各组件正常工作

---

## 快速测试脚本

```bash
#!/bin/bash

echo "🔄 编译..."
mvn clean compile -q

echo "🚀 启动应用..."
mvn spring-boot:run > /tmp/app.log 2>&1 &
APP_PID=$!
sleep 30

echo "📚 添加test文档..."
curl -s -X POST http://localhost:8080/api/knowledge/documents \
  -H "Content-Type: application/json" \
  -d '{
    "title": "分布式系统设计",
    "content": "分布式系统涉及多个计算机节点通过网络协调...",
    "category": "系统设计"
  }' > /dev/null

echo "💬 测试查询..."
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "test",
    "userQuery": "分布式系统的设计原则"
  }' | jq .

echo "📊 查看日志..."
grep -i "smart\|found\|token" /tmp/app.log | tail -10

echo "✅ 测试完成"
kill $APP_PID
```

---

**版本**: 1.1 (修复后)  
**编译状态**: ✅ SUCCESS  
**集成状态**: 就绪  
**日期**: 2026-03-02
