# 快速开始指南 (5 分钟入门)

## 第 1 步：获取 API 密钥（2 分钟）

1. 访问 https://platform.deepseek.com/
2. 注册/登录账户
3. 进入 API 页面，获取 API 密钥
4. 复制密钥，格式为 `sk-xxxxxxxxxxxxxxxx`

## 第 2 步：设置环境变量（1 分钟）

**macOS/Linux：**
```bash
export DEEPSEEK_API_KEY="your-api-key-here"
```

**Windows (PowerShell)：**
```powershell
$env:DEEPSEEK_API_KEY="your-api-key-here"
```

## 第 3 步：启动应用（2 分钟）

```bash
# 进入项目目录
cd agent0228

# 启动应用
mvn spring-boot:run
```

看到以下输出表示成功：
```
Started Agent0228Application in 2.345 seconds
```

## 第 4 步：测试 API（无需任何代码）

在另一个终端运行：

### 测试 1：计算器
```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"query": "计算 100 + 200"}'
```

**预期输出：**
```json
{
  "result": "300",
  "iterations": 1,
  "duration_ms": 1234,
  "is_complete": true
}
```

### 测试 2：字符串操作
```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"query": "把 hello 转换为大写"}'
```

### 测试 3：多步骤任务
```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"query": "计算 20 * 5，然后把结果转换为大写英文"}' | jq
```

### 测试 4：健康检查
```bash
curl http://localhost:8080/api/agent/health
```

## 🎯 发生了什么？

整个过程遵循这个流程：

```
你的查询：计算 100 + 200
         ↓
DeepSeek LLM 进行思考
  Thought: 用户要求计算 100 + 200
  Action: calculator
  Action Input: 100 + 200
         ↓
系统执行 calculator 工具
  结果: 300
         ↓
DeepSeek LLM 生成最终答案
  Final Answer: 100 + 200 = 300
         ↓
返回给用户
```

## 📊 理解输出

响应包含：
- `result`: 最终答案
- `iterations`: 完成该任务用的轮数
- `duration_ms`: 总耗时（毫秒）
- `is_complete`: 是否成功完成

添加 `"includeDetails": true` 可获取每步的详细信息：

```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "query":"计算 50 * 3", 
    "includeDetails": true
  }' | jq '.steps'
```

输出会显示完整的思考过程。

## 🛠️ 后续步骤

1. **添加新工具**：在 `src/main/java/com/agent/tool/` 下创建新的 `@Tool` 方法
2. **自定义提示词**：修改 `SystemPromptBuilder.java` 中的提示模板
3. **性能优化**：调整 `application.yml` 中的 `max-iterations` 和 `timeout`

## 🚨 常见问题

### 问题：API 密钥无效
**解决**：检查 API 密钥是否正确，是否以 `sk-` 开头

### 问题：连接超时
**解决**：检查网络，确保能访问 https://api.deepseek.com

### 问题：工具找不到
**解决**：确保新工具类被 `@Component` 注解，方法被 `@Tool` 注解

## 📚 核心代码位置

- **LLM 调用**：`com.agent.llm.service.DeepSeekService`
- **工具管理**：`com.agent.tool.registry.ToolRegistry`
- **推理循环**：`com.agent.reasoning.engine.ReasoningEngine`
- **REST 接口**：`com.agent.controller.ChatController`

## ✨ 下一步研究

完成快速开始后，建议深入学习：

1. 阅读 [README.md](README.md) 了解完整架构
2. 研究 `ReasoningEngine.java` 理解 ReACT 循环
3. 学习如何在 `BuiltInTools.java` 中添加新工具
4. 探索 `SystemPromptBuilder.java` 调整 Agent 行为

祝学习愉快！🚀
