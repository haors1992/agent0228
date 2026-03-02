# 流式响应实现指南

## 📺 概述

已成功实现完整的 **Server-Sent Events (SSE)** 流式响应系统。用户的 AI 响应现在可以实时流式传输，而不是等待完整答案，大大改善用户体验。

## 🏗️ 架构设计

### 核心组件

#### 1. **StreamingResponseHandler** (流式响应处理器)
位置: `src/main/java/com/agent/streaming/StreamingResponseHandler.java`

负责管理 SSE 连接和数据流传输。

**关键特性:**
- 异步事件发送
- 自动超时管理（5分钟）
- 连接状态监控
- 后台线程池管理

**主要方法:**
```java
void sendChunk(String content)              // 发送文字块
void sendStep(String stepName, String content)  // 发送执行步骤
void sendSearchResult(String docId, String title, double similarity)  // 发送搜索结果
void sendComplete(Map<String, Object> finalResult)  // 发送完成信息
void sendError(String errorMessage)         // 发送错误
void executeAsync(Runnable task)            // 异步执行任务
boolean isActive()                           // 检查连接状态
void close()                                  // 关闭连接
```

#### 2. **ChatController - 流式端点** (REST API)
位置: `src/main/java/com/agent/controller/ChatController.java`

添加了新的流式响应端点。

**新端点:**
```
POST /api/agent/chat/stream
Content-Type: application/json

Request:
{
  "query": "用户查询",
  "sessionId": "可选的会话ID",
  "includeDetails": false
}

Response: SSE (Server-Sent Events) 流
```

#### 3. **SseConfig** (SSE 配置)
位置: `src/main/java/com/agent/config/SseConfig.java`

配置异步请求超时和线程池。

#### 4. **streaming-chat.js** (前端 JavaScript)
位置: `src/main/resources/static/streaming-chat.js`

提供 `StreamingChat` 类用于处理流式响应。

## 📊 数据流图

```
客户端请求 /api/agent/chat/stream
    ↓
ChatController 创建 SseEmitter
    ↓
StreamingResponseHandler 初始化
    ↓
在后台线程中执行:
    ├─ 验证查询
    ├─ 获取或创建会话
    ├─ 发送:"🤔 Reasoning..."
    ├─ 执行 ReasoningEngine
    ├─ 流式发送响应内容
    │   (按句子分割，每 50ms 发送一个)
    ├─ 保存会话
    ├─ 发送完成标记
    └─ 关闭连接
    ↓
客户端接收 SSE 事件
    ├─ message: 响应文本
    ├─ step: 执行步骤
    ├─ search_result: 搜索结果
    ├─ complete: 完成信息
    └─ error: 错误信息
    ↓
实时更新 UI
```

## 🎯 工作流程

### 服务端工作流

```
1. 接收 HTTP POST 请求
   ↓
2. 创建 SseEmitter（事件源）
   ↓
3. 创建 StreamingResponseHandler（处理器）
   ↓
4. 提交异步任务到线程池
   ↓
5. 后台线程执行:
   - 验证输入
   - 获取会话
   - 执行 AI 推理
   - 流式发送响应
   - 保存数据
   ↓
6. 返回 HTTP 响应给客户端（保持连接开放）
   ↓
7. 服务端继续通过 SSE 发送事件
   ↓
8. 完成后发送 "complete" 事件
   ↓
9. 客户端关闭 EventSource
```

### 客户端工作流

```
1. 创建 StreamingChat 实例
2. 调用 startStreamingChat(message, sessionId)
3. fetch() 发送 POST 请求到 /api/agent/chat/stream
4. 获得 Response 对象（含 ReadableStream）
5. 创建 TextDecoder
6. 循环读取 stream:
   - reader.read() ← 块数据
   - 解析 SSE 事件
   - 处理不同事件类型
   - 更新 DOM
7. 完成或错误时关闭流
```

## 🔧 技术实现

### SSE 事件格式

```
: 注释
id: 事件ID
name: 事件名称
data: 事件数据

```

**示例:**

```
id: 1234567890
name: message
data: 这是响应的第一部分

id: 1234567891
name: message
data: 这是响应的第二部分

id: 1234567892
name: complete
data: {"sessionId":"xxx","duration_ms":2500}
```

### 支持的事件类型

| 事件名 | 数据格式 | 说明 |
|--------|---------|------|
| `message` | 纯文本 | AI 响应内容 |
| `step` | JSON | `{step: "名称", content: "内容"}` |
| `search_result` | JSON | `{title: "标题", similarity: "85%", docId: "xxx"}` |
| `complete` | JSON | `{sessionId, duration_ms, iterations, messageCount}` |
| `error` | JSON | `{error: "错误信息", timestamp}` |

## 💻 使用示例

### 1. JavaScript 客户端

```javascript
// 创建流式聊天实例
const chat = new StreamingChat('messages');

// 发送流式查询
chat.startStreamingChat('你好，请告诉我 Python 的优点', sessionId);

// 获取当前会话 ID
const currentSession = chat.getSessionId();

// 清空消息
chat.clearMessages();
```

### 2. HTML 集成

```html
<div id="messages" class="message-container"></div>

<div class="input-area">
    <input type="text" id="messageInput" placeholder="输入消息">
    <button onclick="sendMessage()">发送</button>
</div>

<script src="/streaming-chat.js"></script>
<script>
    const chat = new StreamingChat('messages');
    
    function sendMessage() {
        const message = document.getElementById('messageInput').value;
        chat.startStreamingChat(message);
        document.getElementById('messageInput').value = '';
    }
</script>
```

### 3. 使用 curl 测试

```bash
# 流式请求
curl -N -X POST http://localhost:8080/api/agent/chat/stream \
  -H "Content-Type: application/json" \
  -d '{
    "query": "什么是人工智能?",
    "sessionId": "test-session-1"
  }'
```

## 🎨 前端样式建议

```css
/* 消息容器 */
.message-container {
  max-height: 500px;
  overflow-y: auto;
  padding: 15px;
  background: #f5f5f5;
  border-radius: 8px;
}

/* 消息 */
.message {
  display: flex;
  gap: 10px;
  margin: 10px 0;
  animation: slideIn 0.3s ease-in-out;
}

.message-user {
  flex-direction: row-reverse;
}

.message-user .message-content {
  background: #2196f3;
  color: white;
}

.message-assistant .message-content {
  background: white;
  border: 1px solid #ddd;
}

/* 消息头像 */
.message-avatar {
  font-size: 24px;
  flex-shrink: 0;
}

/* 消息内容 */
.message-content {
  max-width: 70%;
  padding: 12px;
  border-radius: 8px;
  word-wrap: break-word;
  animation: fadeIn 0.5s ease-in-out;
}

/* 执行步骤 */
.stream-step {
  padding: 10px;
  margin: 5px 0;
  background: #e8f5e9;
  border-left: 4px solid #4caf50;
  font-size: 12px;
}

/* 搜索结果 */
.search-result-badge {
  display: inline-block;
  padding: 5px 10px;
  margin: 2px;
  background: #fff3e0;
  border: 1px solid #ffb74d;
  border-radius: 4px;
  font-size: 12px;
}

.result-title {
  font-weight: bold;
}

.result-similarity {
  margin-left: 10px;
  color: #f57c00;
}

/* 完成统计 */
.completion-stats {
  padding: 10px;
  margin: 10px 0;
  background: #e3f2fd;
  border-left: 4px solid #2196f3;
  font-size: 12px;
  color: #1976d2;
}

/* 动画 */
@keyframes slideIn {
  from {
    transform: translateX(-20px);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
```

## 📈 性能考虑

| 指标 | 值 |
|------|-----|
| 超时时间 | 5 分钟 |
| 重连时间 | 5 秒 |
| 线程池大小 | 1（可配置） |
| 流式发送延迟 | 50ms / 句 |

## ⚙️ 配置选项

### application.yml
```yaml
# SSE 配置自动继承 Spring 默认配置
# 可在 SseConfig 中自定义超时时间
server:
  tomcat:
    threads:
      max: 200
```

## 🐛 错误处理

### 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 连接立即关闭 | 客户端错误 | 检查浏览器控制台 |
| 5分钟后断开 | 超时 | 增加 SseEmitter 超时 |
| 无法接收事件 | 格式错误 | 验证 SSE 数据格式 |
| 消息重复 | 重连 | 实现客户端去重 |

### 错误恢复

```javascript
// 自动重新连接
async function newChatWithRetry(message, maxAttempts = 3) {
  for (let i = 0; i < maxAttempts; i++) {
    try {
      await chat.startStreamingChat(message);
      return;
    } catch (e) {
      console.log(`尝试 ${i + 1} 失败，重试...`);
      await new Promise(resolve => setTimeout(resolve, 1000 * (i + 1)));
    }
  }
  console.error('连接失败');
}
```

## 🚀 生产部署建议

### 1. 调整超时时间
```java
// 根据平均响应时间调整
SseEmitter emitter = new SseEmitter(600000L); // 10 分钟
```

### 2. 添加连接池
```yaml
server:
  tomcat:
    threads:
      max: 500        # 更多并发连接
      min-spare: 10
```

### 3. 监控连接数
```java
// 在 StreamingResponseHandler 中添加计数器
private static AtomicInteger activeConnections = new AtomicInteger(0);
```

### 4. 心跳保活
```java
// 定期发送心跳防止连接超时
handler.sendChunk(""); // 空数据保持活跃
```

## 📋 检查清单

- [x] StreamingResponseHandler 实现
- [x] ChatController 流式端点
- [x] SSE 配置
- [x] 异常处理
- [x] JavaScript 客户端库
- [x] 编译验证
- [ ] 前端 UI 集成
- [ ] 生产环境测试
- [ ] 性能基准测试
- [ ] 文档完整化

## 🎓 技术要点

本实现展示了:
- ✅ Server-Sent Events (SSE) 实时通信
- ✅ 异步响应处理
- ✅ Spring Boot SSE 集成
- ✅ 连接生命周期管理
- ✅ 错误恢复机制
- ✅ 前端流式数据接收

## 📚 相关资源

- [MDN SSE 文档](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)
- [Spring SseEmitter 文档](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/SseEmitter.html)
- [EventSource API](https://developer.mozilla.org/en-US/docs/Web/API/EventSource)

---

**创建时间**: 2026-03-01  
**版本**: 1.0  
**状态**: ✅ 完成与编译验证通过
