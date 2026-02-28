# 📋 Plan: Java 智能体快速入门版

## TL;DR
构建一个精简的 Java 智能体框架，包含 **3 层核心架构**（快速入门版本，1-2 周完成）：
1. **基础层** - DeepSeek API 服务集成
2. **工具层** - Tool Calling 和工具执行
3. **推理层** - 简化 ReACT 框架（思考→行动→观察）

核心设计：Agent Loop（智能体循环）→ 通过 LLM 生成思考和行动 → 执行工具 → 反馈观察结果 → 重复直到完成。

**目标**：快速体验智能体的核心能力，理解 Tool Calling 和 ReACT 的工作原理。

---

## 📁 **步骤 1: 初始化 Spring Boot 项目结构**

创建目录结构（精简版）：
```
agent0228/
├── pom.xml
├── src/main/java/com/agent/
│   ├── Agent0228Application.java          # 主启动类
│   │
│   ├── llm/                               # 第1层：基础层 - LLM 服务
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
│   │       ├── LLMService.java            # 统一接口
│   │       ├── impl/
│   │       │   └── DeepSeekService.java
│   │       └── LLMServiceFactory.java
│   │
│   ├── tool/                              # 第2层：工具层 - Tool Calling
│   │   ├── annotation/
│   │   │   └── Tool.java                  # @Tool 注解
│   │   ├── model/
│   │   │   ├── ToolDefinition.java
│   │   │   ├── ToolCall.java
│   │   │   └── ToolResult.java
│   │   ├── registry/
│   │   │   └── ToolRegistry.java          # 工具注册中心
│   │   ├── executor/
│   │   │   └── ToolExecutor.java          # 工具执行器
│   │   └── builtin/
│   │       └── BuiltInTools.java          # 内置工具：计算器、搜索等
│   │
│   ├── reasoning/                         # 第3层：推理层 - 简化 ReACT
│   │   ├── engine/
│   │   │   ├── ReasoningEngine.java       # 推理引擎核心
│   │   │   ├── ThoughtAction.java         # 思考和行动
│   │   │   └── ExecutionContext.java      # 执行上下文
│   │   └── prompt/
│   │       └── SystemPromptBuilder.java   # Prompt 构建
│   │
│   ├── controller/                        # REST 接口
│   │   └── ChatController.java
│   │
│   └── common/                            # 通用工具
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

## 📦 **步骤 2: 配置 Maven 依赖**

编辑 `pom.xml`，添加核心依赖（精简版）：
```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>3.0.0</version>
    </dependency>
    
    <!-- HTTP 客户端 -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.11.0</version>
    </dependency>
    
    <!-- JSON 处理 -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.15.2</version>
    </dependency>
    
    <!-- Lombok 简化代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.30</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- 日志 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-logging</artifactId>
    </dependency>
    
    <!-- 反射工具 - 用于工具注册 -->
    <dependency>
        <groupId>org.reflections</groupId>
        <artifactId>reflections</artifactId>
        <version>0.10.2</version>
    </dependency>
    
    <!-- 单元测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <version>3.0.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 🔧 **步骤 3: 构建第2层 - 工具层 (Tool Calling)**

**目标**：实现工具注册、工具调用、工具执行的完整系统

**3.1** 定义工具接口
- `ToolDefinition.java`：工具元数据（名称、描述、参数）
- `ToolCall.java`：工具调用请求
- `ToolResult.java`：工具执行结果
- `Parameter.java`：参数类型定义

**3.2** 实现工具注册中心
- `ToolRegistry.java`：维护所有可用工具
- 支持动态扫描 `@Tool` 注解的类
- 生成 OpenAI/GLM 兼容的工具描述

**3.3** 实现工具执行器
- `ToolExecutor.java`：通过反射执行工具方法
- 参数验证和类型转换
- 错误捕获和结果封装

**3.4** 内置工具集
```java
// 内置工具示例
@Tool(name = "web_search", description = "搜索网络信息")
public ToolResult webSearch(@Parameter(description = "搜索关键词") String query) {
    // 实现网络搜索
}

@Tool(name = "calculator", description = "数学计算")
public ToolResult calculate(@Parameter(description = "数学表达式") String expression) {
    // 实现计算
}
```

---

## 🧠 **步骤 4: 构建第3层 - 推理层 (ReACT 框架)**

**目标**：实现思考-行动-观察-反思的推理循环

**4.1** ReACT 循环实现
- `ReActLoop.java`：核心循环流程
  ```
  思考(Thought) → 行动(Action) → 观察(Observation) → 反思(Reflection) → 决策
  ```

**4.2** 推理引擎
- `ReasoningEngine.java`：协调推理过程
- `ThoughtAction.java`：思考和行动的数据结构
- `ExecutionStep.java`：每一步的执行记录

**4.3** 系统 Prompt 构建
- `SystemPromptBuilder.java`：动态构建 Prompt
  - 包含工具定义
  - 包含推理指导
  - 包含输出格式要求
- `PromptTemplate.java`：Prompt 模板管理

**4.4** ReACT Prompt 模板
```
You are an AI Agent. You have access to the following tools:
[TOOLS]

Use the following format:
Thought: Do I need to use a tool? Yes
Action: the action to take, should be one of [tool_names]
Action Input: the input to the action
Observation: the result of the action
... (this Thought/Action/Observation can repeat N times)
Thought: Do I now have enough information to answer the question without using more tools?
Final Answer: the final answer to the original input question

Question: {question}
Thought:
```

---

## 🧠 **步骤 5: 构建第4层 - 记忆层 (Memory & RAG)**

**目标**：实现对话历史、长期记忆、知识检索

**5.1** 记忆系统设计
- `ConversationHistory.java`：对话历史管理
- `ShortTermMemory.java`：短期记忆（当前对话）
- `LongTermMemory.java`：长期记忆（持久存储）

**5.2** 向量数据库集成
- `VectorDBConfig.java`：Milvus 或 Weaviate 配置
- `VectorDBService.java`：向量数据库操作
- 支持向量相似度搜索

**5.3** Embedding 服务
- `EmbeddingService.java`：文本向量化
- 选项：
  - 调用 OpenAI Embedding API
  - 使用开源模型（Sentence Transformers）
  - GLM Embedding

**5.4** 知识检索（RAG）
- `KnowledgeRetriever.java`：检索相关知识
- 实现：问题向量化 → 向量库搜索 → 返回相关文档

**5.5** 知识图谱（可选进阶）
- `KnowledgeGraphService.java`：构建和查询知识图
- 支持实体关系存储
- 用于复杂推理

---

## 🔄 **步骤 6: 构建第5层 - 协调层 (Workflow)**

**目标**：实现任务规划、执行计划、工作流编排

**6.1** 任务规划
- `TaskPlanner.java`：将用户需求分解为子任务
- `ExecutionPlan.java`：生成执行计划
- 利用 LLM 进行自适应规划

**6.2** 工作流执行
- `WorkflowExecutor.java`：按顺序/并行执行任务
- 支持任务依赖和条件分支
- 错误恢复和重试

**6.3** 任务队列
- `TaskQueue.java`：管理待执行任务
- 支持优先级队列
- 可集成 Redis 实现分布式队列

---

## 🤖 **步骤 7: 构建第6层 - 应用层 (Agent Implementation)**

**目标**：实现具体的智能体

**7.1** 智能体基类
- `Agent.java`：所有智能体的抽象基类
  ```java
  public abstract class Agent {
      protected LLMService llmService;
      protected ToolRegistry toolRegistry;
      protected MemoryService memoryService;
      protected ReasoningEngine reasoningEngine;
      
      public abstract String execute(String userQuery);
      public abstract void init();
  }
  ```

**7.2** 具体智能体实现
- `ChatAgent.java`：通用聊天机器人
- `SearchAgent.java`：信息搜索智能体
- `ResearcherAgent.java`：研究助手
- `CodeAgent.java`：代码生成和分析
- 自定义专域智能体

**7.3** 智能体管理
- `AgentManager.java`：管理多个智能体
- 支持智能体协作
- 上下文共享

---

## 🌐 **步骤 8: 构建 REST 接口和集成模块**

**8.1** REST Controller
```java
@PostMapping("/api/agent/chat")
public AgentResponse chat(@RequestBody ChatRequest request) {
    String response = agentManager.executeQuery(request.getQuery());
    return new AgentResponse(response);
}

@PostMapping("/api/agent/chat-with-context")
public AgentResponse chatWithContext(@RequestBody ChatWithContextRequest request) {
    // 支持上下文和历史传递
}
```

**8.2** 扩展集成模块
- `WebSearch.java`：Web 搜索工具
- `FileSystem.java`：文件系统访问
- `Calculator.java`：数学计算
- `Database.java`：数据库查询
- `CodeExecutor.java`：代码执行

---

## ⚙️ **步骤 9: 配置管理**

**9.1** 完整配置文件 (`application.yml`)
```yaml
spring:
  application:
    name: ai-agent-framework
  
  # 数据库配置
  datasource:
    url: jdbc:mysql://localhost:3306/agent_db?useSSL=false
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
  
  # Redis 缓存
  redis:
    host: localhost
    port: 6379
    timeout: 2000

# LLM 配置
llm:
  providers:
    deepseek:
      enabled: true
      api-key: ${DEEPSEEK_API_KEY:}
      base-url: https://api.deepseek.com
      model: deepseek-chat
      max-tokens: 4096
      temperature: 0.7
      
    glm:
      enabled: true
      api-key: ${GLM_API_KEY:}
      base-url: https://open.bigmodel.cn/api/paas/v4
      model: glm-4-plus
      max-tokens: 4096
      temperature: 0.75
      
    openai:
      enabled: false
      api-key: ${OPENAI_API_KEY:}
      base-url: https://api.openai.com/v1
      model: gpt-5.2
      max-tokens: 4096
      temperature: 0.7

# 向量数据库配置
vector-db:
  type: milvus  # 或 weaviate
  host: localhost
  port: 19530
  database: agent
  collection: knowledge

# Embedding 配置
embedding:
  provider: openai  # openai, glm, 或 local
  model: text-embedding-3-small
  dimension: 1536
  batch-size: 100

# 智能体配置
agent:
  max-iterations: 10     # ReACT 最大循环次数
  timeout: 300           # 超时时间（秒）
  enable-streaming: true # 是否启用流式输出

# 日志级别
logging:
  level:
    com.agent: DEBUG
    org.springframework: INFO
```

**9.2** 数据库初始化脚本
```sql
-- 对话历史表
CREATE TABLE conversation_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    content LONGTEXT,
    timestamp BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id)
);

-- 执行步骤记录表
CREATE TABLE execution_steps (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(255) NOT NULL,
    step_number INT,
    thought TEXT,
    action VARCHAR(255),
    action_input LONGTEXT,
    observation LONGTEXT,
    timestamp BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id)
);

-- 知识库表
CREATE TABLE knowledge_entities (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    entity_name VARCHAR(255) NOT NULL,
    entity_type VARCHAR(100),
    description LONGTEXT,
    embedding LONGBLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_name (entity_name)
);
```

---

## ✅ **步骤 10: 完整工作流验证**

**10.1** 单次测试流程
```bash
# 1. 启动向量数据库
docker run -d -p 19530:19530 -p 9091:9091 milvusdb/milvus

# 2. 启动 Redis
docker run -d -p 6379:6379 redis

# 3. 创建数据库
mysql -u root -p < init.sql

# 4. 设置环境变量
export DEEPSEEK_API_KEY=your_key
export GLM_API_KEY=your_key

# 5. 启动应用
mvn clean spring-boot:run
```

**10.2** API 测试
```bash
# 简单对话
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"query": "今天天气如何？", "agentType": "chat"}'

# 包含工具调用
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"query": "计算 100 + 200", "agentType": "chat"}'

# 包含知识检索
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"query": "Apple 公司最新财报怎么样？", "agentType": "search"}'

# 查看执行步骤
curl http://localhost:8080/api/agent/steps?sessionId=xxx
```

---

## 🎓 **架构设计决策**

| 层级 | 选择 | 原因 |
|---|---|---|
| **基础层** | DeepSeek/GLM/OpenAI | 支持多模型，降低依赖风险 |
| **工具层** | 注解 + 反射 + 工厂 | 易于扩展，动态加载 |
| **推理层** | ReACT 框架 | 业界标准，提高准确性 |
| **记忆层** | Milvus + MySQL | 向量搜索快速，关系数据可靠 |
| **协调层** | 任务队列 + 工作流 | 支持异步和复杂流程 |
| **应用层** | 抽象 + 中介模式 | 便于多智能体协作 |

---

## 📌 **分阶段实现路线**

### **第一阶段 - 基础阶段（1-2周）**
✅ 步骤 1-2：项目初始化和依赖配置
✅ 步骤 3-4：完成第1层（LLM 基础层）和第2层（工具层基础）

**目标**：能够调用 DeepSeek API 并执行简单工具

### **第二阶段 - 推理阶段（2-3周）**
✅ 步骤 5：完成第3层（ReACT 推理框架）

**目标**：实现思考-行动循环，支持多步推理

### **第三阶段 - 记忆阶段（2-3周）**
✅ 步骤 6：完成第4层（记忆和 RAG）

**目标**：支持上下文管理和知识检索

### **第四阶段 - 协调阶段（1-2周）**
✅ 步骤 7-8：完成第5和第6层

**目标**：任务分解和多智能体协作

### **第五阶段 - 优化阶段（持续）**
- 性能优化
- 监控告警
- 文档完善
- 真实场景验证

---

## 💡 **关键代码示例**

### ReACT 循环伪代码
```java
// 推理引擎核心逻辑
public AgentResponse executeQuery(String query) {
    ConversationContext context = new ConversationContext(query);
    
    for (int i = 0; i < maxIterations; i++) {
        // 1. 利用 LLM 生成思考和行动
        ThoughtAction decision = generateThought(context);
        context.addStep(decision);
        
        if (decision.isFinished()) {
            return createFinalResponse(context);
        }
        
        // 2. 执行选定的工具
        ToolResult result = toolExecutor.execute(decision.getAction());
        context.addObservation(result);
        
        // 3. 将结果反馈给 LLM，继续推理
    }
    
    return createFinalResponse(context);
}
```

### 工具定义示例
```java
@Tool(name = "web_search", description = "在网络上搜索信息")
public ToolResult webSearch(
    @Parameter(description = "搜索查询") String query,
    @Parameter(description = "结果数量") int limit
) {
    // 调用搜索 API
    List<SearchResult> results = searchEngine.search(query, limit);
    return new ToolResult(results);
}
```

---

## 🚀 **下一步行动**

1. 根据需求选择实现阶段
2. 从第一阶段开始，逐步完成每个步骤
3. 每个阶段完成后进行集成测试
4. 收集反馈并调整架构

**建议按照"第一阶段"先实现基础功能，验证框架可运行后再逐步添加高级功能。**
