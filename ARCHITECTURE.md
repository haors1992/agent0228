# 架构详解

## 系统架构概览

```
┌─────────────────────────────────────────────────────────────────────┐
│                        API 层 (REST Endpoints)                      │
│                                                                     │
│  POST /api/agent/chat      GET /api/agent/health                  │
│  └─ ChatController.java                                           │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ↓
┌─────────────────────────────────────────────────────────────────────┐
│                   推理引擎层 (Reasoning Engine)                      │
│                                                                     │
│  ReasoningEngine.java                                              │
│  ├─ 初始化 ExecutionContext                                        │
│  ├─ 循环执行 ReACT 轮数                                            │
│  │   ├─ 构建系统提示 (SystemPromptBuilder)                         │
│  │   ├─ 调用 LLM 获取思考和行动 (LLMService)                      │
│  │   ├─ 解析 LLM 响应 (Thought/Action/ActionInput)                │
│  │   ├─ 如果 Action="finish" → 提取最终答案，结束循环            │
│  │   ├─ 否则 → 执行工具 (ToolExecutor)                           │
│  │   └─ 更新上下文 (ExecutionContext)                             │
│  └─ 返回完整的 ExecutionContext                                    │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                ────────────────────────────────
               │                                 │
               ↓                                 ↓
┌──────────────────────────────┐  ┌──────────────────────────────┐
│   LLM 服务层                 │  │   工具系统层                  │
│   (LLM Services)             │  │   (Tool System)              │
│                              │  │                              │
│ 1. DeepSeekService          │  │ 1. ToolRegistry              │
│    ├─ OkHttpClient          │  │    ├─ 自动扫描 @Tool 注解  │
│    ├─ Request 验证          │  │    ├─ 维护工具映射表        │
│    ├─ API 调用              │  │    └─ 提供工具列表          │
│    └─ Response 解析         │  │                              │
│                              │  │ 2. ToolExecutor             │
│ 2. LLMServiceFactory        │  │    ├─ 反射调用工具           │
│    └─ 获取 LLM 实例         │  │    ├─ 异常处理              │
│                              │  │    └─ 结果包装              │
│ 3. LLMProperties            │  │                              │
│    └─ 配置绑定              │  │ 3. BuiltInTools             │
│                              │  │    ├─ calculator()          │
│ 4. Message/ChatRequest      │  │    ├─ string_tools()       │
│    └─ 数据模型              │  │    └─ get_timestamp()      │
│                              │  │                              │
│ 扩展点:                       │  │ 扩展点:                       │
│ ├─ 添加 GLMService          │  │ └─ 在自己的 @Component     │
│ └─ 添加 OpenAIService       │  │    中用 @Tool 注解添加工具  │
└────────────────────────────────┴──────────────────────────────────┘
```

## 数据流向

### 请求完整流程

```
1️⃣  用户请求
    ┌─────────────────────────────────────┐
    │ POST /api/agent/chat                │
    │ {                                   │
    │   "query": "计算 100 + 200",       │
    │   "includeDetails": true             │
    │ }                                   │
    └────────────────┬────────────────────┘
                     │
2️⃣  ChatController.chat() 参数验证
    ├─ query 非空检查
    └─ 调用 reasoningEngine.execute(query)
                     │
3️⃣  ReasoningEngine.execute()
    ├─ 创建 ExecutionContext
    ├─ 获取 SystemPrompt
    │   └─ SystemPromptBuilder.buildSystemPrompt()
    │       └─ ToolRegistry.getToolsDescription()
    │           返回: "- calculator: 计算数学表达式\n- ..."
    │
    └─┬ 循环迭代 (1 到 maxIterations)
      │
      └─ 第 1 轮
         ├─ 构建 ChatRequest
         │  └─ messages: [system_prompt, user_query]
         │
         ├─ 调用 LLMService.chat(request)
         │  ├─ DeepSeekService 从 LLMServiceFactory 获取
         │  ├─ 使用 OkHttpClient 发送请求
         │  ├─ 添加 Authorization: Bearer <API_KEY>
         │  ├─ 发送到: https://api.deepseek.com/chat/completions
         │  └─ 返回 ChatResponse
         │
         ├─ 解析 LLM 响应
         │  ├─ 正则提取 Thought: "..."
         │  ├─ 正则提取 Action: "calculator"
         │  └─ 正则提取 Action Input: "100 + 200"
         │
         ├─ 检查 Action
         │  ├─ 如果是 "finish" → 解析 Final Answer，跳到完成
         │  │
         │  └─ 否则 执行工具
         │      │
         │      └─ ToolExecutor.execute("calculator", "100 + 200")
         │         ├─ ToolRegistry.hasTool("calculator") 检查
         │         ├─ ToolRegistry.getToolMethod("calculator")
         │         ├─ ToolRegistry.getToolBean(...)
         │         ├─ 通过反射调用:
         │         │   BuiltInTools.calculator("100 + 200")
         │         ├─ 捕获返回值: "300"
         │         ├─ 测量执行时间
         │         └─ 返回 ToolResult.success("calculator", "300")
         │
         ├─ 更新 ExecutionContext
         │  ├─ addMessage() - 添加 LLM 响应
         │  ├─ addThoughtAction() - 记录思考和行动
         │  └─ addToolResult() - 记录工具结果
         │
         └─ 第 2 轮
            ├─ 准备消息 (包含前一轮的观察): "Observation: 300"
            ├─ 调用 LLM (同上)
            ├─ LLM 响应: "Final Answer: 300"
            ├─ 解析发现 Action = "finish"
            ├─ 提取 Final Answer: "300"
            ├─ 设置 context.finish("300")
            └─ 返回 context (isComplete=true)
                     │
4️⃣  ChatController 构建响应
    ├─ 提取 finalAnswer
    ├─ 计算耗时: now - startTime
    ├─ 构建 response Map
    │  ├─ result: "300"
    │  ├─ iterations: 2
    │  ├─ duration_ms: 2345
    │  ├─ is_complete: true
    │  └─ (可选) steps / tool_results (如果 includeDetails=true)
    │
    └─ 返回 200 OK
                     │
5️⃣  客户端接收
    ┌─────────────────────────────────────┐
    │ HTTP 200 OK                         │
    │ {                                   │
    │   "result": "300",                  │
    │   "iterations": 2,                  │
    │   "duration_ms": 2345,              │
    │   "is_complete": true,              │
    │   "steps": [...],                   │
    │   "tool_results": [...]             │
    │ }                                   │
    └─────────────────────────────────────┘
```

## 核心组件交互

### 推理循环的状态变化

```
ExecutionContext 状态转移:

初始状态
│ new ExecutionContext("计算 100 + 200")
│ ├─ userQuery: "计算 100 + 200"
│ ├─ messages: []
│ ├─ thoughtActions: []
│ ├─ toolResults: []
│ ├─ currentIteration: 0
│ └─ isComplete: false
│
↓ 第 1 轮迭代 (iteration 1)
│ addMessage(user_query)
│ addThoughtAction(thought="...", action="calculator", actionInput="100 + 200")
│ addToolResult(success=true, result="300")
│ currentIteration: 1
│ isComplete: false
│
↓ 第 2 轮迭代 (iteration 2)
│ addMessage("Observation: 300")
│ addThoughtAction(thought="...", action="finish", isFished=true)
│ finish(finalAnswer="300")
│ ├─ currentIteration: 2
│ ├─ isComplete: true
│ ├─ finalAnswer: "300"
│ └─ endTime: System.currentTimeMillis()
│
↓ 完成状态
完整的执行历史记录，可用于分析和调试
```

## 工具系统工作流程

```
启动时 (Spring Boot 初始化):

1. ToolRegistry 被创建 (@Component)
2. @PostConstruct 触发
   │
   ├─ 获取所有 Spring Beans
   │  └─ 在启动日志中看到:
   │     Registering tools from: com.agent.tool.impl.BuiltInTools
   │
   ├─ 对每个 Bean，扫描所有方法
   │  └─ 查找 @Tool 注解
   │
   ├─ 找到 @Tool 方法
   │  ├─ calculator(String expression)
   │  ├─ string_tools(String input)
   │  └─ get_timestamp(String input)
   │
   ├─ 为每个工具创建 ToolDefinition
   │  └─ name, description, parameters
   │
   ├─ 存储映射
   │  ├─ toolMethods: {"calculator" -> Method}
   │  ├─ toolBeans: {"calculator" -> BuiltInTools 实例}
   │  └─ tools: {"calculator" -> ToolDefinition}
   │
   └─ 在日志中确认:
      [DEBUG] Registered tool: calculator
      [DEBUG] Registered tool: string_tools
      [DEBUG] Registered tool: get_timestamp


运行时 (API 调用):

1. SystemPromptBuilder.buildSystemPrompt()
   │
   ├─ 获取 ToolRegistry.getToolsDescription()
   │  └─ 返回所有已注册工具的列表
   │
   └─ 包含在系统提示中:
      "Available tools:
       - calculator: 计算数学表达式
       - string_tools: 执行字符串操作
       - get_timestamp: 获取当前时间戳"

2. LLM 看到工具列表，根据需要调用工具

3. ReasoningEngine 解析 Action: "calculator"

4. ToolExecutor.execute("calculator", "100 + 200")
   │
   ├─ 检查: ToolRegistry.hasTool("calculator") ✓
   │
   ├─ 获取: 
   │  ├─ Method = ToolRegistry.getToolMethod("calculator")
   │  └─ Bean = ToolRegistry.getToolBean("calculator")
   │
   ├─ 反射调用:
   │  └─ method.invoke(bean, "100 + 200")
   │
   ├─ 捕获结果和异常
   │
   └─ 返回 ToolResult
      ├─ success: true
      ├─ result: "300"
      └─ executionTimeMs: 12
```

## 配置和依赖注入

```
Spring 依赖注入图:

application.yml
│
├─ LLMProperties (@ConfigurationProperties)
│  ├─ deepseek.api-key
│  ├─ deepseek.base-url
│  └─ deepseek.timeout
│
├─ LLMConfig (@Configuration)
│  └─ ObjectMapper Bean (@Bean)
│
├─ DeepSeekService (@Component)
│  ├─ @Autowired LLMProperties
│  ├─ @Autowired ObjectMapper
│  └─ 创建 OkHttpClient
│
├─ LLMServiceFactory (@Component)
│  └─ @Autowired DeepSeekService
│
├─ BuiltInTools (@Component)
│  ├─ @Tool calculator(String)
│  ├─ @Tool string_tools(String)
│  └─ @Tool get_timestamp(String)
│
├─ ToolRegistry (@Component)
│  └─ @PostConstruct 扫描 @Tool 方法
│
├─ ToolExecutor (@Component)
│  └─ @Autowired ToolRegistry
│
├─ SystemPromptBuilder (@Component)
│  └─ @Autowired ToolRegistry
│
├─ ReasoningEngine (@Component)
│  ├─ @Autowired LLMService
│  ├─ @Autowired ToolExecutor
│  └─ @Autowired SystemPromptBuilder
│
└─ ChatController (@RestController)
   └─ @Autowired ReasoningEngine
```

## 错误处理流程

```
可能的错误场景:

1. API 密钥无效
   │
   ├─ DeepSeekService.chat() 捕获
   ├─ 返回 null 或 exception
   ├─ ReasoningEngine 检测到失败
   ├─ 记录错误日志
   └─ context.finish("调用 LLM 失败: ...")

2. 工具不存在
   │
   ├─ ToolExecutor.execute() 检查
   ├─ ToolRegistry.hasTool() 返回 false
   ├─ 返回 ToolResult.failure()
   └─ ReasoningEngine 继续循环或返回错误

3. 工具执行异常
   │
   ├─ method.invoke() 抛出异常
   ├─ ToolExecutor 捕获并包装
   └─ 返回 ToolResult.failure(toolName, error)

4. 网络超时
   │
   ├─ OkHttp 配置的 timeout
   ├─ DeepSeekService catch SocketTimeoutException
   ├─ ReasoningEngine catch exception
   ├─ 记录详细错误信息
   └─ 返回错误响应

5. 超过最大迭代数
   │
   ├─ ReasoningEngine 检查 iteration < maxIterations
   ├─ 循环退出
   └─ 返回当前状态 (可能未完成)

所有错误都会:
├─ 被记录到 log
├─ 返回给客户端
└─ 包含在 ExecutionContext 中
```

## 扩展点

### 添加新 LLM 提供商

```
1. 创建 XxxService implements LLMService
2. 实现 chat() 方法
3. 在 LLMServiceFactory 中添加条件
4. 在 application.yml 中添加配置
5. 可选: 创建 @ConfigurationProperties 类

示例:
@Service
public class GLMService implements LLMService {
    @Override
    public ChatResponse chat(ChatRequest request) {
        // 调用 GLM API
        // 转换为 ChatResponse
        // 返回
    }
}
```

### 添加新工具

```
1. 创建 @Component 类 (或在现有类中)
2. 添加 @Tool 注解的方法
3. 无需修改 ToolRegistry (自动扫描)
4. 重启应用

示例:
@Component
public class CustomTools {
    @Tool(
        name = "weather",
        description = "查询天气",
        paramsDescription = "城市名称"
    )
    public String getWeather(String city) {
        // 实现逻辑
        return "result";
    }
}
```

## 性能优化机会

```
当前实现的优化:
├─ OkHttp connection pooling
├─ 可配置超时
├─ 正则表达式预编译 (潜在)
└─ Stream API 用于集合处理

潜在的进一步优化:
├─ 缓存 LLMProperties (现已缓存)
├─ 缓存 ToolRegistry 的工具列表
├─ 异步 API 调用 (CompletableFuture)
├─ 流式响应 (Server-Sent Events)
├─ 批量工具调用
└─ 工具结果缓存 (如 get_timestamp)
```

---

这个架构设计兼顾了**清晰性**（易于理解）和**可扩展性**（易于添加新功能）。
对于初学者来说，重点理解的是三层的职责分离和数据流向。
