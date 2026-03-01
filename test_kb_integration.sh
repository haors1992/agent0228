#!/bin/bash

# 测试对话中的知识库集成
# 验证 AI 是否使用知识库进行推理

BASE_URL="http://localhost:8080"
echo "🧠 测试对话中的知识库集成"
echo "================================================"

# 1. 确保知识库有文档
echo "📚 添加知识库文档..."
curl -s -X POST "${BASE_URL}/api/knowledge/documents" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Python 并发编程",
    "content": "Python 中的并发编程可以通过多线程、多进程和异步编程实现。threading 模块用于多线程编程，multiprocessing 模块用于多进程编程。async/await 用于异步编程。GIL（全局解释器锁）限制了线程级别的真正并行。",
    "category": "编程",
    "source": "官方教程"
  }' > /dev/null

curl -s -X POST "${BASE_URL}/api/knowledge/documents" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "REST API 设计最佳实践",
    "content": "RESTful API 应该使用标准的 HTTP 方法（GET、POST、PUT、DELETE）。使用正确的 HTTP 状态码响应请求。API 端点应该是名词形式而不是动词形式。使用版本控制来管理 API 的演变。",
    "category": "web",
    "source": "API 设计指南"
  }' > /dev/null

echo "✅ 知识库文档已添加"

# 2. 创建一个新会话
echo -e "\n📝 创建新会话..."
SESSION_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/chat/history/sessions" \
  -H "Content-Type: application/json" \
  -d '{"domain": "编程"}')

SESSION_ID=$(echo "$SESSION_RESPONSE" | jq -r '.sessionId // empty')

if [ -z "$SESSION_ID" ]; then
  echo "❌ 无法创建会话"
  echo "$SESSION_RESPONSE"
  exit 1
fi

echo "✅ 会话创建成功: $SESSION_ID"

# 3. 发送一个关于 Python 并发的问题
echo -e "\n🎯 发送问题：关于 Python 并发编程..."
RESPONSE=$(curl -s -X POST "${BASE_URL}/api/agent/chat" \
  -H "Content-Type: application/json" \
  -d "{
    \"sessionId\": \"$SESSION_ID\",
    \"query\": \"Python 中有哪些方法可以实现并发编程？\"
  }")

echo "AI 响应:"
echo "$RESPONSE" | jq -c '.result // .'

# 4. 发送关于 REST API 的后续问题
echo -e "\n\n🎯 发送第二个问题：关于 REST API 设计..."
RESPONSE2=$(curl -s -X POST "${BASE_URL}/api/agent/chat" \
  -H "Content-Type: application/json" \
  -d "{
    \"sessionId\": \"$SESSION_ID\",
    \"query\": \"如何设计一个好的 REST API？\"
  }")

echo "AI 响应:"
echo "$RESPONSE2" | jq -c '.result // .'

# 5. 查看该会话的历史
echo -e "\n\n📜 查看会话历史..."
curl -s "${BASE_URL}/api/chat/history/sessions/${SESSION_ID}/messages" | jq '.messages | length as $count | "共 \($count) 条消息" | {message: .}'

echo -e "\n================================================"
echo "✅ 知识库集成测试完成！"
echo "================================================"
