# 🎯 阶段 2 实战代码学习路径

## 📂 你应该研究的文件

### 1️⃣ 配置演示类（从简到复杂）

#### 基础：ConfigValueDemo.java
**目标**：学习 @Value 的各种用法

```
位置：src/main/java/com/agent/demo/ConfigValueDemo.java

学习步骤：
1. 观察 @Value 的基础语法
2. 看每个字段上的注解
3. 理解默认值的写法：@Value("${key:default}")
4. 运行演示端点 demo-value-injection 看实际值
5. 尝试在 application.yml 中修改值，重启应用看效果
```

**关键代码片段**：
```java
@Value("${llm.deepseek.model}")      // 直接注入
private String llmModel;

@Value("${custom.feature.enabled:false}")  // 有默认值
private boolean customFeatureEnabled;
```

#### 中等：AppConfigProperties.java
**目标**：学习 @ConfigurationProperties 的用法

```
位置：src/main/java/com/agent/demo/AppConfigProperties.java

学习步骤：
1. 看 @ConfigurationProperties(prefix = "app") 的使用
2. 观察嵌套类 Features 的结构
3. 理解如何绑定嵌套的 YAML 属性
4. 运行演示端点 demo-config-properties 看绑定结果
5. 在 application.yml 的 app 下添加新属性，看自动绑定
```

**关键代码片段**：
```java
@ConfigurationProperties(prefix = "app")
public class AppConfigProperties {
    
    private String name;
    private String version;
    private Features features = new Features();  // 嵌套
    
    @Data
    public static class Features {
        private boolean caching;
        private boolean monitoring;
    }
}
```

#### 高级：ConfigurationDemoService.java
**目标**：学习配置管理的系统思维

```
位置：src/main/java/com/agent/demo/ConfigurationDemoService.java

学习步骤：
1. 看 @Autowired Environment environment 的使用
2. 学习 6 个演示方法的逻辑
3. 理解配置的完整生命周期
4. 一个一个调用 6 个演示端点
5. 在生产场景中思考如何应用这些方法
```

**关键方法学习顺序**：

1. `explainConfigurationChain()` ← 从这里开始
   └─ 了解配置的加载链

2. `demonstrateConfigurationProperties()` ← 然后这个
   └─ 理解类型安全的配置绑定

3. `demonstrateValueAnnotation()` ← 然后这个
   └─ 理解单值注入

4. `demonstrateMultiEnvironment()` ← 关键！
   └─ 理解多环境隔离

5. `demonstrateEnvironmentAccess()` ← 高级用法
   └─ 动态读取配置

6. `demonstrateConfigValidation()` ← 最后这个
   └─ 理解类型转换和验证

---

### 2️⃣ Controller 的使用（ChatController.java）

**目标**：看如何在实际 Controller 中使用配置

```
位置：src/main/java/com/agent/controller/ChatController.java
      行号：32-60（依赖注入部分）
      行号：470-620（演示端点部分）

学习步骤：
1. 看构造器中如何注入 ConfigurationDemoService
2. 看 6 个配置演示端点的实现
3. 理解 @GetMapping 如何调用服务方法
4. 运行每个端点，理解返回的结构
```

**关键：依赖注入的完整示例**
```java
public ChatController(
    ReasoningEngine reasoningEngine,
    SessionManager sessionManager,
    BeanDemoService beanDemoService,
    BeanContainerDemoService beanContainerDemoService,
    CircularDependencyDemoService circularDependencyDemoService,
    ConfigurationDemoService configurationDemoService  // ← 新的
) {
    // Spring 自动注入所有依赖
    this.configurationDemoService = configurationDemoService;
}
```

---

### 3️⃣ 配置文件（application.yml）

**目标**：理解配置文件的结构

```
位置：src/main/resources/application.yml

学习步骤：
1. 阅读整个文件，理解层级结构
2. 识别 5 个主要配置部分：
   - spring（Spring 框架配置）
   - logging（日志配置）
   - llm（LLM 相关）
   - agent（Agent 相关）
   - app（应用自定义）
   - server（服务器配置）
3. 理解 YAML 的缩进规则
4. 尝试修改某个值，重启应用看效果
5. 创建 application-dev.yml 和 application-prod.yml
```

**YAML 结构分析**：
```yaml
spring:              # 一级
  profiles:          # 二级
    active: dev      # 三级（值）

llm:                 # 一级
  deepseek:          # 二级
    model: xxx       # 三级（值）
    max-tokens: 4096 # 三级（值）
    features:        # 三级
      caching: true  # 四级（值）
```

---

## 🚀 分阶段学习计划

### 第 1 天（第 1 小时）- 基础概念

```
学习顺序：
1. 读 PHASE_2_CONFIGURATION_GUIDE.md 的"核心概念"部分
   └─ 理解配置的 5 个层次

2. 查看 application.yml
   └─ 识别你项目的配置结构

3. 调用 /api/agent/demo-config-chain
   └─ 看配置是如何加载的

预计时间：30-45 分钟
```

### 第 2 天（第 2 小时）- @Value 学习

```
学习顺序：
1. 研究 ConfigValueDemo.java 源码
   └─ 看每个 @Value 的用法

2. 调用 /api/agent/demo-value-injection
   └─ 看实际注入的值

3. 在 application.yml 中修改一个值
   └─ 重启应用，看演示端点的输出变化

4. 尝试添加一个新的 @Value 注入
   └─ 在 ConfigValueDemo 中添加新字段

预计时间：45-60 分钟
```

### 第 3 天（第 3 小时）- @ConfigurationProperties

```
学习顺序：
1. 研究 AppConfigProperties.java 源码
   └─ 理解 @ConfigurationProperties 的使用

2. 看 application.yml 中的 app 配置部分
   └─ 理解 YAML 如何映射到 Java 对象

3. 调用 /api/agent/demo-config-properties
   └─ 看绑定后的值

4. 自己创建一个配置类
   └─ 为 llm.deepseek 创建 LlmProperties

5. 在 ConfigurationDemoService 中使用你的新类
   └─ 加入新的演示方法

预计时间：60-75 分钟
```

### 第 4 天（第 4 小时）- 多环境配置

```
学习顺序：
1. 调用 /api/agent/demo-multi-environment
   └─ 理解不同环境的配置对比

2. 创建 application-prod.yml
   └─ 定义生产环境的配置

3. 修改 spring.profiles.active 从 dev 改为 prod
   └─ 重启应用，看日志变化

4. 创建 application-local.yml
   └─ 定义本地开发配置，在 .gitignore 中忽略

5. 尝试用命令行参数激活环境
   ```
   mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=test"
   ```

预计时间：75-90 分钟
```

### 第 5 天（第 5 小时）- 高级用法

```
学习顺序：
1. 研究 ConfigurationDemoService 的所有 6 个方法
   └─ 理解每个方法的目的

2. 调用所有 6 个演示端点
   └─ 理解完整的配置管理系统

3. 看 Environment 的使用
   └─ 学习动态读取配置

4. 实现自己的配置访问方法
   └─ 在你的代码中使用 Environment

5. PHASE_2_QUICK_REFERENCE.md 的快速参考
   └─ 建立快速查询能力

预计时间：90-120 分钟
```

---

## 🔍 代码阅读技巧

### 当看 @Value 时，关注

```java
@Value("${llm.deepseek.model}")  // ← 配置的路径
private String model;             // ← 注入到哪个字段

// 问自己：
// 1. ${} 中的路径在 application.yml 中是否存在？
// 2. 类型是否匹配？（String 对 String，int 对整数）
// 3. 如果配置不存在会怎样？（需要 :默认值）
```

### 当看 @ConfigurationProperties 时，关注

```java
@ConfigurationProperties(prefix = "app")  // ← 前缀
@Data
public class AppConfig {
    private String name;                   // ← 对应 app.name
    private Features features;             // ← 对应 app.features（嵌套）
}

// 问自己：
// 1. prefix 是什么？
// 2. 每个字段对应 YAML 中的哪个路径？
// 3. 嵌套类是如何绑定的？
```

### 当看演示端点时，关注

```java
@GetMapping("/demo-config-properties")
public ResponseEntity<Map<String, Object>> demoConfigProperties() {
    Map<String, Object> props = 
        configurationDemoService.demonstrateConfigurationProperties();
    // 问自己：
    // 1. 这个端点调用了什么服务方法？
    // 2. 该方法内部是如何获取配置的？
    // 3. 为什么这样设计返回结构？
}
```

---

## 📊 练习题准备

### 练习 1：创建 LLM 配置类

```
任务：为 llm.deepseek 的所有属性创建 @ConfigurationProperties 类

步骤：
1. 创建 LlmProperties.java
2. 使用 @ConfigurationProperties(prefix = "llm.deepseek")
3. 为每个属性创建字段
4. 添加 @Data 和 @Validated
5. 在 ConfigurationDemoService 中注入并使用
6. 创建演示端点显示加载的 LLM 配置

预期结果：
- 看到 model: deepseek-chat
- 看到 temperature: 0.7
- 看到 max-tokens: 4096
等等所有配置
```

### 练习 2：创建多环境配置

```
任务：为你的项目创建完整的多环境配置

步骤：
1. 创建 application-dev.yml（开发环境）
   - logging.level.com.agent: DEBUG
   - server.port: 9090

2. 创建 application-test.yml（测试环境）
   - logging.level.com.agent: INFO
   - server.port: 8081

3. 创建 application-prod.yml（生产环境）
   - logging.level.root: WARN
   - server.port: 8080

4. 测试环境切换
5. 验证配置是否正确覆盖

预期结果：
- 激活 dev 时，日志级别是 DEBUG，端口是 9090
- 激活 test 时，日志级别是 INFO，端口是 8081
- 激活 prod 时，日志级别是 WARN，端口是 8080
```

### 练习 3：敏感信息处理

```
任务：使用环境变量保护 API 密钥

问题：
当前 llm.deepseek.api-key 直接写在配置文件中【不安全】

解决方案：
1. 在 application.yml 中改为：
   api-key: ${LLM_API_KEY}

2. 启动应用时设置环境变量：
   export LLM_API_KEY=sk-real-secret-key
   mvn spring-boot:run

3. 在 .gitignore 中添加：
   application-local.yml

预期结果：
- 密钥从环境变量读取而不是配置文件
- Git 不会提交真实的密钥
- 不同服务器可以有不同的密钥
```

---

## 🎬 演示端点使用场景

### 场景 1：调试环境问题

```bash
# 问题：配置没有生效

# 解决步骤：
1. 调用 demo-config-chain 看环境设置
   curl http://localhost:8080/api/agent/demo-config-chain | jq '.'

2. 看当前激活的环境是什么
   查看输出中的 "当前激活的环境"

3. 检查对应的文件是否存在
   ls -la src/main/resources/application-*.yml

4. 检查属性名称是否正确
   # 在 demo-config-properties 中看已加载的值
   curl http://localhost:8080/api/agent/demo-config-properties | jq '.'
```

### 场景 2：理解配置优先级

```bash
# 问题：哪个配置会被使用？

# 解决步骤：
1. 调用 demo-multi-environment 看优先级
   curl http://localhost:8080/api/agent/demo-multi-environment | jq '.'

2. 理解优先级链
   - application.yml（基础）< application-dev.yml < 环境变量 < 命令行

3. 验证覆盖规则
   在 application.yml 和 application-dev.yml 中都定义 server.port
   确认激活 dev 时，使用的是 dev 中的值
```

### 场景 3：验证配置类是否正确

```bash
# 问题：@ConfigurationProperties 绑定失败？

# 解决步骤：
1. 调用 demo-config-properties 看绑定结果
   curl http://localhost:8080/api/agent/demo-config-properties | jq '.'

2. 检查 YAML 路径是否与类的 prefix 匹配
3. 检查字段名是否与 YAML 属性名一致（短横线 vs 驼峰）
4. 检查类型是否正确
```

---

## 💾 代码修改练习

### 练习 1：添加新的配置

```
步骤：
1. 在 application.yml 中添加新配置：
   cache:
     enabled: true
     ttl: 3600
     max-size: 1000

2. 在 ConfigValueDemo 中添加：
   @Value("${cache.enabled}")
   private boolean cacheEnabled;
   
   @Value("${cache.ttl}")
   private int cacheTtl;

3. 重编译并运行应用

4. 调用 demo-value-injection 看新值
```

### 练习 2：创建新的配置类

```
步骤：
1. 创建新文件 CacheProperties.java

2. 使用 @ConfigurationProperties(prefix = "cache")

3. 添加字段并用 @Data

4. 在 ConfigurationDemoService 中注入

5. 创建新的演示方法

6. 在 ChatController 中添加演示端点

7. 新建一个端点测试你的新类
```

---

## ✅ 学习检查清单

完成以下所有项目表示你已经掌握阶段 2：

- [ ] 能解释配置的 5 个层次
- [ ] 能区分 @Value 和 @ConfigurationProperties 的使用场景
- [ ] 能创建嵌套的配置类（有内部类）
- [ ] 能创建多环境配置文件并正确切换
- [ ] 能用 Environment 动态读取配置
- [ ] 能为配置添加验证注解
- [ ] 能解释配置优先级链
- [ ] 能处理敏感信息（使用环境变量）
- [ ] 能调用所有 6 个演示端点并理解输出
- [ ] 能自己创建一个新的配置类并使用
- [ ] 能解释为什么某个配置没有生效
- [ ] 能写出规范的 YAML 配置文件

---

## 🎓 知识转移

学完这些后，你可以：

1. **迁移到其他 Spring Boot 项目**
   - 能快速理解和修改配置
   - 能创建适当的配置类
   - 能实现多环境隔离

2. **处理生产环境问题**
   - 能调整配置而不修改代码
   - 能快速诊断配置问题
   - 能安全地处理敏感信息

3. **设计可配置的系统**
   - 能识别哪些值应该配置化
   - 能设计优雅的配置结构
   - 能实现灵活的配置方案

4. **参与团队协作**
   - 能与云运维团队沟通配置需求
   - 能提供清晰的配置文档
   - 能处理配置版本控制

---

## 📞 遇到问题

如果你在学习过程中遇到问题：

1. **首先检查**：
   - 文件名是否正确（短横线 vs 驼峰）
   - YAML 缩进是否正确
   - 路径是否与 @ConfigurationProperties 的 prefix 匹配

2. **然后调用演示端点**：
   - 查看实际加载的配置值
   - 对比期望值和实际值
   - 找出差异的原因

3. **最后查看日志**：
   - 应用启动日志可能有 Warn 信息
   - 配置绑定失败时会有错误提示

---

**准备好开始学习了吗？👉 从 ConfigValueDemo.java 开始！**
