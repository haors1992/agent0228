# 🚀 Token 优化系统 - 集成指南

## 📦 已创建的组件清单

```
src/main/java/com/agent/optimization/
├── TokenUsageService.java              ✅ Token计数和成本追踪
├── ConversationHistoryManager.java     ✅ 対话历史管理（滑动窗口）
├── OptimizedPromptBuilder.java         ✅ Prompt构建和缓存
├── SmartKnowledgeRetrieval.java        ✅ 智能知识库检索
└── TieredResponseManager.java          ✅ 分级响应管理
```

## 🔧 集成步骤

### ✅ 步骤1：修改 ReasoningEngine.java

**位置**: `src/main/java/com/agent/reasoning/ReasoningEngine.java`

**修改内容**：在类中注入优化服务，然后改进消息构建

```java
// 在 ReasoningEngine 类顶部添加
@Autowired
private TokenUsageService tokenUsageService;

@Autowired
private ConversationHistoryManager historyManager;

@Autowired
private OptimizedPromptBuilder promptBuilder;

@Autowired
private SmartKnowledgeRetrieval smartRetrieval;

@Autowired
private TieredResponseManager responseManager;

// ============================================

// 修改 chat() 方法的消息构建逻辑
public ChatResponse chat(String sessionId, String userQuery) {
    
    // 1️⃣ 优化 System Prompt
    OptimizedPromptBuilder.PromptConfig promptConfig = 
        OptimizedPromptBuilder.createDefaultConfig();
    promptConfig.setDomain("通用");
    String systemPrompt = promptBuilder.buildSystemPrompt(promptConfig);
    
    List<Map<String, String>> messages = new ArrayList<>();
    messages.add(createMessage("system", systemPrompt));
    
    // 2️⃣ 优化对话历史（滑动窗口）
    List<ConversationHistoryManager.ConversationTurn> history = 
        getConversationHistory(sessionId);
    List<Map<String, String>> contextMessages = 
        historyManager.buildOptimizedMessages(sessionId, history);
    messages.addAll(contextMessages);
    
    // 3️⃣ 优化知识库检索
    List<SmartKnowledgeRetrieval.SearchResult> results = 
        smartRetrieval.smartSearch(userQuery, 
            historyManager.shouldEnableSavingMode(history));
    
    // 添加知识库结果
    for (SmartKnowledgeRetrieval.SearchResult result : results) {
        messages.add(createMessage("assistant", 
            "【知识库】" + result.getTitle() + "\n" + result.getContent()));
    }
    
    // 添加用户查询
    messages.add(createMessage("user", userQuery));
    
    // 4️⃣ 估算Token成本并选择响应级别
    int promptTokens = tokenUsageService.estimateTokens(systemPrompt);
    int contextTokens = tokenUsageService.estimateMessagesTokens(contextMessages);
    int knowledgeTokens = results.stream()
        .mapToInt(r -> r.getTokensUsed())
        .sum();
    
    TieredResponseManager.ResponseBudget budget = 
        responseManager.estimateBudget(4000, promptTokens, contextTokens, knowledgeTokens);
    
    TieredResponseManager.ResponseGuidance guidance = 
        responseManager.makeGuidanceDecision(budget, history.size(), "normal", false);
    
    // 添加响应指导
    messages.add(createMessage("system", guidance.getInstruction()));
    
    // 5️⃣ LLM调用
    String response = callLLM(messages);
    
    // 6️⃣ 记录Token使用
    TokenUsageService.TokenStats stats = new TokenUsageService.TokenStats();
    stats.addCost("deepseek", promptTokens + contextTokens + knowledgeTokens, 
                  tokenUsageService.estimateTokens(response));
    tokenUsageService.recordUsage(sessionId, stats);
    
    return ChatResponse.builder()
        .sessionId(sessionId)
        .response(response)
        .tokensUsed(stats.getTotalTokens())
        .build();
}
```

---

## 📊 使用示例

### 示例1：获取会话的Token统计

```java
// 在你的Controller中
@GetMapping("/api/session/{sessionId}/token-stats")
public ResponseEntity<?> getTokenStats(@PathVariable String sessionId) {
    Map<String, Object> stats = tokenUsageService.getSessionStats(sessionId);
    return ResponseEntity.ok(stats);
}

// 返回结果：
// {
//   "sessionId": "xxx-xxx",
//   "totalTokens": 8500,
//   "totalCostUSD": "$0.0425",
//   "totalCostCNY": "¥0.30",
//   "averageTokensPerTurn": 850,
//   "turnCount": 10
// }
```

### 示例2：检查对话历史是否应启用节省模式

```java
List<ConversationHistoryManager.ConversationTurn> history = getHistory(sessionId);

if (historyManager.shouldEnableSavingMode(history)) {
    log.warn("⚠️ 启用Token节省模式 - 对话过深或Token过多");
    // ... 后续使用更激进的优化
}
```

### 示例3：查看Token预算分配

```java
TieredResponseManager.ResponseBudget budget = 
    responseManager.estimateBudget(4000, 500, 1200, 800);

// 可视化显示
System.out.println(responseManager.visualizeBudget(budget));

// 输出：
// ╔════════════════════════════════════════╗
// ║       Token 预算分配可视化              ║
// ├────────────────────────────────────────┤
// ║ Prompt:    ████████  500/4000 (12.5%) ║
// ║ Context:   ██████████████  1200/4000 (30.0%) ║
// ║ Knowledge: ██████  800/4000 (20.0%) ║
// ║ Available: ███████████  1500/4000 (37.5%) ║
// ╚════════════════════════════════════════╝
```

### 示例4：根据Token情况选择响应级别

```java
TieredResponseManager.ResponseBudget budget = calculateBudget();
TieredResponseManager.ResponseGuidance guidance = 
    responseManager.makeGuidanceDecision(budget, 5, "complex", false);

log.info("推荐级别: {}", guidance.getTier().name); // "DETAILED"
log.info("给LLM的指导:\n{}", guidance.getInstruction());
log.info("置信度: {:.0f}%", guidance.getConfidenceScore() * 100);
```

---

## 🎯 快速改造检查清单

- [ ] **第1阶段：基础集成** (30分钟)
  - [ ] 在ReasoningEngine中注入5个优化服务
  - [ ] 修改chat()方法的消息构建逻辑
  - [ ] 添加TokenStats记录
  - [ ] 编译并运行测试

- [ ] **第2阶段：功能验证** (20分钟)
  - [ ] 测试单个会话的Token统计
  - [ ] 验证历史缩减功能
  - [ ] 检查Prompt缓存是否有效
  - [ ] 观察知识库精确检索效果

- [ ] **第3阶段：监控集成** (15分钟)
  - [ ] 修改MetricsCollector记录Token指标
  - [ ] 添加Token成本监控API
  - [ ] 在监控仪表板展示Token趋势

- [ ] **第4阶段：性能优化** (后续)
  - [ ] 调整各种Token预算参数
  - [ ] 测试不同的历史窗口大小
  - [ ] 优化知识库检索精度

---

## 📈 预期效果

集成完成后，应该看到：

```
【优化前的典型会话】
Turn 1: Input ~500 tokens  → Output 200 tokens  = 700 tokens 成本
Turn 2: Input ~1000 tokens → Output 250 tokens  = 1250 tokens 成本 (累积1950)
Turn 3: Input ~1500 tokens → Output 300 tokens  = 1800 tokens 成本 (累积3750)
Turn 5: Input ~2500 tokens → Output 400 tokens  = 2900 tokens 成本 (累积8000+)
成本: ¥0.56

【优化后的同样会话】
Turn 1: Input ~500 tokens  → Output 200 tokens  = 700 tokens 成本
Turn 2: Input ~700 tokens  → Output 250 tokens  = 950 tokens 成本 (累积1650)
Turn 3: Input ~650 tokens  → Output 300 tokens  = 950 tokens 成本 (累积2600)
Turn 5: Input ~600 tokens  → Output 400 tokens  = 1000 tokens 成本 (累积4000)
成本: ¥0.28

节省: 50% ✅
```

---

## 🐛 故障排除

### 问题1：优化后响应质量下降

**原因**：Token预算过紧，导致信息丢失过多

**解决**：
```java
// 提高Token预算
MAX_HISTORY_TOKENS = 4000;  // 从3000增加到4000
MAX_RESULT_TOKENS = 2000;   // 从1500增加到2000
```

### 问题2：缓存未生效

**原因**：Prompt配置不一致导致缓存键不同

**调试**：
```java
// 打印缓存统计
Map<String, Object> cacheStats = promptBuilder.getCacheStats();
log.info("缓存统计: {}", cacheStats);
// 如果cacheStats.size == 0，说明没有缓存命中
```

### 问题3：历史没有被缩减

**原因**：`shouldEnableSavingMode()` 返回false

**调试**：
```java
boolean savingMode = historyManager.shouldEnableSavingMode(history);
double redundancy = historyManager.getRedundancyRate(history);
log.info("节省模式: {}, 重复率: {:.1f}%", savingMode, redundancy * 100);
```

---

## 📡 监控面板集成

修改 `MetricsCollector.java`：

```java
@Autowired
private TokenUsageService tokenUsageService;

public void recordSessionMetrics(String sessionId) {
    Map<String, Object> stats = tokenUsageService.getSessionStats(sessionId);
    
    // 记录到数据库
    metricsStorage.save(sessionId, "total_tokens", 
        ((Number) stats.get("totalTokens")).intValue());
    
    metricsStorage.save(sessionId, "total_cost_cny",
        Double.parseDouble(((String) stats.get("totalCostCNY")).replace("¥", "")));
}
```

在仪表板显示：
```javascript
// streaming-monitoring.html 中添加
const tokenStats = fetch(`/api/session/${sessionId}/token-stats`)
    .then(r => r.json());

// 显示成本统计
document.getElementById('tokenCost').innerText = 
    tokenStats.totalCostCNY;
```

---

## 🎓 最佳实践

1. **监控Token趋势**
   ```
   每天查看平均Token/轮的趋势
   目标：保持 < 800 tokens/turn
   ```

2. **定期优化参数**
   ```
   每周检查：
   - 历史窗口大小是否合适
   - 知识库检索精度是否足够
   - Prompt是否过于冗长
   ```

3. **用户反馈循环**
   ```
   A/B测试不同的Token预算
   监控用户对回答质量的评价
   找到质量与成本的平衡点
   ```

4. **成本控制**
   ```
   为每个会话设置Token上限
   超出预算时自动触发激进优化
   向用户显示预计成本
   ```

---

## ✅ 验证清单

运行以下测试确保集成成功：

```bash
# 1. 编译检查
mvn clean compile

# 2. Token计数准确性
curl http://localhost:8080/api/token/estimate -d "text=测试文本"

# 3. 会话统计
curl http://localhost:8080/api/session/test-session/token-stats

# 4. 全局统计
curl http://localhost:8080/api/metrics/token/global

# 5. 性能测试
# 进行一个5轮对话，观察Token增长是否线性而非指数
```

---

**版本**: 1.0  
**完成日期**: 2026-03-02  
**预期成本节省**: 60-80%
