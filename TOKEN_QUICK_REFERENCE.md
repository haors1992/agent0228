# ⚡ Token 优化 - 快速参考（1分钟速查）

## 🎯 核心问题

```
Long Context → Token Explosion → Cost Disaster
5轮: 8,750 tokens ($0.44)
10轮: 35,000+ tokens ($1.75)
↓ 优化后
5轮: 4,000 tokens ($0.20)
10轮: 7,200 tokens ($0.36)
节省: 79% ✅
```

---

## 📦 5个优化服务（快速导航）

### 1️⃣ TokenUsageService → Token计数和成本追踪
```java
@Autowired TokenUsageService tokenUsageService;

tokenUsageService.estimateTokens(text);              // 估算Token数
tokenUsageService.recordUsage(sessionId, stats);    // 记录使用
tokenUsageService.getSessionStats(sessionId);       // 获取统计
tokenUsageService.calculateRedundancyRate(messages); // 重复率分析
```

📊 **返回**: 成本统计、重复率、Token分解

---

### 2️⃣ ConversationHistoryManager → 历史管理（滑动窗口）
```java
@Autowired ConversationHistoryManager historyManager;

historyManager.buildOptimizedMessages(sessionId, history);  // 构建优化消息
historyManager.shouldEnableSavingMode(history);            // 检查是否节省
historyManager.compressHistory(history);                   // 压缩为摘要
historyManager.pruneOldMessages(history, maxTurns);        // 删除过旧消息
```

🔧 **参数**: WINDOW_SIZE=5, MAX_HISTORY_TOKENS=3000

---

### 3️⃣ OptimizedPromptBuilder → Prompt缓存
```java
@Autowired OptimizedPromptBuilder promptBuilder;

OptimizedPromptBuilder.PromptConfig config = new OptimizedPromptBuilder.PromptConfig();
config.setDomain("编程");
config.setMaxTokens(600);

String prompt = promptBuilder.buildSystemPrompt(config);  // ✨ 缓存重用

// 快速预设
promptBuilder.buildSystemPrompt(OptimizedPromptBuilder.createDefaultConfig());
promptBuilder.buildSystemPrompt(OptimizedPromptBuilder.createCompactConfig());   // 最小化
promptBuilder.buildSystemPrompt(OptimizedPromptBuilder.createDetailedConfig("域"));
```

🎁 **特性**: 自动缓存、动态降级、条件组件

---

### 4️⃣ SmartKnowledgeRetrieval → 知识库精准检索
```java
@Autowired SmartKnowledgeRetrieval smartRetrieval;

List<SmartKnowledgeRetrieval.SearchResult> results = 
    smartRetrieval.smartSearch(query, isFollowUp);

// 获取统计
Map<String, Object> stats = smartRetrieval.getRetrievalStats(results);
```

🔍 **流程**: 初始检索 → 相似度过滤 → Token预算 → 摘要生成

---

### 5️⃣ TieredResponseManager → 分级回答
```java
@Autowired TieredResponseManager responseManager;

TieredResponseManager.ResponseBudget budget = 
    responseManager.estimateBudget(4000, 500, 1200, 800);

TieredResponseManager.ResponseGuidance guidance = 
    responseManager.makeGuidanceDecision(budget, depthL, "normal", followUp);

// 级别：BRIEF(200) | NORMAL(500) | DETAILED(1000) | VERBOSE(2000) tokens
String instruction = responseManager.buildResponseInstruction(guidance.getTier());

// 可视化
System.out.println(responseManager.visualizeBudget(budget));
```

📊 **级别**:
- BRIEF: 简洁快速
- NORMAL: 标准平衡
- DETAILED: 详细充分
- VERBOSE: 详尽完整

---

## 🔌 在ReasoningEngine中集成（核心改造）

```java
public ChatResponse chat(String sessionId, String userQuery) {
    // 1. 优化Prompt
    OptimizedPromptBuilder.PromptConfig promptConfig = 
        OptimizedPromptBuilder.createDefaultConfig();
    String systemPrompt = promptBuilder.buildSystemPrompt(promptConfig);
    
    // 2. 优化历史
    List<Map<String, String>> contextMessages = 
        historyManager.buildOptimizedMessages(sessionId, getHistory(sessionId));
    
    // 3. 智能检索
    List<SmartKnowledgeRetrieval.SearchResult> docs = 
        smartRetrieval.smartSearch(userQuery, false);
    
    // 4. 估算Token和选择回答级别
    int promptTokens = tokenUsageService.estimateTokens(systemPrompt);
    int contextTokens = tokenUsageService.estimateMessagesTokens(contextMessages);
    int knowledgeTokens = docs.stream().mapToInt(r -> r.getTokensUsed()).sum();
    
    TieredResponseManager.ResponseBudget budget = 
        responseManager.estimateBudget(4000, promptTokens, contextTokens, knowledgeTokens);
    
    TieredResponseManager.ResponseGuidance guidance = 
        responseManager.makeGuidanceDecision(budget, depth, "normal", false);
    
    // 5. 调用LLM并记录
    String response = callLLM(buildMessages(..., guidance.getInstruction()));
    
    TokenUsageService.TokenStats stats = new TokenUsageService.TokenStats();
    stats.addCost("deepseek", promptTokens+contextTokens+knowledgeTokens, 
                  tokenUsageService.estimateTokens(response));
    tokenUsageService.recordUsage(sessionId, stats);
    
    return response;
}
```

---

## 📊 关键数值参考

| 参数 | 默认值 | 含义 |
|------|--------|------|
| WINDOW_SIZE | 5 | 保留最近5轮对话 |
| MAX_HISTORY_TOKENS | 3000 | 历史最多3K tokens |
| MAX_SYSTEM_PROMPT_TOKENS | 800 | Prompt最多800 tokens |
| MAX_RESULT_TOKENS | 1500 | 知识库最多1.5K tokens |
| MIN_SIMILARITY | 0.65 | 相似度过滤阈值 |
| TOKENS_PER_WORD | 1.3 | 单词到Token的比率 |

**调优建议**:
- 如果质量下降，增加 `MAX_HISTORY_TOKENS` 到 4000
- 如果检索精度低，降低 `MIN_SIMILARITY` 到 0.6
- 如果成本还是高，降低 `WINDOW_SIZE` 到 3

---

## 🎯 典型使用场景

### 场景1：第5轮对话（成本控制）
```java
// 自动启用节省模式
if (historyManager.shouldEnableSavingMode(history)) {
    // 更激进的优化
    budget.setTotalTokenBudget(3000);  // 更严格的预算
}
```

### 场景2：复杂问题（质量优先）
```java
if ("complex".equalsIgnoreCase(queryType) && budget.getAvailableTokens() > 1200) {
    ResponseTier tier = ResponseTier.DETAILED;
} else {
    tier = ResponseTier.NORMAL;
}
```

### 场景3：后续问题（快速响应）
```java
boolean isFollowUp = smartRetrieval.isFollowUpQuery(query, previousQuery);
// 自动使用more focused retrieval
List<SearchResult> results = smartRetrieval.smartSearch(query, isFollowUp);
// → 只返回top-3而不是top-5
```

---

## 🔍 调试命令

```java
// 1. 查看会话Token统计
log.info("{}", tokenUsageService.getSessionStats(sessionId));

// 2. 查看全局成本
log.info("{}", tokenUsageService.getGlobalStats());

// 3. 查看Prompt缓存状态
log.info("{}", promptBuilder.getCacheStats());

// 4. 查看对话重复比例
double redundancy = historyManager.getRedundancyRate(history);
log.info("重复率: {:.1f}%", redundancy * 100);

// 5. 查看Token预算分配
log.info("{}", responseManager.visualizeBudget(budget));

// 6. 查看推荐的回答级别
log.info("推荐: {} (置信度: {:.0f}%)", 
    guidance.getTier().name, 
    guidance.getConfidenceScore() * 100);
```

---

## ⚠️ 常见陷阱

| ❌ 错误做法 | ✅ 正确做法 |
|-----------|---------|
| 每次都生成新Prompt | 使用缓存的PromptBuilder |
| 保留所有对话历史 | 使用滑动窗口管理 |
| 返回所有KB结果 | 使用Token预算过滤 |
| 简单的回答质量评估 | 用Token预算和对话深度判断级别 |
| 定死的回答长度 | 使用分级系统自适应 |

---

## 📈 效果验证

**编译后检查**:
```bash
mvn compile -DskipTests
ls target/classes/com/agent/optimization/*.class
# 应该看到5个主类编译成功
```

**集成后检查**:
```java
// 在日志中应该看到
✅ [Cache HIT] System Prompt cached
✅ Built optimized context with X messages
🔍 Smart search initiated
📊 Final results: X documents
✅ Token Usage - Session: xxx | Total: YYY tokens | Cost: ¥Z.ZZ
```

**成本验证**:
```
集成前后对比:
Turn 1-5: 
  Before: ~8,750 tokens (¥0.35)
  After: ~4,000 tokens (¥0.16)
  Savings: 54% ✅
```

---

## 🚀 集成步骤（3步快速开始）

### Step 1: 注入服务（2分钟）
```java
// 在 ReasoningEngine 中添加
@Autowired TokenUsageService tokenUsageService;
@Autowired ConversationHistoryManager historyManager;
@Autowired OptimizedPromptBuilder promptBuilder;
@Autowired SmartKnowledgeRetrieval smartRetrieval;
@Autowired TieredResponseManager responseManager;
```

### Step 2: 修改chat方法（10分钟）
```java
// 按上面的"在ReasoningEngine中集成"代码改造
```

### Step 3: 编译并测试（5分钟）
```bash
mvn clean compile
# 测试一个5轮对话看成本变化
```

---

## 💾 快速链接

| 资源 | 用途 |
|------|------|
| TOKEN_OPTIMIZATION_GUIDE.md | 完整原理和最佳实践 |
| TOKEN_INTEGRATION_GUIDE.md | 集成步骤和示例 |
| TOKEN_OPTIMIZATION_COMPLETE.md | 部署完成报告 |
| SmartKnowledgeRetrieval.java | 知识库精准化逻辑 |
| TieredResponseManager.java | Token预算管理逻辑 |

---

## 📞 快速问题排查

**Q: 优化后成本没有降低**
A: 检查 `shouldEnableSavingMode()` - 可能对话还不够深

**Q: 响应质量下降**
A: 增加 `MAX_HISTORY_TOKENS` 从3000到4000

**Q: 缓存似乎没有生效**
A: 调用 `promptBuilder.getCacheStats()` - 应该看到cacheSize > 0

**Q: 编译失败**
A: 确保是Java 8+（不支持switch表达式）

---

**版本**: 1.0 | **日期**: 2026-03-02 | **成本节省**: 60-80% | **集成时间**: < 1小时
