# 上下文管理系统 - API 快速参考

## 快速开始 (5 分钟上手)

### 1. 基本使用
```java
@Autowired
private ContextManager contextManager;

// 初始化
contextManager.initializeWithSystemPrompt(systemPrompt);

// 添加消息
contextManager.addUserMessage("用户输入");
contextManager.addAssistantMessage("助手回复");
contextManager.addObservation("观察结果");

// 获取 LLM 消息（自动压缩）
List<Message> messages = contextManager.getMessagesForLLM();

// 监控状态
double usage = contextManager.getTokenBudgetUsage();
log.info(contextManager.getContextSummary());
```

---

## API 细节

### ContextManager

| 方法 | 用途 | 返回值 |
|------|------|--------|
| `initializeWithSystemPrompt(String)` | 初始化系统提示 | void |
| `addUserMessage(String)` | 添加用户消息 | void |
| `addAssistantMessage(String)` | 添加 AI 回复 | void |
| `addObservation(String)` | 添加观察结果 | void |
| `addMessage(String role, String content)` | 通用添加方法 | void |
| `shouldCompress()` | 检查是否需要压缩 | boolean |
| `compress()` | 手动压缩 | void |
| `getMessagesForLLM()` | 获取 LLM 消息（自动压缩） | List\<Message\> |
| `getTokenBudgetUsage()` | 获取 token 使用率 (0-1) | double |
| `getContextSummary()` | 获取状态报告 | String |
| `getLastCompressionResult()` | 获取最后压缩结果 | CompressionResult |

### TokenCounter

| 方法 | 用途 | 返回值 |
|------|------|--------|
| `estimateTokens(String)` | 快速估算 | long |
| `estimateTokensPrecise(String)` | 精确估算 | long |
| `truncateByTokens(String, long)` | 截断到指定 token | String |

### ConversationHistory

| 方法 | 用途 | 返回值 |
|------|------|--------|
| `addMessage(String role, String content)` | 添加消息 | void |
| `getMessageMap()` | 获取消息映射 | Map\<String, ConversationMessage\> |
| `getMessagesForLLM()` | 获取 LLM 格式消息 | List\<Message\> |
| `getTotalTokens()` | 获取总 token | long |
| `getStats()` | 获取统计信息 | HistoryStats |

### HistoryCompressor

| 方法 | 用途 | 返回值 |
|------|------|--------|
| `compress(ConversationHistory)` | 执行压缩 | CompressionResult |

---

## 常见场景

### 场景 1: 长对话管理
```java
for (String userInput : userInputs) {
    contextManager.addUserMessage(userInput);
    
    // 自动管理
    List<Message> msgs = contextManager.getMessagesForLLM();
    ChatResponse response = llmService.chat(msgs);
    
    contextManager.addAssistantMessage(response.getContent());
}

log.info(contextManager.getContextSummary());
```

### 场景 2: 显式压缩控制
```java
// 检查是否需要压缩
if (contextManager.shouldCompress()) {
    log.info("执行压缩...");
    contextManager.compress();
    
    HistoryCompressor.CompressionResult result = 
        contextManager.getLastCompressionResult();
    
    log.info("节省 {} tokens，压缩比 {}", 
        result.getTokensSaved(), 
        String.format("%.1f%%", result.getComressionRatio() * 100));
}
```

### 场景 3: Token 预算监控
```java
List<Message> messages = contextManager.getMessagesForLLM();

double usage = contextManager.getTokenBudgetUsage();

if (usage > 0.8) {
    log.warn("⚠️  警告: Token 使用超过 80%");
} else if (usage > 0.9) {
    log.error("🔴 错误: Token 使用超过 90%，立即压缩!");
    contextManager.compress();
}
```

### 场景 4: 观察结果添加
```java
// 工具执行后
ToolResult result = toolExecutor.execute(toolCall);

contextManager.addObservation(
    "Tool: " + result.getToolName() + 
    "\nResult: " + result.getOutput()
);
```

---

## 配置参数

### 默认配置
```properties
agent.context.max-tokens=4096
agent.context.compression-threshold=3000
agent.context.keep-recent-messages=5
```

### 调整建议

| 参数 | 值范围 | 推荐值 | 说明 |
|------|--------|--------|------|
| max-tokens | 2048-8192 | 4096 | 根据 LLM 模型调整 |
| compression-threshold | 60-80% | 3000 (73%) | 更激进->更早压缩，更保守->压缩次数少 |
| keep-recent-messages | 3-10 | 5 | 保留多少条最新消息不被压缩 |

---

## 常见问题

### Q: 为什么有时会压缩，有时不会？
A: 当 token 达到 compression-threshold (默认 3000) 时自动触发。实际值取决于添加的消息内容。

### Q: 压缩会丢失重要信息吗？
A: 不会。压缩会：
- ✅ 保留最近的 5 条消息（完整）
- ✅ 保留系统提示（完整）
- ✅ 合并老消息为摘要（关键信息保留）
- ✅ 从未压缩的消息中提取要点

### Q: 如何调整压缩敏感度？
A: 修改 `agent.context.compression-threshold`：
- 更低 (2000) = 更早压缩，更频繁
- 更高 (3500) = 更晚压缩，更少频繁

### Q: 能否禁用自动压缩？
A: 可以。不调用 `getMessagesForLLM()`，改为手动调用 `compress()`。

---

## 调试技巧

### 1. 查看压缩日志
```
INFO Context compressed at iteration 2: saved 156 tokens
INFO Compression ratio: 0.68 (messages merged: 3)
```

### 2. 打印上下文摘要
```java
log.info(contextManager.getContextSummary());

// 输出示例：
// Context Summary:
// ├─ Total Messages: 12
// ├─ Total Tokens: 2850/4096 (69%)
// ├─ System Tokens: 450
// ├─ Last Compression: Ratio 0.65, Saved 156 tokens
// └─ Progress: [████████░░] 69%
```

### 3. 手动检查消息
```java
ConversationHistory history = contextManager.getHistory();
Map<String, ConversationHistory.ConversationMessage> msgs = 
    history.getMessageMap();

for (String id : msgs.keySet()) {
    ConversationHistory.ConversationMessage msg = msgs.get(id);
    System.out.printf("[%s] %s tokens: %s...\n",
        msg.getRole(),
        msg.getTokenCount(),
        msg.getContent().substring(0, 50));
}
```

---

## 性能提示

✅ **做这些**:
- 定期检查 token 使用率
- 为不同任务调整 max-tokens
- 使用自动压缩，不要手动管理
- 监听压缩日志了解系统行为

❌ **避免这些**:
- 频繁调用 estimateTokensPrecise()（用 estimateTokens 替代）
- 禁用自动压缩而尝试手动管理
- 设置 keep-recent-messages 太高
- 在每条消息后都调用 compress()

---

## 版本兼容性

- ✅ Java 11+
- ✅ Spring 5.0+
- ✅ Spring Boot 2.0+
- ⚠️ 需要 Lombok 1.18.8+

---

## 集成检查清单

- [ ] ContextManager 已通过 @Autowired 注入
- [ ] systemPrompt 已在启动时初始化
- [ ] 每个用户消息都通过 addUserMessage 添加
- [ ] llm 调用使用 getMessagesForLLM()
- [ ] 定期检查 shouldCompress()
- [ ] 工具结果通过 addObservation 记录
- [ ] 错误处理中包含 getContextSummary()

---

## 相关文档

- 📖 [Step 2 完整实现文档](STEP_2_CONTEXT_OPTIMIZATION.md)
- 📖 [Token 计算细节](TOKEN_QUICK_REFERENCE.md)
- 📖 [ReasoningEngine 集成](QUICK_REFERENCE.md)

