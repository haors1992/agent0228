全网最火的10个OpenClaw应用场景

# STEP 1：强化工具定义系统 - 完整实现指南

## 📝 实现总结

### 🎯 目标
强化工具定义系统，使 LLM 能够更准确地理解和调用工具，减少调用错误，支持自动重试和超时控制。

### ✅ 完整的实现清单

#### 1️⃣ **增强 @Tool 注解** ✅
**文件**: `com.agent.tool.annotation.Tool`

**新增字段**:
```java
// 参数定义 - 使用 @ToolParam 数组替代简单字符串
ToolParam[] params() default {};

// 返回值描述
String returnDescription() default "";

// 使用示例 - 帮助 LLM 理解正确用法
String[] examples() default {};

// 重试配置
int maxRetries() default 2;  // 默认重试 2 次

// 权限管理
String requiredPermission() default "";

// 超时控制
long timeoutMs() default 30000;  // 默认 30 秒超时

// 标签分类 - 支持动态工具加载
String[] tags() default {};
```

**使用示例**:
```java
@Tool(
    name = "calculator",
    description = "执行数学计算",
    params = @ToolParam(
        name = "expression",
        type = "string",
        description = "数学表达式，如 '100 + 200'",
        example = "10 * (5 + 3)",
        pattern = "[0-9+\\-*/%().\\s]+"
    ),
    returnDescription = "计算结果",
    examples = {
        "Input: 100 + 200 * 2",
        "Output: 500"
    },
    maxRetries = 2,
    timeoutMs = 5000,
    tags = {"math", "calculation"}
)
public ToolResult calculator(String expression) { ... }
```

**优势**:
- ✅ 类型安全：参数校验在编译时进行
- ✅ IDE 智能提示：完全支持 IDE 自动完成
- ✅ 文档化：注解本身就是文档
- ✅ Schema 自动生成：无需手写 JSON Schema

---

#### 2️⃣ **创建 @ToolParam 注解** ✅
**文件**: `com.agent.tool.annotation.ToolParam`

**参数字段**:
| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `name` | String | 参数名 | `"query"` |
| `type` | String | 参数类型 | `"string"`, `"integer"`, `"array"` |
| `description` | String | 参数描述 | 清楚地说明参数含义 |
| `required` | boolean | 是否必需 | `true` / `false` |
| `defaultValue` | String | 默认值 | `"10"` |
| `enum_` | String[] | 枚举值列表 | `{"北京", "上海", "深圳"}` |
| `pattern` | String | 正则表达式 | `"[0-9]+"` |
| `minValue` | String | 最小值 | `"0"` |
| `maxValue` | String | 最大值 | `"100"` |
| `itemType` | String | 数组元素类型 | `"string"` |
| `example` | String | 示例值 | `"hello@example.com"` |

**工具链示例**:
```java
@ToolParam(
    name = "price",
    type = "number",
    description = "房产价格（万元）",
    example = "500",
    minValue = "100",
    maxValue = "10000",
    required = true
)
```

---

#### 3️⃣ **创建 ToolSchema 类** ✅
**文件**: `com.agent.tool.model.ToolSchema`

**职责**:
- 将 @Tool 注解转换为 JSON Schema
- 生成 LLM 可识别的提示格式
- 支持两种输出模式：

**模式 1：完整 JSON Schema** (用于严格验证)
```json
{
  "name": "calculator",
  "description": "执行数学计算",
  "parameters": {
    "type": "object",
    "properties": {
      "expression": {
        "type": "string",
        "description": "数学表达式",
        "example": "10 * (5 + 3)"
      }
    },
    "required": ["expression"]
  },
  "examples": [
    "Input: 100 + 200 * 2",
    "Output: 500"
  ]
}
```

**模式 2：简洁提示格式** (用于节省 token)
```
calculator: 执行数学计算
  Parameters:
    - expression (string) *required: 数学表达式
  Examples:
    Input: 100 + 200 * 2
    Output: 500
```

**核心方法**:
```java
// 从注解创建 schema
ToolSchema schema = ToolSchema.fromAnnotation(toolAnnotation);

// 转为提示格式（节省 token）
String promptText = schema.toPromptFormat();
```

---

#### 4️⃣ **增强 ToolDefinition 字段** ✅
**文件**: `com.agent.tool.model.ToolDefinition`

**新增字段**:
```java
// 完整的 schema（推荐使用）
ToolSchema schema;

// 最大重试次数
Integer maxRetries;

// 超时时间（毫秒）
Long timeoutMs;

// 所需权限
String requiredPermission;

// 使用示例列表
List<String> examples;

// 分类标签
List<String> tags;
```

---

#### 5️⃣ **增强 ToolRegistry - 支持 Schema 管理** ✅
**文件**: `com.agent.tool.registry.ToolRegistry`

**新增功能**:

1. **Schema 缓存**:
```java
private final Map<String, ToolSchema> schemas = new HashMap<>();

public ToolSchema getToolSchema(String toolName) { ... }
```

2. **按标签查询工具** (支持动态加载):
```java
// 只加载"real-estate"相关工具
Collection<ToolDefinition> realEstateTools = 
    registry.getToolsByTag("real-estate");
```

3. **两种提示格式生成**:
```java
// 简洁格式（节省 token）
String brief = registry.getToolsDescription();

// 详细格式（更多信息）
String detailed = registry.getDetailedToolsDescription();
```

**示例输出**:
```
calculator: 执行数学计算
  Parameters:
    - expression (string) *required: 数学表达式
  Examples:
    Input: 100 + 200 * 2
    Output: 500

string_tools: 执行字符串操作
  Parameters:
    - input (string) *required: 格式："operation:text"
  Examples:
    Input: upper:hello world
    Output: HELLO WORLD
```

---

#### 6️⃣ **强化 ToolExecutor - 添加重试机制** ✅
**文件**: `com.agent.tool.executor.ToolExecutor`

**新增功能**:

1. **自动重试逻辑**:
```java
// 根据工具定义的 maxRetries 自动重试
// 指数退避策略：100ms, 200ms, 400ms, 800ms...
// 失败时返回最后的错误信息
```

2. **超时控制**:
```java
// 使用 ExecutorService 和 Future.get(timeout)
// 超过超时时间自动中断
// 防止工具调用卡住整个 Agent
```

3. **详细的执行指标**:
```java
// 每次执行记录：
// - toolName: 工具名
// - success: 是否成功
// - result: 执行结果
// - error: 错误信息
// - executionTimeMs: 执行时间
// - callId: 调用跟踪 ID
```

**工作流程**:
```
输入 → 验证工具存在
     ↓
获取重试配置和超时配置
     ↓
第一次尝试 → 超时控制 → 返回结果
     ↓
如果成功 → 返回结果 ✅
如果失败 → 等待 100ms
     ↓
重试 1 → 失败？ → 等待 200ms
     ↓
重试 2 → 失败？ → 返回最后的错误 ❌
```

**与 OpenClaw 的相似之处**:
- ✅ 自动重试机制减少偶发错误
- ✅ 超时防止 Agent 卡死
- ✅ 详细的执行信息便于调试

---

#### 7️⃣ **更新具体工具实现** ✅
**文件**: `com.agent.tool.builtin.BuiltInTools` 和 `RealEstateTools`

**示例 1：计算器工具**
```java
@Tool(
    name = "calculator",
    description = "执行数学计算",
    params = @ToolParam(
        name = "expression",
        type = "string",
        description = "数学表达式如 '100 + 200'",
        example = "10 * (5 + 3)",
        pattern = "[0-9+\\-*/%().\\s]+"
    ),
    returnDescription = "计算结果",
    examples = {
        "Input: 100 + 200 * 2",
        "Output: 500"
    },
    maxRetries = 2,
    timeoutMs = 5000,
    tags = {"math", "calculation"}
)
```

**示例 2：房产估价工具**
```java
@Tool(
    name = "housing_estimate",
    description = "根据房产信息估算房价",
    params = @ToolParam(
        name = "input",
        type = "string",
        description = "格式：'地址,户型,年龄' 如'朝阳区建国路,2房2厅,5年'",
        example = "朝阳区建国路,2房2厅,5年"
    ),
    examples = {
        "Input: 朝阳区建国路,2房2厅,5年",
        "Output: 📍 朝阳区建国路 估价结果..."
    },
    maxRetries = 2,
    timeoutMs = 10000,
    tags = {"real-estate", "estimation"}
)
```

---

## 🚀 核心改进点

| 方面 | 之前 | 之后 |
|------|------|------|
| **参数定义** | 简单字符串描述 | 完整的 @ToolParam 注解 |
| **类型validation** | 运行时报错 | 编译时检查 + 运行时校验 |
| **示例** | 仅在描述中 | 结构化的 examples 数组 |
| **重试机制** | 不支持 | 支持，可配置次数和退避时间 |
| **超时控制** | 没有 | 完全支持，可配置每个工具 |
| **权限管理** | 无 | 支持，标记敏感工具 |
| **工具分类** | 无 | 支持 tags，便于动态加载 |
| **LLM 提示** | 粗糙 | 高质量的 JSON Schema 和简洁格式 |

---

## 💡 使用建议

### 对 LLM 的好处

1. **更准确的理解**:
   - 详细的参数描述让 LLM 知道怎么调用
   - 示例让 LLM 学会正确的输入格式
   - 枚举值让 LLM 只生成有效的值

2. **减少调用错误**:
   - 错误的调用自动重试 2 次
   - 超时保护，不会卡死
   - 详细的错误信息更容易修正

3. **Token 效率**:
   - 可以选择简洁格式（节省 token）
   - 或详细格式（更多上下文）
   - 支持按标签动态加载（只加载需要的工具）

### 对开发者的好处

1. **易于维护**:
   - 注解即文档
   - IDE 智能提示
   - 无需手写 JSON Schema

2. **扩展性强**:
   - 加新工具只需添加注解
   - 工具框架自动扫描注册
   - 完全后向兼容

3. **生产级质量**:
   - 自动重试机制
   - 超时保护
   - 详细的执行指标

---

## 📊 性能指标目标

| 指标 | 目标 |
|------|------|
| 工具调用成功率 | ≥ 95%（含重试） |
| 平均响应时间 | < 200ms（不含网络请求） |
| LLM 调用错误率 | < 5%（格式错误） |
| 超时导致的 Agent 卡顿 | 0（完全防护） |

---

## 🔄 下一步（Step 2 和 Step 3）

### Step 2：上下文管理优化
- [ ] 实现 conversation 历史压缩
- [ ] 按 token 数量自动裁剪历史
- [ ] 支持关键步骤标记和回溯

### Step 3：浏览器自动化集成
- [ ] 集成 Playwright 库
- [ ] 实现基础的 web 自动化
- [ ] 支持截图反馈给 LLM

---

## 📚 参考资源

### 新增的类文件
- `com.agent.tool.annotation.ToolParam` - 参数注解
- `com.agent.tool.model.ToolSchema` - Schema 模型

### 修改的核心文件
- `com.agent.tool.annotation.Tool` - 增强的工具注解
- `com.agent.tool.model.ToolDefinition` - 增强的定义模型
- `com.agent.tool.registry.ToolRegistry` - 支持 Schema 和标签
- `com.agent.tool.executor.ToolExecutor` - 完整的重试和超时机制
- `com.agent.tool.builtin.BuiltInTools` - 示范实现
- `com.agent.tool.industry.RealEstateTools` - 复杂工具示范

### 编译验证
```bash
✅ mvn clean compile - BUILD SUCCESS
```

---

## 🎓 最佳实践

### DO ✅
```java
@Tool(
    name = "search_web",
    description = "在网络上搜索信息",
    params = @ToolParam(
        name = "query",
        type = "string",
        description = "搜索关键词",
        example = "中国房价",
        required = true
    ),
    returnDescription = "搜索结果列表",
    examples = {
        "Input: 北京房价",
        "Output: 找到约1,500,000 条结果"
    },
    maxRetries = 2,
    timeoutMs = 30000,
    tags = {"search", "web"}
)
```

### DON'T ❌
```java
@Tool(
    name = "search web",  // ❌ 不要有空格
    description = "在网络上搜索信息。支持所有搜索类型。可以返回多种格式的结果。支持高级搜索语法。你可以搜索任何关键词。"  // ❌ 太冗长
    // ❌ 缺少 params
    // ❌ 缺少 examples
    // ❌ 没有 maxRetries 和 timeoutMs
)
```

---

**状态**: ✅ 完成
**编译**: ✅ 通过
**下一步**: Step 2 - 上下文管理优化

