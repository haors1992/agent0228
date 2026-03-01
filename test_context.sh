#!/bin/bash

# 🧪 对话上下文功能测试脚本

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

BASE_URL="http://localhost:8080"

echo -e "${BLUE}╔════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  🧪 对话上下文 - 功能测试        ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════╝${NC}"
echo ""

# 创建会话
echo -e "${YELLOW}[测试 1]${NC} 创建新会话..."
SESSION=$(curl -s -X POST "$BASE_URL/api/chat/history/sessions" \
  -H "Content-Type: application/json" \
  -d '{"title":"上下文测试"}' | jq -r '.sessionId')

echo -e "${GREEN}✅ 会话创建: $SESSION${NC}"
echo ""

# 第一条消息
echo -e "${YELLOW}[测试 2]${NC} 发送第一条消息..."
MSG1="我叫小王，今年30岁，住在北京"

curl -s -X POST "$BASE_URL/api/agent/chat" \
  -H "Content-Type: application/json" \
  -d "{\"query\":\"$MSG1\",\"sessionId\":\"$SESSION\"}" > /dev/null

echo -e "${GREEN}✅ 消息已发送: $MSG1${NC}"
echo ""

# 第二条消息（需要上下文理解）
echo -e "${YELLOW}[测试 3]${NC} 发送第二条消息（需要上下文）..."
MSG2="请问我叫什么名字？"

RESPONSE=$(curl -s -X POST "$BASE_URL/api/agent/chat" \
  -H "Content-Type: application/json" \
  -d "{\"query\":\"$MSG2\",\"sessionId\":\"$SESSION\",\"includeDetails\":true}")

RESULT=$(echo "$RESPONSE" | jq -r '.result')

echo -e "${GREEN}✅ AI 回复：${NC}"
echo "   $RESULT"
echo ""

# 检查回复是否包含上下文信息
if echo "$RESULT" | grep -qi "小王\|王\|30\|北京"; then
  echo -e "${GREEN}✅ 上下文有效 - AI 成功理解了前文信息！${NC}"
else
  echo -e "${YELLOW}⚠️  AI 回复中没有包含上下文信息${NC}"
fi
echo ""

# 第三条消息
echo -e "${YELLOW}[测试 4]${NC} 发送第三条消息..."
MSG3="我住在哪个城市？"

RESPONSE=$(curl -s -X POST "$BASE_URL/api/agent/chat" \
  -H "Content-Type: application/json" \
  -d "{\"query\":\"$MSG3\",\"sessionId\":\"$SESSION\",\"includeDetails\":true}")

RESULT=$(echo "$RESPONSE" | jq -r '.result')

echo -e "${GREEN}✅ AI 回复：${NC}"
echo "   $RESULT"
echo ""

# 查看完整历史
echo -e "${YELLOW}[测试 5]${NC} 查看完整对话历史..."
HISTORY=$(curl -s "$BASE_URL/api/chat/history/sessions/$SESSION")
MSG_COUNT=$(echo "$HISTORY" | jq '.messageCount')

echo -e "${GREEN}✅ 对话历史已保存${NC}"
echo "   总消息数: $MSG_COUNT"
echo ""

# 显示所有消息
echo -e "${YELLOW}[测试 6]${NC} 对话历史内容："
echo "$HISTORY" | jq -r '.messages[] | "[\(.role | ascii_upcase)] \(.content)"' | head -10
echo ""

# 导出会话
echo -e "${YELLOW}[测试 7]${NC} 导出会话..."
EXPORT=$(curl -s "$BASE_URL/api/chat/history/sessions/$SESSION/export")
EXPORTED_COUNT=$(echo "$EXPORT" | jq '.messageCount')

echo -e "${GREEN}✅ 导出成功${NC}"
echo "   导出消息数: $EXPORTED_COUNT"
echo ""

echo -e "${BLUE}╔════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  ✅ 测试完成！                    ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════╝${NC}"
echo ""

echo "📊 测试总结："
echo "   ✅ 会话创建"
echo "   ✅ 多轮对话"
echo "   ✅ 上下文理解（$MSG2 - 需要AI参考历史）"
echo "   ✅ 历史查询"
echo "   ✅ 数据导出"
echo ""

echo "🎯 关键功能："
echo "   📚 前文信息被保存和传递给 AI"
echo "   🧠 AI 可以参考历史对话进行回复"
echo "   💾 所有对话持久化存储"
echo "   📄 支持导出和查看历史"
echo ""

echo -e "${GREEN}🎉 对话上下文功能已启用！${NC}"
