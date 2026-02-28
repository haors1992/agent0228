# 📋 Java 智能体快速入门版计划

## 🎯 目标
在 1-2 周内快速构建一个可工作的智能体，包含：
- ✅ DeepSeek API 集成
- ✅ Tool Calling（工具调用）
- ✅ 简化 ReACT 推理循环

**核心体验**：用户输入 → 智能体思考 → 调用工具 → 反馈结果 → 重复直到完成

---

## 📁 项目结构（精简版）

```
agent0228/
├── pom.xml
├── src/main/java/com/agent/
│   ├── Agent0228Application.java
│   │
│   ├── llm/                               # 第1层：LLM 基础
│   │   ├── config/
│   │   │   ├── LLMConfig.java
│   │   │   └── LLMProperties.java
│   │   ├── model/
│   │   │   ├── dto/
│   │   │   │   ├── Message.java
│   │   │   │   ├── ChatRequest.java
│   │   │   │   └── ChatResponse.java
│   │   │   └── enums/
│   │   │       └── LLMProvider.java
│   │   └── service/
│   │       ├── LLMService.java
│   │       ├── impl/
│   │       │   └── DeepSeekService.java
│   │       └── LLMServiceFactory.java
│   │
│   ├── tool/                              # 第2层：工具系统
│   │   ├── annotation/
│   │   │   └── Tool.java
│   │   ├── model/
│   │   │   ├── ToolDefinition.java
│   │   │   ├── ToolCall.java
│   │   │   └── ToolResult.java
│   │   ├── registry/
│   │   │   └── ToolRegistry.java
│   │   ├── executor/
│   │   │   └── ToolExecutor.java
│   │   └── builtin/
│   │       └── BuiltInTools.java
│   │
│   ├── reasoning/                         # 第3层：推理引擎
│   │   ├── engine/
│   │   │   ├── ReasoningEngine.java
│   │   │   ├── ThoughtAction.java
│   │   │   └── ExecutionContext.java
│   │   └── prompt/
│   │       └── SystemPromptBuilder.java
│   │
│   ├── controller/
│   │   └── ChatController.java
│   │
│   └── common/
│       ├── exception/
│       │   └── AgentException.java
│       └── util/
│           └── JsonUtils.java
│
├── src/main/resources/
│   └── application.yml
│
└── README.md
```

---

## 📦 Maven 依赖（最小化）

```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>3.0.0</version>
    </dependency>
    
    <!-- OkHttp for API calls -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.11.0</version>
    </dependency>
    
    <!-- Jackson for JSON -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.15.2</version>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.30</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- Reflections for tool scanning -->
    <dependency>
        <groupId>org.reflections</groupId>
        <artifactId>reflections</artifactId>
        <version>0.10.2</version>
    </dependency>
</dependencies>
```

---

## 🔨 实现步骤

### **步骤 1: 项目初始化 + 配置**
- [ ] 创建 pom.xml 和项目结构
- [ ] 配置 application.yml
- [ ] 创建主启动类 Agent0228Application

### **步骤 2: 第1层 - LLM 基础服务**
- [ ] 实现 Message, ChatRequest, ChatResponse 数据模型
- [ ] 实现 LLMService 接口
- [ ] 实现 DeepSeekService（调用 DeepSeek API）
- [ ] 配置管理（LLMProperties, LLMConfig）

**目标**：能够通过代码调用 DeepSeek API

### **步骤 3: 第2层 - 工具系统**
- [ ] 定义 @Tool 注解
- [ ] 实现 ToolRegistry（扫描并注册工具）
- [ ] 实现 ToolExecutor（执行工具）
- [ ] 添加内置工具（Calculator、StringTools 等）

**目标**：定义工具 → 调用工具 → 获取结果

### **步骤 4: 第3层 - 推理引擎**
- [ ] 实现 ThoughtAction 数据结构
- [ ] 实现 ExecutionContext（维护对话上下文）
- [ ] 实现 SystemPromptBuilder（构建 ReACT Prompt）
- [ ] 实现 ReasoningEngine（Agent 循环核心）

**目标**：Agent 能进行多步推理和工具调用

### **步骤 5: REST 接口**
- [ ] 实现 ChatController
- [ ] 两个端点：
  - POST `/api/agent/chat` - 发起对话
  - GET `/api/agent/chat` - 查询历史（可选）

**目标**：通过 HTTP 与智能体交互

### **步骤 6: 测试验证**
- [ ] 本地测试（curl）
- [ ] 演示场景：
  - 简单问答
  - 工具调用
  - 多步推理

---

## 🧠 核心 ReACT 逻辑

```
用户问题输入
    ↓
生成 System Prompt（包含工具定义）
    ↓
发送给 DeepSeek LLM：
   "Thought: [智能体思考]
    Action: [选择工具]
    Action Input: [工具参数]"
    ↓
解析 LLM 响应
    ↓
执行工具 → 获取结果
    ↓
将结果反馈给 LLM：
   "Observation: [结果]"
    ↓
是否完成？
   YES → 返回最终答案
   NO → 继续循环（max 10 次）
```

---

## 📋 配置示例

```yaml
spring:
  application:
    name: ai-agent
  
  # 日志配置
logging:
  level:
    com.agent: DEBUG
    org.springframework: INFO

# LLM 配置
llm:
  deepseek:
    enabled: true
    api-key: ${DEEPSEEK_API_KEY}
    base-url: https://api.deepseek.com
    model: deepseek-chat
    max-tokens: 4096
    temperature: 0.7

# 智能体配置
agent:
  max-iterations: 10
  timeout: 300
```

---

## ✅ 验证清单

- [ ] 项目编译成功：`mvn clean compile`
- [ ] DeepSeek API 可调用
- [ ] 工具可注册和执行
- [ ] ReACT 循环正常工作
- [ ] REST API 可访问
- [ ] 完整的对话流程可运行

---

## 🚀 快速启动

```bash
# 1. 克隆项目
cd /Users/limengya/Work/IdeaProjects/agent0228

# 2. 设置环境变量
export DEEPSEEK_API_KEY=your_key_here

# 3. 运行应用
mvn spring-boot:run

# 4. 测试 API
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"query": "计算 100 + 200"}'

# 期望响应：
# {
#   "result": "300",
#   "steps": [...]
# }
```

---

## 📚 核心文件说明

| 文件 | 职责 |
|---|---|
| `DeepSeekService` | 调用 DeepSeek API |
| `ToolRegistry` | 扫描和管理工具 |
| `ToolExecutor` | 执行工具方法 |
| `ReasoningEngine` | Agent 循环逻辑 |
| `SystemPromptBuilder` | 构建 Prompt |
| `ChatController` | REST 接口 |

---

## 💡 关键实现原理

### ReACT Prompt 示例
```
You are an intelligent agent that can use tools to solve problems.

Available tools:
- calculator: Calculate mathematical expressions
  Input format: {"expression": "..."}
- string_tools: Manipulate strings
  Input format: {"operation": "...", "text": "..."}

When given a question, follow this format exactly:
Thought: [Your reasoning about what to do]
Action: [One of: calculator, string_tools]
Action Input: [The JSON input for the action]
Observation: [The result will be provided]
... (repeat Thought/Action/Observation as needed)
Thought: [Final analysis]
Final Answer: [Your final answer]

Question: {user_query}
Thought:
```

---

## 📌 下一步计划

完成快速入门版后：
1. 添加对话历史管理（内存存储）
2. 扩展内置工具库
3. 集成向量数据库（可选）
4. 部署到生产环境

**预期完成时间**：7-10 个工作日
