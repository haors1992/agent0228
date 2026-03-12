# 📚 阶段 2：Spring Boot 配置管理完全指南

## 目录

1. [核心概念](#核心概念)
2. [三种配置注入方式](#三种配置注入方式)
3. [配置文件结构](#配置文件结构)
4. [多环境配置](#多环境配置)
5. [演示端点汇总](#演示端点汇总)
6. [最佳实践](#最佳实践)

---

## 核心概念

### 什么是 Spring Boot 配置？

**配置** 就是应用的参数，用 YAML 或 Properties 文件定义，在运行时被 Spring 注入到 Bean 中。

```
示例配置项：
- 应用名称：ai-agent-quickstart
- LLM API 密钥：sk-6c677513426744a6833bd437bbc8733c
- 数据库连接：jdbc:mysql://localhost:3306/db
- 日志级别：DEBUG
- 服务器端口：8080
```

### 配置的生命周期

```
1️⃣ 定义      → 在 application.yml 中定义配置
   ↓
2️⃣ 加载      → Spring 读取 YAML 文件，存储在 Environment 对象
   ↓
3️⃣ 绑定      → 自动转换类型（String → int/boolean）
   ↓
4️⃣ 注入      → 通过 @Value 或 @ConfigurationProperties 注入到 Bean
   ↓
5️⃣ 使用      → Bean 在代码中使用配置值
```

### 配置来源优先级（从低到高）

```
📊 优先级排序：

1. application.yml（最低）
   ↓
2. application-{profile}.yml（中等）
   例：application-dev.yml, application-test.yml
   ↓
3. 环境变量
   ↓
4. 命令行参数（最高）
   java -jar app.jar --server.port=9090

⚠️ 高优先级配置会覆盖低优先级配置
```

---

## 三种配置注入方式

### 方式 1：@Value 注解（简单配置）

用于注入 **单个属性**，适合简单配置项。

#### 基础语法

```java
@Component
public class MyService {
    
    // ✅ 直接注入
    @Value("${llm.deepseek.model}")
    private String model;
    
    // ✅ 注入整数
    @Value("${llm.deepseek.max-tokens}")
    private int maxTokens;
    
    // ✅ 注入布尔值
    @Value("${llm.deepseek.enabled}")
    private boolean enabled;
    
    // ✅ 带默认值（如果不存在配置，使用默认值）
    @Value("${custom.feature:false}")
    private boolean customFeature;
    
    // ✅ 注入列表
    @Value("#{'${server.allowed-hosts}'.split(',')}")
    private List<String> allowedHosts;
}
```

#### 类型自动转换

| YAML 值 | Java 类型 | 转换后 |
|---------|---------|--------|
| `true` | boolean | `true` |
| `4096` | int | `4096` |
| `0.7` | double | `0.7` |
| `deepseek-chat` | String | `"deepseek-chat"` |
| `[1,2,3]` | List | `[1, 2, 3]` |

#### 项目中的实际例子

```yaml
# application.yml
llm:
  deepseek:
    enabled: true
    model: deepseek-chat
    max-tokens: 4096
    temperature: 0.7
```

```java
@Component
public class ConfigValueDemo {
    
    @Value("${llm.deepseek.enabled:true}")
    private boolean enabled;
    
    @Value("${llm.deepseek.model}")
    private String model;
    
    @Value("${llm.deepseek.max-tokens}")
    private int maxTokens;
    
    @Value("${llm.deepseek.temperature}")
    private double temperature;
    
    public void print() {
        System.out.println("启用: " + enabled);
        System.out.println("模型: " + model);
        System.out.println("Token: " + maxTokens);
        System.out.println("温度: " + temperature);
    }
}
```

#### 何时使用 @Value

✅ 使用 @Value：
- 注入单个属性
- 配置项较少（少于 5 个）
- 简单的数据类型（String、int、boolean）

❌ 不用 @Value：
- 需要注入一组相关的属性（超过 5 个）
- 有复杂的嵌套结构
- 需要参数验证

---

### 方式 2：@ConfigurationProperties（复杂配置）

用于注入 **一组相关属性**，适合复杂配置项。

#### 基础语法

```java
@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    
    private String name;
    private String version;
    private Features features = new Features();
    
    @Data
    public static class Features {
        private boolean caching;
        private boolean monitoring;
        private int maxConnections = 10;
    }
}
```

#### 对应的 YAML 配置

```yaml
app:
  name: My Application
  version: 1.0.0
  features:
    caching: true
    monitoring: true
    maxConnections: 100
```

#### 使用这个配置类

```java
@Service
public class MyService {
    
    @Autowired
    private AppProperties appProps;
    
    public void showConfig() {
        System.out.println("应用名: " + appProps.getName());
        System.out.println("版本: " + appProps.getVersion());
        System.out.println("缓存启用: " + appProps.getFeatures().isCaching());
        System.out.println("连接数: " + appProps.getFeatures().getMaxConnections());
    }
}
```

#### 项目中的实际例子

```yaml
# application.yml
llm:
  deepseek:
    enabled: true
    api-key: sk-6c677513426744a6833bd437bbc8733c
    base-url: https://api.deepseek.com
    model: deepseek-chat
    max-tokens: 4096
    temperature: 0.7
    timeout: 120
```

```java
@Component
@ConfigurationProperties(prefix = "llm.deepseek")
@Data
public class LlmProperties {
    
    private boolean enabled;
    private String apiKey;
    private String baseUrl;
    private String model;
    private int maxTokens;
    private double temperature;
    private int timeout;
}
```

#### 何时使用 @ConfigurationProperties

✅ 使用 @ConfigurationProperties：
- 注入一组相关的属性（5 个以上）
- 有嵌套结构（例如 agent.session.storage-path）
- 需要类型安全和自动转换
- 想要添加参数验证
- 代码复用（多处使用同组配置）

---

### 方式 3：Environment 接口（动态读取）

用于 **运行时动态读取** 配置，适合条件判断和灵活需求。

#### 基础语法

```java
@Service
public class ConfigService {
    
    @Autowired
    private Environment env;
    
    public void demonstrateEnvironment() {
        // 方式 1：直接读取，可能为 null
        String model = env.getProperty("llm.deepseek.model");
        
        // 方式 2：提供默认值
        String timeout = env.getProperty("llm.deepseek.timeout", "120");
        
        // 方式 3：转换成特定类型
        Integer port = env.getProperty("server.port", Integer.class, 8080);
        
        // 方式 4：获取当前激活的环境
        String[] profiles = env.getActiveProfiles();
    }
}
```

#### 使用场景

```java
@Service
public class FeatureToggleService {
    
    @Autowired
    private Environment env;
    
    public void useFeatures() {
        // 根据配置动态启用/禁用功能
        if ("prod".equals(env.getProperty("spring.profiles.active"))) {
            // 生产环境，使用保守配置
            useProductionConfig();
        } else {
            // 开发环境，使用调试配置
            useDevelopmentConfig();
        }
    }
}
```

#### 项目中的使用场景

```java
@Service
public class LoggingConfigService {
    
    @Autowired
    private Environment env;
    
    public void configureLogging() {
        String activeProfile = env.getProperty("spring.profiles.active");
        String llmModel = env.getProperty("llm.deepseek.model");
        
        // 根据环境配置日志
        if ("prod".equals(activeProfile)) {
            setLogLevel("WARN");
        } else if ("dev".equals(activeProfile)) {
            setLogLevel("DEBUG");
        }
    }
}
```

#### 何时使用 Environment

✅ 使用 Environment：
- 需要条件判断配置
- 配置可能在运行时改变
- 编写通用工具类，需要灵活访问配置
- 需要获取当前激活的环境（profile）

---

## 配置文件结构

### 你项目的配置文件结构

```
src/main/resources/
├── application.yml          ← 主配置（总是加载）
├── application-test.yml     ← 测试环境配置
└── prompts/                 ← 提示词目录
```

### application.yml 详解

```yaml
spring:
  application:
    name: ai-agent-quickstart    # 应用名称
  
  profiles:
    active: dev                  # 激活的环境（dev 表示使用 application-dev.yml）
  
  main:
    allow-circular-references: true  # 允许循环依赖

logging:                         # 日志配置
  level:
    com.agent: DEBUG            # 你的包名的日志级别
    org.springframework: INFO    # Spring 框架日志级别

llm:                            # LLM 配置（嵌套结构）
  deepseek:
    enabled: true
    api-key: sk-6c6775...       # API 密钥
    base-url: https://...       # API 地址
    model: deepseek-chat        # 模型名称
    max-tokens: 4096            # 最大 Token
    temperature: 0.7            # 温度（创意度）
    timeout: 120                # 超时时间

agent:                          # Agent 配置
  max-iterations: 10            # 最大迭代次数
  timeout: 300                  # 超时（秒）
  enable-streaming: false       # 是否启用流式输出
  session:
    storage-path: ./data/sessions  # 会话存储路径
  knowledge:
    enabled: true
    storage-path: ./data/knowledge # 知识库路径
    top-k: 3                    # 检索 Top K 结果
    embedding-model: local      # 嵌入模型

server:                         # 服务器配置
  port: 8080                    # 服务器端口

app:                           # 应用自定义配置
  name: AI Agent Application
  version: 2.0.0
  features:
    caching: true
    monitoring: true
    maxConnections: 100
```

### 嵌套结构的两种访问方式

#### 使用 @Value

```java
@Component
public class Service1 {
    @Value("${llm.deepseek.model}")
    private String model;
    
    @Value("${agent.session.storage-path}")
    private String sessionPath;
}
```

#### 使用 @ConfigurationProperties

```java
@Component
@ConfigurationProperties(prefix = "llm")
@Data
public class LlmConfig {
    private Deepseek deepseek;
    
    @Data
    public static class Deepseek {
        private boolean enabled;
        private String apiKey;
        private String model;
        // 其他属性...
    }
}

// 使用时
@Autowired
private LlmConfig llmConfig;

String model = llmConfig.getDeepseek().getModel();
```

---

## 多环境配置

### 为什么需要多环境配置？

不同环境有不同的需求：

| 配置项 | 开发环境 | 测试环境 | 生产环境 |
|-------|--------|--------|--------|
| 日志级别 | DEBUG | INFO | WARN |
| LLM 温度 | 1.0（创意） | 0.5 | 0.3（保守） |
| 数据库 | 本地 SQLite | 测试 MySQL | 线上 PostgreSQL |
| API 密钥 | 测试密钥 | 测试密钥 | 真实密钥 |
| 缓存 | 禁用 | 启用 | 启用 |

### 实现多环境配置

#### 步骤 1：创建基础配置

```yaml
# application.yml（共通配置）
spring:
  application:
    name: ai-agent-quickstart
  profiles:
    active: dev                # 指定默认激活的环境

llm:
  deepseek:
    model: deepseek-chat
    max-tokens: 4096
```

#### 步骤 2：创建环境特定配置

```yaml
# application-dev.yml（开发环境）
logging:
  level:
    com.agent: DEBUG
    org.springframework: DEBUG

llm:
  deepseek:
    temperature: 1.0           # ← 覆盖基础配置

server:
  port: 9090                   # ← 覆盖基础配置
```

```yaml
# application-test.yml（测试环境）
logging:
  level:
    com.agent: INFO

llm:
  deepseek:
    temperature: 0.5
    max-tokens: 2048

server:
  port: 8081
```

```yaml
# application-prod.yml（生产环境）
logging:
  level:
    com.agent: WARN
    org.springframework: WARN

llm:
  deepseek:
    temperature: 0.3           # ← 保守生成
    max-tokens: 8192           # ← 生产环境可用更多 Token

server:
  port: 8080
```

#### 步骤 3：激活环境

##### 方式 1：修改 application.yml

```yaml
spring:
  profiles:
    active: prod    # ← 改成你想要的环境
```

##### 方式 2：运行时参数

```bash
java -jar app.jar --spring.profiles.active=prod
```

##### 方式 3：环境变量

```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar app.jar
```

##### 方式 4：IDE 配置（IntelliJ IDEA）

```
Run → Edit Configurations
→ VM options: -Dspring.profiles.active=dev
```

### 配置加载流程

```
当激活环境为 dev 时：

1️⃣ 加载 application.yml
   ├─ spring.application.name = ai-agent-quickstart
   ├─ llm.deepseek.model = deepseek-chat
   └─ llm.deepseek.temperature = 0.7

   ↓
2️⃣ 读取 spring.profiles.active = dev
   ↓
3️⃣ 加载 application-dev.yml（覆盖同名属性）
   ├─ logging.level.com.agent = DEBUG ← 覆盖基础
   ├─ llm.deepseek.temperature = 1.0  ← 覆盖基础
   └─ server.port = 9090              ← 覆盖基础
   
   ↓
4️⃣ 最终配置结果
   ├─ spring.application.name = ai-agent-quickstart（来自基础）
   ├─ llm.deepseek.model = deepseek-chat（来自基础）
   ├─ llm.deepseek.temperature = 1.0（来自 dev）← 被覆盖了
   ├─ logging.level.com.agent = DEBUG（来自 dev）← 被覆盖了
   └─ server.port = 9090（来自 dev）← 被覆盖了
```

---

## 演示端点汇总

### 【演示 1】配置文件加载链

```
GET /api/agent/demo-config-chain

展示：配置文件加载的完整流程
```

### 【演示 2】@ConfigurationProperties 配置绑定

```
GET /api/agent/demo-config-properties

展示：如何使用 @ConfigurationProperties 注入一组配置
演示值：app.name, app.version, app.features
```

### 【演示 3】@Value 注解注入单个属性

```
GET /api/agent/demo-value-injection

展示：如何使用 @Value 注入配置
演示值：llm 模型、温度、最大 Token 等
```

### 【演示 4】多环境配置

```
GET /api/agent/demo-multi-environment

展示：不同环境的配置对比
列出：dev、test、prod 三种环境的差异
```

### 【演示 5】使用 Environment 动态读取配置

```
GET /api/agent/demo-environment-access

展示：如何用 Environment 对象动态读取配置
演示：当前激活的环境，各个配置值
```

### 【演示 6】配置验证和类型转换

```
GET /api/agent/demo-config-validation

展示：Spring 的自动类型转换
演示：YAML → Java 各种类型的转换
```

---

## 最佳实践

### 1. 配置命名规范

```yaml
# ✅ 好的实践
llm:
  deepseek:
    api-key: ...       # 使用 kebab-case（短横线）
    max-tokens: 4096
    temperature: 0.7

# ❌ 避免
llm:
  deepseek:
    apiKey: ...        # 不要用 camelCase
    maxTokens: 4096
    Temperature: 0.7   # 大小写不一致
```

### 2. 选择合适的配置方式

```
简单配置（1-2 个属性）
        ↓
    使用 @Value
        
复杂配置（3 个以上属性，相关）
        ↓
    使用 @ConfigurationProperties
        
需要条件判断或动态读取
        ↓
    使用 Environment
```

### 3. 配置的验证

```java
@Component
@ConfigurationProperties(prefix = "llm.deepseek")
@Data
@Validated  // ← 启用验证
public class LlmProperties {
    
    @NotNull
    private String apiKey;
    
    @Min(1)
    @Max(10000)
    private int maxTokens;
    
    @DecimalMin("0.0")
    @DecimalMax("2.0")
    private double temperature;
}
```

### 4. 敏感信息处理

```yaml
# ❌ 不要直接写密钥
llm:
  deepseek:
    api-key: sk-6c677513426744a6833bd437bbc8733c

# ✅ 使用环境变量
llm:
  deepseek:
    api-key: ${LLM_API_KEY}  # 从环境变量读取
```

```bash
# 运行时设置环境变量
export LLM_API_KEY=sk-your-secret-key
java -jar app.jar
```

### 5. 配置文件版本控制

```
git 提交：
✅ application.yml 提交（通用配置）
✅ application-test.yml 提交（测试配置）
❌ application-prod.yml 不提交（生产密钥）
❌ 本地 IDE 配置不提交

最佳实践：
1. 共通配置提交到 git
2. 每个开发者本地创建 application-local.yml
3. 在 .gitignore 中忽略本地配置
```

### 6. 配置的日志记录

```java
@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppConfig {
    
    @PostConstruct
    public void logConfig() {
        log.info("应用配置已加载:");
        log.info("  应用名: {}", name);
        log.info("  版本: {}", version);
        log.info("  启用缓存: {}", features.caching);
        // 注意：不要输出敏感信息（密钥、密码等）
    }
}
```

---

## 总结对比

### 三种注入方式的对比

| 特性 | @Value | @ConfigurationProperties | Environment |
|-----|--------|------------------------|-------------|
| **适用场景** | 单个属性 | 一组属性 | 动态读取 |
| **代码复用性** | 低 | 高 | 中 |
| **类型安全** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| **支持验证** | 需自己实现 | 原生支持 | 需自己实现 |
| **性能** | 快 | 快 | 中等 |
| **可读性** | 好 | 很好 | 中等 |
| **学习曲线** | 简单 | 中等 | 简单 |

### 何时使用哪种方式？

```
选择流程：

是否只注入 1-2 个属性？
  ├─ 是 → 使用 @Value
  └─ 否 → 下一问
  
属性之间有关联标吗？
  ├─ 是 → 使用 @ConfigurationProperties
  └─ 否 → 使用 @Value
  
需要条件判断配置吗？
  ├─ 是 → 使用 Environment
  └─ 否 → 使用上面选择的方式
```

---

## 实练习题

### 练习 1：使用 @Value 注入 LLM 配置

创建一个 Service，使用 @Value 注入 llm.deepseek 的所有属性。

**答案：**
```java
@Component
public class LlmService {
    @Value("${llm.deepseek.enabled}")
    private boolean enabled;
    
    @Value("${llm.deepseek.model}")
    private String model;
    
    @Value("${llm.deepseek.temperature}")
    private double temperature;
    
    @Value("${llm.deepseek.max-tokens}")
    private int maxTokens;
}
```

### 练习 2：创建 Agent 配置类

使用 @ConfigurationProperties 为 agent 相关配置创建一个配置类。

**答案：**
```java
@Component
@ConfigurationProperties(prefix = "agent")
@Data
public class AgentProperties {
    
    private int maxIterations;
    private int timeout;
    private boolean enableStreaming;
    
    private Session session = new Session();
    private Knowledge knowledge = new Knowledge();
    
    @Data
    public static class Session {
        private String storagePath;
    }
    
    @Data
    public static class Knowledge {
        private boolean enabled;
        private String storagePath;
        private int topK;
        private String embeddingModel;
    }
}
```

### 练习 3：环境感知的日志配置

使用 Environment 根据不同环境设置不同的日志级别。

**答案：**
```java
@Component
public class LoggingInitializer {
    
    @Autowired
    private Environment env;
    
    @PostConstruct
    public void initLogging() {
        String profile = env.getProperty("spring.profiles.active");
        
        if ("prod".equals(profile)) {
            setLogLevel("WARN");
        } else if ("test".equals(profile)) {
            setLogLevel("INFO");
        } else {
            setLogLevel("DEBUG");
        }
    }
}
```

---

## 常见问题（FAQ）

### Q1: 如何在单元测试中覆盖配置？

```java
@SpringBootTest
@TestPropertySource(properties = {
    "llm.deepseek.temperature=0.5",
    "server.port=8888"
})
public class MyTest {
    // 这些配置只在测试时生效
}
```

### Q2: 如何使用 application.properties 代替 YAML？

```properties
# application.properties
llm.deepseek.model=deepseek-chat
llm.deepseek.temperature=0.7
llm.deepseek.max-tokens=4096
app.name=My Application
```

### Q3: 如何按优先级加载多个 YAML 文件？

```yaml
spring:
  config:
    import:
      - "optional:file:/etc/config/application.yml"
      - "classpath:/application-${spring.profiles.active}.yml"
```

### Q4: 如何动态刷新配置（不重启应用）？

需要使用 Spring Cloud Config 或更高级的配置中心（Nacos、Apollo 等）。

### Q5: 配置中包含 ${} 应该怎么处理？

```yaml
# 如果配置值本身需要包含 ${}
message: $${placeholder}  # 使用 $${} 会被转换成 ${}
```

---

## 下一步

学完阶段 2，你已经掌握：

✅ 配置文件的组织和加载机制
✅ @Value 和 @ConfigurationProperties 的使用
✅ 多环境配置的实践
✅ Environment 的动态读取
✅ 配置的最佳实践

🎯 下一阶段（阶段 3）将学习：**业务逻辑层的设计与实现**

---

## 进阶资源

- [Spring Boot 官方文档 - Externalized Configuration](https://spring.io/projects/spring-boot)
- [Spring 参考文档 - Profile](https://docs.spring.io/spring-framework/reference/)
- [12-factor app - Config](https://12factor.net/config)
