# 工具定义快速参考 - STEP 1 实现

## 🎯 5分钟速成指南

### 如何定义一个新工具？

```java
// 1. 添加注解
@Tool(
    name = "tool_name",              // 唯一识别符，使用 snake_case
    description = "工具的功能描述",    // 一句话说明工具做什么
    params = @ToolParam(              // 参数定义
        name = "param_name",
        type = "string",              // 类型：string, integer, number, array, object
        description = "参数说明",
        example = "示例值",           // LLM 学习的关键
        required = true               // 是否必需
    ),
    returnDescription = "返回值说明",  // 工具返回什么
    examples = {                      // 调用示例
        "Input: 示例输入",
        "Output: 示例输出"
    },
    maxRetries = 2,                   // 失败重试次数
    timeoutMs = 10000,                // 超时时间（毫秒）
    tags = {"category"}               // 分类标签
)
public ToolResult myTool(String input) {
    try {
        // 实现工具逻辑
        return ToolResult.success("tool_name", "结果");
    } catch (Exception e) {
        return ToolResult.failure("tool_name", "错误信息: " + e.getMessage());
    }
}
```

### 参数类型详解

| 类型 | 示例 | 使用场景 |
|------|------|--------|
| `string` | `"hello"` | 文本输入、查询、命令 |
| `integer` | `100` | 整数参数、数量、年龄 |
| `number` | `99.99` | 浮点数、价格、比率 |
| `boolean` | `true` | 开关选项 |
| `array` | `["a", "b"]` | 列表、多个值 |
| `object` | JSON 对象 | 复杂结构 |

### 常见参数配置

**必选参数**:
```java
@ToolParam(
    name = "query",
    type = "string",
    required = true,
    description = "必须提供的参数"
)
```

**可选参数 + 默认值**:
```java
@ToolParam(
    name = "limit",
    type = "integer",
    required = false,
    defaultValue = "10",
    description = "结果数量限制"
)
```

**枚举类型** (让 LLM 只选择有效值):
```java
@ToolParam(
    name = "city",
    type = "string",
    enum_ = {"北京", "上海", "深圳"},
    description = "选择城市"
)
```

**范围限制** (数值):
```java
@ToolParam(
    name = "price",
    type = "number",
    minValue = "0",
    maxValue = "1000000",
    description = "价格范围"
)
```

**格式验证** (文本):
```java
@ToolParam(
    name = "email",
    type = "string",
    pattern = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
    example = "user@example.com",
    description = "电子邮件地址"
)
```

---

## 📝 实际工具示例

### 示例 1：简单工具（无参数）

```java
@Tool(
    name = "get_current_time",
    description = "获取当前系统时间",
    params = {},  // 无参数
    returnDescription = "当前 Unix 时间戳",
    examples = {
        "Input: (无参数)",
        "Output: 1741920000000"
    },
    maxRetries = 1,
    timeoutMs = 2000,
    tags = {"time", "utility"}
)
public ToolResult getCurrentTime(String input) {
    long timestamp = System.currentTimeMillis();
    return ToolResult.success("get_current_time", String.valueOf(timestamp));
}
```

### 示例 2：单参数工具

```java
@Tool(
    name = "calculator",
    description = "执行数学计算",
    params = @ToolParam(
        name = "expression",
        type = "string",
        description = "数学表达式，支持 +, -, *, /, %, ()",
        example = "10 * (5 + 3)",
        pattern = "[0-9+\\-*/%().\\s]+",
        required = true
    ),
    returnDescription = "计算结果",
    examples = {
        "Input: 100 + 200 * 2",
        "Output: 500",
        "Input: (10 + 20) / 3",
        "Output: 10"
    },
    maxRetries = 2,
    timeoutMs = 5000,
    tags = {"math"}
)
public ToolResult calculator(String expression) {
    try {
        if (!expression.matches("[0-9+\\-*/%().\\s]+")) {
            return ToolResult.failure("calculator", 
                "仅支持数字和基本操作符");
        }
        
        // 使用 JavaScript 引擎计算
        ScriptEngine engine = 
            new ScriptEngineManager().getEngineByName("JavaScript");
        Object result = engine.eval(expression);
        
        return ToolResult.success("calculator", 
            String.valueOf(result));
    } catch (Exception e) {
        return ToolResult.failure("calculator", 
            "计算失败: " + e.getMessage());
    }
}
```

### 示例 3：多参数工具（复合格式）

```java
@Tool(
    name = "housing_estimate",
    description = "估算房产价格",
    params = @ToolParam(
        name = "input",
        type = "string",
        description = "房产信息，格式：'地点,户型,年龄'",
        example = "朝阳区建国路,2房2厅,5",
        required = true
    ),
    returnDescription = "房产估价结果和分析",
    examples = {
        "Input: 朝阳区建国路,2房2厅,5",
        "Output: 📍 朝阳区建国路 估价结果 ...",
        "Input: 浦东新区世纪大道,3房2厅,10",
        "Output: 📍 浦东新区世纪大道 估价结果 ..."
    },
    maxRetries = 2,
    timeoutMs = 10000,
    tags = {"real-estate", "estimation"}
)
public ToolResult estimateHousingPrice(String input) {
    try {
        String[] parts = input.split(",");
        if (parts.length != 3) {
            return ToolResult.failure("housing_estimate",
                "格式错误，应为：地点,户型,年龄");
        }
        
        String location = parts[0].trim();
        String type = parts[1].trim();
        int age = Integer.parseInt(parts[2].trim());
        
        // 估价逻辑
        double basePrice = getBasePriceForLocation(location);
        double typeMultiplier = getPriceMultiplier(type);
        double ageDiscount = 1.0 - (age * 0.02); // 每年贬值2%
        
        double estimatedPrice = basePrice * typeMultiplier * ageDiscount;
        
        String result = String.format(
            "📍 %s\n户型：%s | 年龄：%d年\n估价：%.0f万元",
            location, type, age, estimatedPrice);
        
        return ToolResult.success("housing_estimate", result);
    } catch (Exception e) {
        return ToolResult.failure("housing_estimate",
            "估价失败: " + e.getMessage());
    }
}
```

---

## 🔧 高级特性

### 权限控制

标记需要特殊权限的工具：

```java
@Tool(
    name = "delete_file",
    description = "删除文件（危险操作）",
    requiredPermission = "write:files",  // 标记权限
    // ... 其他配置
)
public ToolResult deleteFile(String filePath) {
    // 执行前框架会检查用户权限
}
```

### 工具分类和动态加载

使用 `tags` 便于动态加载需要的工具：

```java
// 搜索相关工具
@Tool(
    name = "search_web",
    tags = {"search", "web"}  // 标签
)

@Tool(
    name = "search_local_files",
    tags = {"search", "file"}
)

// 在代码中动态加载
Collection<ToolDefinition> searchTools = 
    registry.getToolsByTag("search");
```

### 性能优化

调整重试和超时配置以平衡性能：

```java
// 快速响应的工具
@Tool(
    name = "check_status",
    maxRetries = 1,      // 只重试 1 次
    timeoutMs = 2000,    // 2 秒超时
)

// 慢速但重要的工具
@Tool(
    name = "process_large_file",
    maxRetries = 3,      // 重试 3 次
    timeoutMs = 60000,   // 60 秒超时
)
```

---

## ✅ 清单：创建新工具时

- [ ] 工具名是否为 `snake_case` 格式?
- [ ] 描述是否简洁（< 100 字）?
- [ ] 是否添加了参数定义 (`@ToolParam`)?
- [ ] 参数示例是否真实可用?
- [ ] 是否包括了调用 examples?
- [ ] 是否设置了合理的 `maxRetries` 和 `timeoutMs`?
- [ ] 是否添加了 `tags` 便于分类?
- [ ] 返回值是否总是 `ToolResult`?
- [ ] 错误情况是否使用 `ToolResult.failure()`?
- [ ] 是否在 Spring 的 `@Component` 中定义?

---

## 🐛 常见错误

### ❌ 错误 1：参数格式不清晰

```java
// ❌ 不好
@ToolParam(
    name = "data",
    type = "string",
    description = "数据",  // 太模糊
    example = "123"
)

// ✅ 好
@ToolParam(
    name = "house_info",
    type = "string",
    description = "房产信息，格式：'地址,户型,年龄'，例如：'朝阳区,2房2厅,5'",
    example = "朝阳区建国路,2房2厅,5"
)
```

### ❌ 错误 2：缺少重试配置

```java
// ❌ 网络请求容易失败，应该重试
@Tool(
    name = "search_web",
    // ❌ 缺少 maxRetries...
)

// ✅ 应该这样
@Tool(
    name = "search_web",
    maxRetries = 3,  // 网络操作，重试更多次
    timeoutMs = 30000,
)
```

### ❌ 错误 3：超时设置过短

```java
// ❌ 数据库查询可能需要时间
@Tool(
    name = "query_database",
    timeoutMs = 1000,  // ❌ 太短
)

// ✅ 应该这样
@Tool(
    name = "query_database",
    timeoutMs = 10000,  // 10 秒比较合理
)
```

---

## 📊 工具定义对比

| 方面 | 新系统 | 旧系统 |
|------|--------|--------|
| 参数定义 | `@ToolParam` 注解 | 字符串描述 |
| IDE 支持 | 完全支持 | 手动编写 |
| 验证 | 编译+运行时 | 仅运行时 |
| 示例 | 结构化数组 | 文本混合 |
| 重试 | 自动处理 | 需手动实现 |
| 超时 | 配置即生效 | 无保护 |
| 分类 | 支持 tags | 无分类 |

---

## 🚀 下一步

1. **编写你的第一个工具** - 照着示例改改
2. **编译并测试** - `mvn clean compile`
3. **在 Agent 中使用** - LLM 会自动识别
4. **观察重试效果** - 查看日志中的 "Retrying" 信息
5. **调整参数** - 根据实际情况调整 `maxRetries` 和 `timeoutMs`

