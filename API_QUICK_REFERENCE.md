# 🎯 多轮对话 API 快速参考

## 📡 所有 API 端点汇总

```
┌─────────────────────────────────────────────────────────┐
│            🗨️ 会话管理 API                            │
└─────────────────────────────────────────────────────────┘

1️⃣  创建会话
    POST /api/chat/history/sessions
    Body: { "title": "可选标题" }
    Response: { sessionId, title, message }

2️⃣  查看单个会话
    GET /api/chat/history/sessions/{sessionId}
    Response: { sessionId, title, messages[], messageCount, ... }

3️⃣  查看所有会话
    GET /api/chat/history/sessions
    Response: { total, sessions[] }

4️⃣  删除会话
    DELETE /api/chat/history/sessions/{sessionId}
    Response: { message, sessionId }

5️⃣  清空会话消息
    DELETE /api/chat/history/sessions/{sessionId}/messages
    Response: { message, sessionId }

6️⃣  删除单条消息
    DELETE /api/chat/history/sessions/{sessionId}/messages/{messageId}
    Response: { message, sessionId, messageId, remainingMessages }

7️⃣  导出会话
    GET /api/chat/history/sessions/{sessionId}/export
    Response: { 完整会话 JSON }

8️⃣  统计信息
    GET /api/chat/history/stats
    Response: { totalSessions, totalMessages, averageMessagesPerSession }

┌─────────────────────────────────────────────────────────┐
│            💬 聊天 API                                 │
└─────────────────────────────────────────────────────────┘

🔵  发送消息（可选会话）
    POST /api/agent/chat
    Body: {
      "query": "用户问题",
      "sessionId": "可选，不传则自动创建",
      "includeDetails": true/false
    }
    Response: {
      sessionId,
      result,
      messageCount,
      iterations,
      duration_ms,
      ...
    }
```

## ⚡ 最常用操作快速命令

### 完整工作流

```bash
# 1. 创建新会话
SESSION=$(curl -s -X POST http://localhost:8080/api/chat/history/sessions \
  -H "Content-Type: application/json" \
  -d '{"title":"我的对话"}' | jq -r '.sessionId')

echo "✅ 会话 ID: $SESSION"

# 2. 第一轮对话
curl -s -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d "{\"query\":\"你好\",\"sessionId\":\"$SESSION\"}" | jq

# 3. 第二轮对话
curl -s -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d "{\"query\":\"请解释什么是人工智能\",\"sessionId\":\"$SESSION\"}" | jq

# 4. 查看历史
curl -s http://localhost:8080/api/chat/history/sessions/$SESSION | jq

# 5. 查看所有会话
curl -s http://localhost:8080/api/chat/history/sessions | jq '.total'
```

### 单个操作

```bash
# 查看所有会话
curl http://localhost:8080/api/chat/history/sessions

# 查看特定会话历史
curl http://localhost:8080/api/chat/history/sessions/{sessionId}

# 统计数据
curl http://localhost:8080/api/chat/history/stats

# 导出会话
curl http://localhost:8080/api/chat/history/sessions/{sessionId}/export > session.json

# 删除消息
curl -X DELETE http://localhost:8080/api/chat/history/sessions/{sessionId}/messages/{messageId}

# 清空会话
curl -X DELETE http://localhost:8080/api/chat/history/sessions/{sessionId}/messages

# 删除会话
curl -X DELETE http://localhost:8080/api/chat/history/sessions/{sessionId}
```

## 📝 请求/响应示例

### 创建会话 ✏️

```json
// 请求
{
  "title": "医疗咨询"
}

// 响应 200 OK
{
  "sessionId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "title": "医疗咨询",
  "message": "✅ Session created successfully"
}
```

### 发送消息 💬

```json
// 请求
{
  "query": "我最近一直头痛",
  "sessionId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "includeDetails": true
}

// 响应 200 OK
{
  "sessionId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "result": "头痛可能由多种原因引起...",
  "iterations": 1,
  "duration_ms": 3254,
  "is_complete": true,
  "messageCount": 2,
  "domainDetected": "medical",
  "steps": [...],
  "tool_results": [...]
}
```

### 查看历史 📖

```json
// 请求
GET /api/chat/history/sessions/f47ac10b-58cc-4372-a567-0e02b2c3d479

// 响应 200 OK
{
  "sessionId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "title": "医疗咨询",
  "createdTime": 1709287200000,
  "lastActivityTime": 1709287500000,
  "messageCount": 4,
  "messages": [
    {
      "role": "user",
      "content": "我最近一直头痛",
      "timestamp": 1709287200000,
      "messageId": "msg-uuid-1"
    },
    {
      "role": "assistant",
      "content": "头痛可能由多种原因引起...",
      "timestamp": 1709287205000,
      "messageId": "msg-uuid-2"
    },
    {
      "role": "user",
      "content": "应该怎么缓解",
      "timestamp": 1709287300000,
      "messageId": "msg-uuid-3"
    },
    {
      "role": "assistant",
      "content": "缓解头痛的方法包括...",
      "timestamp": 1709287500000,
      "messageId": "msg-uuid-4"
    }
  ]
}
```

## 🗂️ 数据存储架构

```
./data/sessions/
├── f47ac10b-58cc-4372-a567-0e02b2c3d479.json    (会话 1)
├── a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6.json    (会话 2)
└── ...

每个 JSON 文件的结构:
{
  "sessionId": "...",
  "createdTime": 时间戳,
  "lastActivityTime": 时间戳,
  "title": "标题",
  "messageCount": 数字,
  "messages": [
    { role, content, timestamp, messageId },
    ...
  ]
}
```

## 🎨 集成到前端

### JavaScript 示例

```javascript
// 会话管理类
class ChatSession {
  constructor() {
    this.sessionId = null;
  }

  async createSession(title = "New Chat") {
    const resp = await fetch('/api/chat/history/sessions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ title })
    });
    const data = await resp.json();
    this.sessionId = data.sessionId;
    return this.sessionId;
  }

  async sendMessage(query) {
    // 如果没有会话，创建一个
    if (!this.sessionId) {
      await this.createSession();
    }

    const resp = await fetch('/api/agent/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(
        {
          query,
          sessionId: this.sessionId,
          includeDetails: true
        }
      )
    });
    return await resp.json();
  }

  async getHistory() {
    if (!this.sessionId) return null;
    const resp = await fetch(`/api/chat/history/sessions/${this.sessionId}`);
    return await resp.json();
  }

  async deleteMessage(messageId) {
    const resp = await fetch(
      `/api/chat/history/sessions/${this.sessionId}/messages/${messageId}`,
      { method: 'DELETE' }
    );
    return await resp.json();
  }

  async exportSession() {
    const resp = await fetch(`/api/chat/history/sessions/${this.sessionId}/export`);
    return await resp.json();
  }
}

// 使用示例
const chat = new ChatSession();

// 第一轮对话
await chat.sendMessage("你好，今天天气怎么样？");
// -> 返回 sessionId 和响应

// 第二轮对话
await chat.sendMessage("那明天呢？");
// -> 使用同一个 sessionId，历史保留

// 查看完整对话历史
const history = await chat.getHistory();
console.log(history.messages);
// -> [用户消息1, 助手消息1, 用户消息2, 助手消息2]
```

## 🔍 常见 HTTP 状态码

| 状态码 | 说明 | 示例 |
|--------|------|------|
| 200 | 成功 | 成功创建/查询会话 |
| 201 | 创建成功 | 创建新会话 |
| 400 | 请求格式错误 | sessionId 格式不正确 |
| 404 | 未找到 | sessionId 不存在 |
| 500 | 服务器错误 | 文件I/O错误 |

## 🚀 性能提示

1. **缓存 sessionId**  
   - 保存返回的 sessionId，不要重复创建会话

2. **批量操作**  
   - 如需删除多条消息，循环调用删除 API

3. **定期清理**  
   - 使用统计 API 监控会话数量
   - 周期性删除过期会话

4. **导出备份**  
   - 定期导出重要会话作为备份

---

**快速开始**: 参考本文档的"完整工作流"section！
