# AI Agent Quick Start - DeepSeek 集成

一个轻量级、易上手的智能体框架，采用 **3 层架构**（LLM + 工具系统 + 推理引擎），结合 DeepSeek API 实现 ReACT 推理循环。

## 🎯 项目特点

- **快速入门**：3 层极简架构，代码清晰易懂
- **即插即用**：Spring Boot 框架，开箱即用
- **工具扩展**：基于注解的工具系统，新增工具无需修改核心代码
- **完整循环**：实现了 ReACT（Reasoning + Acting）推理模式
- **开源友好**：采用 MIT 许可证

## 📋 项目结构

```
✅ 第 1 层 - LLM 基础层（com.agent.llm.）
   ├── Message.java              # 对话消息 DTO
   ├── ChatRequest.java          # LLM 请求封装
   ├── ChatResponse.java         # LLM 响应封装
   ├── LLMProvider.java          # 支持的 LLM 提供商
   ├── LLMProperties.java        # 配置绑定
   ├── LLMService.java           # 服务接口
   ├── DeepSeekService.java      # DeepSeek 实现
   ├── LLMServiceFactory.java    # 工厂模式
   └── LLMConfig.java            # Spring 配置

✅ 第 2 层 - 工具系统（com.agent.tool.）
   ├── @Tool                     # 工具注解
   ├── ToolDefinition.java       # 工具定义
   ├── ToolCall.java             # 工具调用请求
   ├── ToolResult.java           # 工具执行结果
   ├── ToolRegistry.java         # 工具注册表（自动扫描）
   ├── ToolExecutor.java         # 工具执行器
   └── BuiltInTools.java         # 内置工具集
       ├── calculator()          # 计算器
       ├── string_tools()        # 字符串操作
       └── get_timestamp()       # 时间戳

✅ 第 3 层 - 推理引擎（com.agent.reasoning.）
   ├── ThoughtAction.java        # 思考-行动对
   ├── ExecutionContext.java     # 执行上下文
   ├── SystemPromptBuilder.java  # 系统提示生成
   └── ReasoningEngine.java      # 主推理循环（ReACT）

✅ 控制层（REST API）
   ├── ChatController.java       # /api/agent/chat 端点
   └── CorsConfig.java           # CORS 跨域配置

✅ 前端层
   └── src/main/resources/static/index.html  # 现代化 Web 对话界面
```

## 🚀 快速开始

### 前置条件

- **Java 8+** (推荐 Java 8-11)
- **Maven 3.6+**
- **DeepSeek API 密钥** (可从 https://platform.deepseek.com 申请)

### 安装步骤

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd agent0228
   ```

2. **配置 DeepSeek API 密钥**
   
   **方式 A：环境变量（推荐）**
   ```bash
   export DEEPSEEK_API_KEY="your-api-key-here"
   ```

   **方式 B：修改配置文件**
   编辑 `src/main/resources/application.yml`：
   ```yaml
   llm:
     deepseek:
       api-key: "your-api-key-here"
   ```

3. **编译项目**
   ```bash
   mvn clean package -DskipTests
   ```

4. **启动应用**

   **方式 A：Maven 启动（推荐开发环境）**
   ```bash
   export DEEPSEEK_API_KEY="your-api-key-here"
   mvn spring-boot:run
   ```

   **方式 B：JAR 启动（推荐生产环境）**
   ```bash
   export DEEPSEEK_API_KEY="your-api-key-here"
   java -jar target/agent0228-1.0.0.jar
   ```

   **方式 C：IDE 调试模式（推荐）**
   
   在 IntelliJ IDEA 或 VS Code 中：
   - 设置断点（点击代码行号左边）
   - Run → Debug 'Agent0228Application'
   - 应用会在断点处暂停，可以逐步执行和检查变量

   **方式 D：JDWP 远程调试（命令行）**
   
   1. 先编译：
   ```bash
   mvn clean package -DskipTests
   ```
   
   2. 启动应用（暂停等待调试器）：
   ```bash
   export DEEPSEEK_API_KEY="your-api-key-here"
   java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 \
     -jar target/agent0228-1.0.0.jar
   ```
   
   3. 在 IDE 中连接远程调试：
      - **IntelliJ**: Run → Edit Configurations → Remote → localhost:5005
      - **VS Code**: 使用 Java Debug 扩展配置远程调试
   
   **参数说明：**
   - `suspend=y` - 启动时暂停（等待调试器连接）
   - `suspend=n` - 立即启动，调试器可随时连接
   - `address=5005` - 调试服务监听的端口号

   应用将在 `http://localhost:8080` 启动

## 🌐 访问方式

### Web 前端界面（推荐）

启动应用后，直接访问：
```
http://localhost:8080/
```

**前端特性：**
- 💬 实时对话界面，支持消息历史
- 🧠 显示完整的 AI 推理过程
- 🔧 展示工具调用详情
- ⏱️ 显示执行时间和迭代轮数
- 📱 响应式设计，支持手机访问
- ⌨️ 支持 Enter 快速发送

### REST API 调用（高级）

如果想通过 API 集成到其他应用，可以直接调用 REST 端点。

## 💬 API 使用示例

### 1. 简单查询

**请求：**
```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "query": "请计算 100 + 200 的结果"
  }'
```

**响应：**
```json
{
  "result": "100 + 200 = 300",
  "iterations": 2,
  "duration_ms": 1234,
  "is_complete": true
}
```

### 2. 详细步骤查询

**请求：**
```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "query": "计算 50 * 3 并将结果转换为大写字母表示",
    "includeDetails": true
  }'
```

**响应：**
```json
{
  "result": "最终答案：150（ONE HUNDRED FIFTY）",
  "iterations": 3,
  "duration_ms": 2456,
  "is_complete": true,
  "steps": [
    {
      "thought": "用户要求计算 50 * 3",
      "action": "calculator",
      "action_input": "50 * 3"
    },
    {
      "thought": "计算结果是 150，现在需要转换为大写",
      "action": "string_tools",
      "action_input": "upper:one hundred fifty"
    },
    {
      "thought": "完成了所有任务",
      "action": "finish",
      "action_input": ""
    }
  ],
  "tool_results": [
    {
      "tool_name": "calculator",
      "result": "150",
      "success": true,
      "execution_time_ms": 45,
      "error": ""
    },
    {
      "tool_name": "string_tools",
      "result": "ONE HUNDRED FIFTY",
      "success": true,
      "execution_time_ms": 23,
      "error": ""
    }
  ]
}
```

### 3. 健康检查

**请求：**
```bash
curl http://localhost:8080/api/agent/health
```

**响应：**
```json
{
  "status": "UP"
}
```

## 🔧 配置说明

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  application:
    name: ai-agent-quickstart
  profiles:
    active: dev
  # 允许循环依赖（用于 Tool 注解）
  main:
    allow-circular-references: true

# LLM 配置
llm:
  deepseek:
    enabled: true
    api-key: ${DEEPSEEK_API_KEY:}              # 从环境变量读取（推荐）
    base-url: https://api.deepseek.com         # DeepSeek API 地址
    model: deepseek-chat                       # 使用的模型
    max-tokens: 4096                           # 最大 token 数
    temperature: 0.7                           # 创意度（0-1）
    timeout: 30                                # 超时秒数

# 智能体配置
agent:
  max-iterations: 10                           # 最多推理轮数
  timeout: 300                                 # 总超时秒数
  enable-streaming: false                      # 流式响应

server:
  port: 8080                                   # 服务端口

logging:
  level:
    root: INFO
    com.agent: DEBUG                           # 日志级别
```

### 环境变量配置（推荐方式）

**Bash/Zsh（Linux/macOS）：**
```bash
export DEEPSEEK_API_KEY="sk-your-api-key-here"
```

**PowerShell（Windows）：**
```powershell
$env:DEEPSEEK_API_KEY="sk-your-api-key-here"
```

**CMD（Windows）：**
```cmd
set DEEPSEEK_API_KEY=sk-your-api-key-here
```

**Docker：**
```bash
docker run -e DEEPSEEK_API_KEY="sk-your-api-key-here" -p 8080:8080 agent0228
```

## 🛠️ 如何添加新工具

### 示例：添加天气查询工具

1. **创建新工具方法**
   
   在 `com.agent.tool.impl.CustomTools` 中：
   
   ```java
   @Component
   public class CustomTools {
       
       @Tool(
           name = "weather_query",
           description = "查询指定城市的天气信息",
           paramsDescription = "城市名称，例如：北京、上海、深圳"
       )
       public String queryWeather(String city) {
           // 实现天气查询逻辑
           // ...
           return "北京：晴朗，温度 25°C";
       }
   }
   ```

2. **启动应用**
   
   工具会自动被 `ToolRegistry` 扫描并注册，无需修改其他代码

3. **在查询中使用**
   
   ```bash
   curl -X POST http://localhost:8080/api/agent/chat \
     -H "Content-Type: application/json" \
     -d '{"query": "北京现在的天气怎么样？"}'
   ```

## 📊 工作流程图

```
用户查询
   ↓
ReasoningEngine (推理引擎)
   ├─→ 构建系统提示 (SystemPromptBuilder)
   │    └─→ 获取可用工具列表 (ToolRegistry)
   │
   ├─→ 调用 LLM 获取思考和行动 (DeepSeekService)
   │    └─→ 发送请求到 DeepSeek API
   │
   ├─→ 解析 LLM 响应
   │    ├─ 提取 Thought（思考过程）
   │    ├─ 提取 Action（要执行的工具）
   │    └─ 提取 Action Input（工具参数）
   │
   ├─→ 执行工具 (ToolExecutor)
   │    ├─→ 查找工具 (ToolRegistry)
   │    ├─→ 通过反射调用工具方法
   │    └─→ 返回执行结果
   │
   ├─→ 更新执行上下文 (ExecutionContext)
   │    └─→ 保存思考、行动和观察结果
   │
   ├─→ 循环判断
   │    ├─ 如果 Action = "finish" → 返回最终答案
   │    ├─ 如果达到最大迭代数 → 返回当前结果
   │    └─ 否则 → 继续下一轮循环
   │
   └─→ 返回最终答案给用户
```

## 🧪 内置工具详解

### 1. 计算器 (`calculator`)

**功能**：计算数学表达式

**调用示例**：
```
Action: calculator
Action Input: 100 + 50 * 2
结果: 200
```

**支持的操作**：`+`, `-`, `*`, `/`, `%`, `^` 等

### 2. 字符串工具 (`string_tools`)

**功能**：字符串操作

**格式**：`operation:text`

**支持的操作**：
- `upper:hello` → `HELLO`
- `lower:HELLO` → `hello`
- `reverse:hello` → `olleh`
- `length:hello` → `5`
- `trim:  hello  ` → `hello`

### 3. 时间戳 (`get_timestamp`)

**功能**：获取当前时间戳

**调用示例**：
```
Action: get_timestamp
Action Input: (任意文本)
结果: 1704067200000
```

## 📝 日志和调试

### 启用详细日志

编辑 `src/main/resources/application.yml`：

```yaml
logging:
  level:
    root: INFO
    com.agent: DEBUG              # 项目内部日志
    org.springframework: DEBUG     # Spring 框架日志
    org.springframework.web: TRACE # HTTP 请求追踪
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/app.log            # 输出到文件
    max-size: 10MB
    max-history: 30
```

### 应用启动日志示例

```
[INFO] Started Agent0228Application in 2.345 seconds
[DEBUG] Registered tool: calculator
[DEBUG] Registered tool: string_tools
[DEBUG] Registered tool: get_timestamp
[DEBUG] Starting agent reasoning for query: 计算 100 + 200
[DEBUG] Iteration 1/10
[DEBUG] LLM response: Thought: I need to calculate 100 + 200...
[DEBUG] Tool execution: calculator with input '100 + 200'
[DEBUG] Tool result: 300
```

### IDE 集成调试

**IntelliJ IDEA / Android Studio（推荐）：**
1. 在要调试的代码行，点击左边行号区域设置断点（红色圆点）
2. 顶部菜单：Run → Debug 'Agent0228Application'
3. 应用启动后，执行会在断点处暂停
4. 在 Debug 面板中可以：
   - 查看局部变量和对象状态
   - 逐行执行代码（Step Over）
   - 进入方法内部（Step Into）
   - 继续执行（Resume）
5. 鼠标悬停在变量上可查看当前值

**VS Code：**
1. 安装 Extension Pack for Java 扩展
2. 创建或编辑 `.vscode/launch.json`：
   ```json
   {
     "version": "0.2.0",
     "configurations": [
       {
         "type": "java",
         "name": "Debug Spring Boot",
         "request": "launch",
         "mainClass": "com.agent.Agent0228Application",
         "projectName": "agent0228",
         "cwd": "${workspaceFolder}",
         "console": "integratedTerminal",
         "env": {
           "DEEPSEEK_API_KEY": "your-api-key-here"
         }
       }
     ]
   }
   ```
3. 在代码中设置断点（点击行号左边）
4. 按 F5 或点击 Run and Debug 的绿色运行按钮启动调试

## 🔐 安全建议

1. **API 密钥管理**
   - 永远不要在代码中硬编码 API 密钥
   - 使用环境变量或密钥管理服务
   - 定期更换 API 密钥

2. **请求验证**
   - 输入验证和清理
   - 请求速率限制
   - 超时控制

3. **工具权限**
   - 添加工具时考虑安全影响
   - 不要暴露危险的系统命令
   - 验证工具输入参数

## 📚 学习资源

### 推荐阅读

1. **ReACT 论文**: [Reasoning + Acting in Language Models](https://arxiv.org/abs/2210.03629)
2. **Chain-of-Thought**: [Chain-of-Thought Prompting Elicits Reasoning in Large Language Models](https://arxiv.org/abs/2201.11903)
3. **DeepSeek 文档**: https://platform.deepseek.com/docs

### 进阶功能（待实现）

- [ ] 多轮对话历史存储
- [ ] 用户会话管理
- [ ] 工具权限控制
- [ ] 向量数据库集成（用于知识库）
- [ ] 流式响应支持
- [ ] 代理调度和编排
- [ ] 监控和指标收集

## 🐛 故障排除

### 问题 1：连接超时

**症状**：`SocketTimeoutException`

**解决方案**：
1. 检查网络连接
2. 增加超时时间（在 `application.yml` 中修改 `timeout: 60`）
3. 检查 DeepSeek API 服务状态（访问 https://platform.deepseek.com）

### 问题 2：API 密钥未配置

**症状**：`DeepSeek API key is not configured`

**解决方案**：
```bash
# 检查环境变量
echo $DEEPSEEK_API_KEY

# 如果为空，设置环境变量
export DEEPSEEK_API_KEY="your-api-key-here"

# 然后重新启动应用
mvn spring-boot:run
```

### 问题 3：API 密钥错误

**症状**：`401 Unauthorized` 或 `403 Forbidden`

**解决方案**：
1. 检查 API 密钥是否正确，确认以 `sk-` 开头
2. 访问 https://platform.deepseek.com/account 验证密钥
3. 检查密钥是否过期或被禁用
4. 确保使用 `export` 或 `set` 正确设置环境变量

### 问题 4：端口已占用

**症状**：`Port already in use: 8080`

**解决方案**：
```bash
# macOS/Linux：找出占用端口的进程
lsof -i :8080

# 强制终止进程
kill -9 <PID>

# 或者修改配置文件，使用其他端口
# 在 application.yml 中修改：
server:
  port: 8081
```

### 问题 5：工具找不到

**症状**：`Tool not found: xxx`

**解决方案**：
1. 确认工具类被 `@Component` 注解
2. 确认工具方法被 `@Tool` 注解
3. 检查工具名称拼写（区分大小写）
4. 查看启动日志中的 "Registered tool" 记录
5. 确认工具方法的返回类型是 `String`

### 问题 6：前端无法访问后端

**症状**：前端请求返回 CORS 错误

**解决方案**：
CORS 配置已内置在 `CorsConfig.java` 中，允许所有来源访问。如需进一步配置：

```java
// 修改 src/main/java/com/agent/config/CorsConfig.java
registry.addMapping("/**")
    .allowedOrigins("http://localhost:3000")  // 限制具体域名
    .allowedMethods("*")
    .allowedHeaders("*");
```

### 问题 7：断点调试不工作

**症状**：使用 `mvn spring-boot:run` 启动后，IDE 中的断点没有触发

**根本原因**：
- `mvn spring-boot:run -Dspring-boot.run.arguments="--debug"` **只是启用调试日志**，不启动 Java 调试器
- `--debug` 参数会让应用打印更多 DEBUG 级别的日志，不会让应用进入断点

**解决方案**：

**方案 A（推荐）：IDE 中直接调试**
1. IntelliJ IDEA：Run → Debug 'Agent0228Application'
2. VS Code：F5 或点击 Run and Debug 的绿色运行按钮
3. 在代码行号左边点击设置断点
4. 应用会在断点处自动暂停

**方案 B：命令行远程调试**
1. 编译：`mvn clean package -DskipTests`
2. 启动应用（暂停等待调试器）：
   ```bash
   export DEEPSEEK_API_KEY="your-api-key"
   java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 \
     -jar target/agent0228-1.0.0.jar
   ```
3. IDE 中配置连接到 `localhost:5005`

**JDWP 参数说明：**
| 参数 | 含义 | 常用值 |
|------|------|--------|
| `transport` | 传输方式 | `dt_socket`（Socket）/ `dt_shmem`（共享内存） |
| `server` | 作为调试服务器 | `y`（是）/ `n`（否） |
| `suspend` | 启动时是否暂停 | `y`（暂停等待调试器）/ `n`（立即启动） |
| `address` | 监听地址和端口 | `5005`（端口号）/ `localhost:5005` |

## 📄 许可证

MIT License - 详见 LICENSE 文件

## 🚀 下一步

### 本地开发
1. Fork 认真看看，这是开源项目示例
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交修改 (`git commit -m 'Add amazing feature'`)
4. 推送分支 (`git push origin feature/amazing-feature`)
5. 提出 Pull Request

### 扩展建议
- [ ] 添加更多内置工具（Web 爬虫、文件操作等）
- [ ] 实现多轮对话历史存储
- [ ] 集成向量数据库（用于知识库检索）
- [ ] 支持流式响应（SSE）
- [ ] 添加用户认证和授权
- [ ] 部署到云平台（阿里云、AWS、Azure）
- [ ] 性能优化和监控指标

## 📞 联系方式

如有问题，欢迎反馈和讨论！

---

**最后更新**: 2026-02-28
**当前版本**: 1.0.0

**获取 DeepSeek API Key**: https://platform.deepseek.com

**相关资源**:
- 🔗 [DeepSeek 官方文档](https://platform.deepseek.com/docs)
- 📚 [ReACT 论文](https://arxiv.org/abs/2210.03629)
- 🧠 [Chain-of-Thought Prompting](https://arxiv.org/abs/2201.11903)
- 🎓 [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
