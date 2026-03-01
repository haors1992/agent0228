# 🗨️ 多轮对话历史存储 - 完整使用指南

## 📋 功能概述

本项目已支持**多轮对话历史存储**，允许用户在多次交互中保持对话上下文。所有对话历史存储在本地 JSON 文件中，支持查看、导出、删除、清空等操作。

## 🏗️ 架构设计

### 核心组件

```
ChatSession（会话）
    ├── sessionId: 唯一会话标识符
    ├── createdTime: 创建时间戳
    ├── lastActivityTime: 最后活动时间
    ├── title: 会话标题
    └── messages: 对话消息列表[]
        └── ConversationMessage
            ├── role: "user" 或 "assistant"
            ├── content: 消息内容
            ├── timestamp: 消息时间戳
            └── messageId: 唯一消息 ID
```

### 数据存储

- **位置**: `./data/sessions/` 目录
- **格式**: JSON 文件（每个会话一个文件）
- **文件名**: `{sessionId}.json`

**示例文件**:
```
./data/sessions/
├── 550e8400-e29b-41d4-a716-446655440000.json
├── 6ba7b810-9dad-11d1-80b4-00c04fd430c8.json
└── ...
```

## 🚀 使用方式

### 1️⃣ **创建新会话**

**请求**:
```bash
POST /api/chat/history/sessions
Content-Type: application/json

{
  "title": "医疗咨询 - 头痛问题"
}
```

**响应**:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "医疗咨询 - 头痛问题",
  "message": "✅ Session created successfully"
}
```

### 2️⃣ **发送对话消息（带会话）**

**使用新创建的 sessionId 发送问题**:

```bash
POST /api/agent/chat
Content-Type: application/json

{
  "query": "我最近一直头痛，应该吃什么药？",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "includeDetails": true
}
```

**响应**:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "result": "头痛的原因很多，不建议自行用药。建议您先咨询医生...",
  "iterations": 1,
  "duration_ms": 4250,
  "is_complete": true,
  "messageCount": 2,
  "steps": [...],
  "tool_results": [...]
}
```

**关键点**:
- `messageCount: 2` = 1 条用户消息 + 1 条助手消息
- 消息自动保存到会话历史
- 后续问题使用同一个 `sessionId` 继续对话

### 3️⃣ **查看对话历史**

**获取单个会话的所有消息**:

```bash
GET /api/chat/history/sessions/550e8400-e29b-41d4-a716-446655440000
```

**响应**:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "医疗咨询 - 头痛问题",
  "createdTime": 1709287200000,
  "lastActivityTime": 1709287320000,
  "messageCount": 4,
  "messages": [
    {
      "role": "user",
      "content": "我最近一直头痛，应该吃什么药？",
      "timestamp": 1709287200000,
      "messageId": "msg-001"
    },
    {
      "role": "assistant",
      "content": "头痛的原因很多...",
      "timestamp": 1709287205000,
      "messageId": "msg-002"
    },
    {
      "role": "user",
      "content": "那应该怎么缓解？",
      "timestamp": 1709287250000,
      "messageId": "msg-003"
    },
    {
      "role": "assistant",
      "content": "缓解头痛的方法包括...",
      "timestamp": 1709287320000,
      "messageId": "msg-004"
    }
  ]
}
```

### 4️⃣ **查看所有会话**

```bash
GET /api/chat/history/sessions
```

**响应**:
```json
{
  "total": 3,
  "sessions": [
    {
      "sessionId": "550e8400-e29b-41d4-a716-446655440000",
      "title": "医疗咨询 - 头痛问题",
      "createdTime": 1709287200000,
      "lastActivityTime": 1709287320000,
      "messageCount": 4,
      "messages": [...]
    },
    {
      "sessionId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
      "title": "编程问题 - Spring Boot",
      "createdTime": 1709286000000,
      "lastActivityTime": 1709286500000,
      "messageCount": 6,
      "messages": [...]
    },
    ...
  ]
}
```

### 5️⃣ **删除单条消息**

```bash
DELETE /api/chat/history/sessions/550e8400-e29b-41d4-a716-446655440000/messages/msg-003
```

**响应**:
```json
{
  "message": "✅ Message deleted successfully",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "messageId": "msg-003",
  "remainingMessages": 3
}
```

### 6️⃣ **清空会话内所有消息**

```bash
DELETE /api/chat/history/sessions/550e8400-e29b-41d4-a716-446655440000/messages
```

**响应**:
```json
{
  "message": "✅ Session messages cleared",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 7️⃣ **删除整个会话**

```bash
DELETE /api/chat/history/sessions/550e8400-e29b-41d4-a716-446655440000
```

**响应**:
```json
{
  "message": "✅ Session deleted successfully",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 8️⃣ **导出会话为 JSON**

```bash
GET /api/chat/history/sessions/550e8400-e29b-41d4-a716-446655440000/export
```

返回完整的会话对象（与查看历史相同的格式），可用于备份或分享。

### 9️⃣ **统计信息**

```bash
GET /api/chat/history/stats
```

**响应**:
```json
{
  "totalSessions": 5,
  "totalMessages": 28,
  "averageMessagesPerSession": 5.6
}
```

## 🔄 多轮对话流程示例

### 实际场景：医疗咨询

```bash
# 1️⃣ 创建新会话
curl -X POST http://localhost:8080/api/chat/history/sessions \
  -H "Content-Type: application/json" \
  -d '{"title": "头痛诊询"}'

# 响应得到: sessionId = "abc123"

# 2️⃣ 第一轮对话
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "query": "我最近一直头痛，应该吃什么药？",
    "sessionId": "abc123"
  }'

# 响应: 医学建议 + messageCount: 2

# 3️⃣ 第二轮对话（同一会话）
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "query": "那应该怎么缓解？",
    "sessionId": "abc123"
  }'

# 响应: 缓解方法 + messageCount: 4

# 4️⃣ 第三轮对话
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "query": "需要去医院吗？",
    "sessionId": "abc123"
  }'

# 响应: 医院建议 + messageCount: 6

# 5️⃣ 查看完整对话历史
curl http://localhost:8080/api/chat/history/sessions/abc123

# 响应: 所有 6 条消息的完整历史
```

## 📊 数据存储格式

### JSON 文件示例

`./data/sessions/abc123.json`:
```json
{
  "sessionId": "abc123",
  "createdTime": 1709287200000,
  "lastActivityTime": 1709287500000,
  "title": "头痛诊询",
  "messageCount": 6,
  "messages": [
    {
      "role": "user",
      "content": "我最近一直头痛，应该吃什么药？",
      "timestamp": 1709287200000,
      "messageId": "uuid-1"
    },
    {
      "role": "assistant",
      "content": "建议咨询医生...",
      "timestamp": 1709287205000,
      "messageId": "uuid-2"
    },
    ...
  ]
}
```

## ⚙️ 配置说明

### application.yml

```yaml
agent:
  session:
    storage-path: ./data/sessions  # 会话存储路径
```

### 修改存储位置

编辑 `src/main/resources/application.yml`:

```yaml
agent:
  session:
    storage-path: /var/lib/agent/sessions  # 改为自定义路径
```

## 🔐 会话管理特性

| 特性 | 说明 |
|------|------|
| **自动创建** | 第一次使用 sessionId 时自动创建会话 |
| **自动保存** | 每次发送消息后自动保存会话 |
| **时间戳** | 每条消息都保存时间戳 |
| **消息 ID** | 每条消息有唯一 ID，支持删除 |
| **持久化** | 应用重启后历史保留 |
| **缓存** | 内存缓存加速 |

## 🚨 常见问题

### Q: 没有传递 sessionId 会怎样？

**A**: 系统会自动生成新的 sessionId。
- 响应中会包含新的 sessionId
- 可以使用这个 ID 继续对话

```bash
POST /api/agent/chat
{
  "query": "请计算 100 + 200"
  # 没有传 sessionId
}

# 响应会包含:
{
  "sessionId": "auto-generated-uuid",
  "result": "300",
  "messageCount": 2
}
```

### Q: 如何在前端中实现多轮对话？

**A**: 保存返回的 sessionId，在后续请求中使用：

```javascript
// 初始化
let sessionId = null;

// 发送第一条消息
fetch('/api/agent/chat', {
  method: 'POST',
  body: JSON.stringify({
    query: userInput,
    sessionId: sessionId  // 首次为 null
  })
})
.then(resp => resp.json())
.then(data => {
  sessionId = data.sessionId;  // 保存 sessionId
  displayMessage(data.result);
});

// 发送后续消息时重用 sessionId
fetch('/api/agent/chat', {
  method: 'POST',
  body: JSON.stringify({
    query: userInput,
    sessionId: sessionId  // 使用保存的 sessionId
  })
})
.then(resp => resp.json())
.then(data => {
  displayMessage(data.result);
});
```

### Q: 数据存储在哪里？

**A**: 默认存储在 `./data/sessions/` 目录：

```bash
./data/sessions/
├── session-id-1.json
├── session-id-2.json
└── session-id-3.json

# 查看文件
ls -la ./data/sessions/

# 查看单个会话内容
cat ./data/sessions/abc123.json | jq
```

### Q: 如何备份和恢复?

**A**: 直接复制 `./data/sessions/` 目录：

```bash
# 备份
cp -r ./data/sessions ./data/sessions.backup

# 恢复
cp -r ./data/sessions.backup/* ./data/sessions/
```

## 📈 下一步扩展

考虑的功能增强：

- [ ] **数据库存储** - 使用 SQLite / MySQL 替代文件存储
- [ ] **用户认证** - 基于用户的会话隔离
- [ ] **时间限制** - 自动过期老会话
- [ ] **搜索功能** - 跨会话搜索消息
- [ ] **导入/导出** - CSV、PDF 格式导出
- [ ] **版本管理** - 记录消息编辑历史
- [ ] **分享链接** - 公开分享会话

---

**现在你可以进行完整的多轮对话，并保留完整的对话历史！** 🎉
