# 🧠 对话上下文功能说明

## 概述

现在系统已经支持**完整的对话上下文**。AI 在回复时会自动参考之前的对话历史，从而提供更准确和连贯的回答。

## 工作原理

### 后端流程

```
1. 用户发送消息 → ChatController
   ↓
2. 获取该会话的完整消息历史
   ↓
3. 将历史消息转换为上下文格式 (ROLE: content)
   ↓
4. 传递给 ReasoningEngine.execute(query, conversationHistory)
   ↓
5. ReasoningEngine 将历史消息添加到 LLM 请求
   ↓
6. LLM 基于完整上下文进行推理
   ↓
7. AI 回复 → 保存到历史 → 返回给用户
```

### 前端流程

```
1. 用户输入消息
   ↓
2. 获取该会话的所有历史消息
   ↓
3. 构建请求：
   {
     "query": "当前问题",
     "sessionId": "session-id",
     "conversationHistory": ["USER: ...", "ASSISTANT: ...", ...],
     "includeDetails": true
   }
   ↓
4. 发送到后端
   ↓
5. 显示 AI 回复 + 更新消息计数
```

## 核心修改

### 1. 后端 - ChatController.java

**新增**：`conversationHistory` 字段到 ChatRequest
```java
public static class ChatRequest {
    private String query;
    private String sessionId;
    private List<String> conversationHistory;  // ← NEW
    private boolean includeDetails = false;
}
```

**修改**：chat 方法中传递历史到 ReasoningEngine
```java
// 构建对话历史上下文
List<String> conversationHistory = session.getMessages().stream()
        .map(msg -> msg.getRole().toUpperCase() + ": " + msg.getContent())
        .collect(Collectors.toList());

// 执行推理，带上下文
ExecutionContext context = reasoningEngine.execute(request.getQuery(), conversationHistory);
```

### 2. 后端 - ReasoningEngine.java

**新增**：重载 execute 方法接收历史消息
```java
public ExecutionContext execute(String userQuery) {
    return execute(userQuery, new ArrayList<>());
}

public ExecutionContext execute(String userQuery, List<String> conversationHistory) {
    // ... 初始化 messages ...
    
    // 添加对话历史到消息列表
    if (conversationHistory != null && !conversationHistory.isEmpty()) {
        for (String history : conversationHistory) {
            String[] parts = history.split(":", 2);
            String role = parts[0].trim().toLowerCase();
            String content = parts[1].trim();
            
            messages.add(Message.builder()
                    .role(role)
                    .content(content)
                    .build());
        }
        log.info("✅ Context loaded: {} previous messages added", messages.size() - 1);
    }
    
    // ... 继续推理循环 ...
}
```

### 3. 前端 - index.html

**修改**：sendMessage 函数获取并传递历史
```javascript
async function sendMessage() {
    // ... 基本验证 ...
    
    // 🧠 获取当前会话的历史消息作为上下文
    let conversationHistory = [];
    const historyResponse = await fetch(`/api/chat/history/sessions/${currentSessionId}`);
    const historyData = await historyResponse.json();
    
    if (historyData.messages && historyData.messages.length > 0) {
        conversationHistory = historyData.messages
            .filter(msg => msg.content !== message)
            .map(msg => msg.role.toUpperCase() + ": " + msg.content);
    }
    
    // 发送请求，包含历史消息
    const response = await fetch('/api/agent/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            query: message,
            sessionId: currentSessionId,
            conversationHistory: conversationHistory,  // ← 传递上下文
            includeDetails: true
        })
    });
    
    // ... 处理响应 ...
}
```

## 使用示例

### 多轮对话场景

```
轮次 1：
  用户: "我叫小王，今年30岁，住在北京"
  AI: "很高兴认识你，小王"
  
  系统内部：
  ✅ 消息被保存到会话历史
  
轮次 2：
  用户: "请问我叫什么名字？"
  
  系统内部：
  📚 获取历史：["USER: 我叫小王...", "ASSISTANT: 很高兴..."]
  🧠 传递给 AI：包含完整上下文
  ✅ AI 理解：根据历史回复 "你叫小王"
  
轮次 3：
  用户: "我住在哪个城市？"
  
  系统内部：
  📚 获取历史：["USER: 我叫小王...", "ASSISTANT: ...", "USER: 请问...", "ASSISTANT: ..."]
  🧠 传递给 AI：包含更多上下文
  ✅ AI 理解：根据历史回复 "你住在北京"
```

## API 数据流

### 请求格式

```json
{
  "query": "请问我叫什么名字？",
  "sessionId": "749f8985-2ace-472a-b568-840c1e25994d",
  "conversationHistory": [
    "USER: 我叫小王，今年30岁，住在北京",
    "ASSISTANT: 很高兴认识你，小王"
  ],
  "includeDetails": true
}
```

### 后端处理

1. **接收请求** → ChatController
2. **加载会话** → SessionManager
3. **构建上下文** → 从历史消息构建 conversationHistory
4. **传递给引擎** → ReasoningEngine.execute(query, history)
5. **添加到 LLM** → 作为消息序列的一部分
6. **保存和回复** → 新消息保存，结果返回

## 日志示例

启用上下文时，你会看到这样的日志：

```
2026-03-01 11:30:45.123  INFO ReasoningEngine: Starting agent reasoning for query: 请问我叫什么名字？
2026-03-01 11:30:45.124  INFO ReasoningEngine: 📚 Including 2 messages in conversation context
2026-03-01 11:30:45.125  INFO ReasoningEngine: ✅ Context loaded: 2 previous messages added
2026-03-01 11:30:45.567  INFO ReasoningEngine: Chat request completed in 442ms with 1 iterations
```

## 测试方法

### 1. 运行自动化测试

```bash
bash test_context.sh
```

这个脚本会：
- 创建新会话
- 发送包含个人信息的消息
- 发送需要上下文的问题
- 验证 AI 是否理解历史信息

### 2. 前端测试

1. 打开 http://localhost:8080
2. 创建新会话
3. 发送消息：`"我叫小李，喜欢编程"`
4. 发送问题：`"我叫什么名字？"`
5. AI 应该能准确回答（需要 API 密钥有效）

### 3. 查看上下文信息

获取会话历史查看是否包含所有消息：

```bash
curl http://localhost:8080/api/chat/history/sessions/{sessionId}
```

## 配置选项

系统认识到当前实现中的一些优化点：

- **消息历史长度**：目前保存所有历史（可配置限制）
- **上下文窗口**：全部历史都被发送给 LLM
- **压缩策略**：可考虑添加消息摘要功能

## 注意事项

✅ **当前支持**：
- 完整的对话历史存储
- 上下文自动传递
- 多轮连贯对话
- 历史查看和导出
- 会话持久化

⚠️ **限制**：
- LLM API 密钥需要有效
- 历史消息完全发送（大量历史可能影响性能）
- 暂无消息压缩或摘要功能

## 总结

现在你的 AI Agent 支持**智能多轮对话**：
- 每条消息都被保存
- AI 可以参考完整的对话历史
- 用户体验如同与真实对话者交谈
- 所有数据持久化存储和管理

🎯 **核心优势**：从"问答机"升级到"对话伙伴"！
