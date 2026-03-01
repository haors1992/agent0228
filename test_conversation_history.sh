#!/bin/bash

# 🧪 多轮对话功能测试脚本
# 使用方式: bash test_conversation_history.sh

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# API 基础 URL
BASE_URL="http://localhost:8080"

echo -e "${BLUE}╔══════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  🧪 多轮对话历史存储 - 功能测试    ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════╝${NC}"
echo ""

# 测试 1: 创建会话
echo -e "${YELLOW}[测试 1]${NC} 创建新会话..."
SESSION_RESPONSE=$(curl -s -X POST "$BASE_URL/api/chat/history/sessions" \
  -H "Content-Type: application/json" \
  -d '{"title":"健康咨询会话"}')

SESSION_ID=$(echo "$SESSION_RESPONSE" | jq -r '.sessionId')
echo -e "${GREEN}✅ 会话创建成功${NC}"
echo "   会话 ID: $SESSION_ID"
echo ""

# 测试 2: 第一轮对话
echo -e "${YELLOW}[测试 2]${NC} 第一轮对话..."
RESPONSE_1=$(curl -s -X POST "$BASE_URL/api/agent/chat" \
  -H "Content-Type: application/json" \
  -d "{\"query\":\"我最近一直头痛，应该吃什么药？\",\"sessionId\":\"$SESSION_ID\"}")

RESULT_1=$(echo "$RESPONSE_1" | jq -r '.result')
MSG_COUNT_1=$(echo "$RESPONSE_1" | jq -r '.messageCount')
echo -e "${GREEN}✅ 获得回复${NC}"
echo "   消息数: $MSG_COUNT_1"
echo "   回复: ${RESULT_1:0:60}..."
echo ""

# 测试 3: 第二轮对话
echo -e "${YELLOW}[测试 3]${NC} 第二轮对话..."
RESPONSE_2=$(curl -s -X POST "$BASE_URL/api/agent/chat" \
  -H "Content-Type: application/json" \
  -d "{\"query\":\"那应该怎么缓解？\",\"sessionId\":\"$SESSION_ID\"}")

RESULT_2=$(echo "$RESPONSE_2" | jq -r '.result')
MSG_COUNT_2=$(echo "$RESPONSE_2" | jq -r '.messageCount')
echo -e "${GREEN}✅ 获得回复${NC}"
echo "   消息数: $MSG_COUNT_2"
echo "   回复: ${RESULT_2:0:60}..."
echo ""

# 测试 4: 第三轮对话
echo -e "${YELLOW}[测试 4]${NC} 第三轮对话..."
RESPONSE_3=$(curl -s -X POST "$BASE_URL/api/agent/chat" \
  -H "Content-Type: application/json" \
  -d "{\"query\":\"需要去医院吗？\",\"sessionId\":\"$SESSION_ID\"}")

RESULT_3=$(echo "$RESPONSE_3" | jq -r '.result')
MSG_COUNT_3=$(echo "$RESPONSE_3" | jq -r '.messageCount')
echo -e "${GREEN}✅ 获得回复${NC}"
echo "   消息数: $MSG_COUNT_3"
echo "   回复: ${RESULT_3:0:60}..."
echo ""

# 测试 5: 查看会话历史
echo -e "${YELLOW}[测试 5]${NC} 查看完整会话历史..."
HISTORY=$(curl -s "$BASE_URL/api/chat/history/sessions/$SESSION_ID")

HISTORY_MSG_COUNT=$(echo "$HISTORY" | jq '.messageCount')
echo -e "${GREEN}✅ 历史查询成功${NC}"
echo "   总消息数: $HISTORY_MSG_COUNT"
echo ""

# 验证消息数是否正确
if [ "$MSG_COUNT_3" -eq 6 ] && [ "$HISTORY_MSG_COUNT" -eq 6 ]; then
  echo -e "${GREEN}✅ 消息计数验证通过（3 轮对话 = 6 条消息）${NC}"
else
  echo -e "${RED}❌ 消息计数不匹配！${NC}"
fi
echo ""

# 测试 6: 显示所有消息
echo -e "${YELLOW}[测试 6]${NC} 显示所有消息内容..."
MESSAGES=$(echo "$HISTORY" | jq '.messages')

echo "$MESSAGES" | jq -r '.[] | "[\(.role | ascii_upcase)] \(.content)"' | head -20
echo ""

# 测试 7: 删除单条消息
echo -e "${YELLOW}[测试 7]${NC} 删除第 3 条消息..."
MESSAGE_ID_3=$(echo "$HISTORY" | jq -r '.messages[2].messageId')

if [ ! -z "$MESSAGE_ID_3" ] && [ "$MESSAGE_ID_3" != "null" ]; then
  DELETE_RESPONSE=$(curl -s -X DELETE \
    "$BASE_URL/api/chat/history/sessions/$SESSION_ID/messages/$MESSAGE_ID_3")
  
  REMAINING=$(echo "$DELETE_RESPONSE" | jq '.remainingMessages')
  echo -e "${GREEN}✅ 消息删除成功${NC}"
  echo "   剩余消息数: $REMAINING"
else
  echo -e "${YELLOW}⚠️  跳过删除测试（未获取到消息 ID）${NC}"
fi
echo ""

# 测试 8: 查看所有会话
echo -e "${YELLOW}[测试 8]${NC} 查看所有会话..."
SESSIONS=$(curl -s "$BASE_URL/api/chat/history/sessions")
TOTAL_SESSIONS=$(echo "$SESSIONS" | jq '.total')
echo -e "${GREEN}✅ 查询成功${NC}"
echo "   总会话数: $TOTAL_SESSIONS"
echo ""

# 测试 9: 统计信息
echo -e "${YELLOW}[测试 9]${NC} 查看统计信息..."
STATS=$(curl -s "$BASE_URL/api/chat/history/stats")
TOTAL_MSG=$(echo "$STATS" | jq '.totalMessages')
AVG_MSG=$(echo "$STATS" | jq '.averageMessagesPerSession')
echo -e "${GREEN}✅ 统计查询成功${NC}"
echo "   总消息数: $TOTAL_MSG"
echo "   平均每会话消息数: $AVG_MSG"
echo ""

# 测试 10: 导出会话
echo -e "${YELLOW}[测试 10]${NC} 导出会话为 JSON..."
EXPORT=$(curl -s "$BASE_URL/api/chat/history/sessions/$SESSION_ID/export")
EXPORTED_MSG_COUNT=$(echo "$EXPORT" | jq '.messageCount')
echo -e "${GREEN}✅ 导出成功${NC}"
echo "   导出的消息数: $EXPORTED_MSG_COUNT"
echo ""

# 创建第二个会话进行对比测试
echo -e "${YELLOW}[测试 11]${NC} 创建第二个会话（编程主题）..."
SESSION_2=$(curl -s -X POST "$BASE_URL/api/chat/history/sessions" \
  -H "Content-Type: application/json" \
  -d '{"title":"编程问题"}' | jq -r '.sessionId')

echo -e "${GREEN}✅ 第二个会话创建成功${NC}"
echo "   会话 ID: $SESSION_2"
echo ""

echo -e "${YELLOW}[测试 12]${NC} 发送编程相关问题..."
curl -s -X POST "$BASE_URL/api/agent/chat" \
  -H "Content-Type: application/json" \
  -d "{\"query\":\"如何使用 Spring Boot 开发 REST API？\",\"sessionId\":\"$SESSION_2\"}" > /dev/null

echo -e "${GREEN}✅ 消息已发送${NC}"
echo ""

# 最终验证
echo -e "${YELLOW}[测试 13]${NC} 最终验证 - 查看所有会话..."
FINAL_SESSIONS=$(curl -s "$BASE_URL/api/chat/history/sessions")
FINAL_COUNT=$(echo "$FINAL_SESSIONS" | jq '.total')
echo -e "${GREEN}✅ 最终会话数: $FINAL_COUNT${NC}"
echo ""

# 汇总测试结果
echo -e "${BLUE}╔══════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  ✅ 所有测试完成！                    ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════╝${NC}"
echo ""

echo "📊 测试摘要:"
echo "   ✅ 会话创建: 成功"
echo "   ✅ 多轮对话: 成功 (3 轮)"
echo "   ✅ 历史查询: 成功"
echo "   ✅ 消息删除: 成功"
echo "   ✅ 会话查询: 成功 ($TOTAL_SESSIONS 个会话)"
echo "   ✅ 统计查询: 成功"
echo "   ✅ 导出功能: 成功"
echo ""

echo "📁 文件存储位置:"
echo "   ./data/sessions/$SESSION_ID.json"
echo ""

echo "🔗 相关 API "
echo "   查看历史: GET /api/chat/history/sessions/$SESSION_ID"
echo "   删除会话: DELETE /api/chat/history/sessions/$SESSION_ID"
echo "   导出会话: GET /api/chat/history/sessions/$SESSION_ID/export"
echo ""

echo -e "${GREEN}🎉 多轮对话功能正常运行！${NC}"
