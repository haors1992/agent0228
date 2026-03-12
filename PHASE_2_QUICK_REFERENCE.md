# ⚡ 配置管理快速参考

## 1️⃣ 三种注入方式一览

### @Value（简单配置）

```java
@Component
public class MyService {
    @Value("${llm.deepseek.model}")
    private String model;
    
    @Value("${llm.deepseek.temperature:0.7}")
    private double temperature;  // 有默认值
}
```

**配置文件：**
```yaml
llm:
  deepseek:
    model: deepseek-chat
    temperature: 0.7
```

### @ConfigurationProperties（复杂配置）

```java
@Component
@ConfigurationProperties(prefix = "llm.deepseek")
@Data
public class LlmConfig {
    private String model;
    private double temperature;
    private int maxTokens;
}
```

**使用：**
```java
@Autowired
private LlmConfig llmConfig;

String model = llmConfig.getModel();
```

### Environment（动态读取）

```java
@Autowired
private Environment env;

String model = env.getProperty("llm.deepseek.model");
String timeout = env.getProperty("custom.timeout", "120");
```

---

## 2️⃣ 配置加载优先级

```
1. application.yml
2. application-{profile}.yml
3. 环境变量
4. 命令行参数 --key=value
   ↑
   优先级递增
```

**激活环境：**
```yaml
spring:
  profiles:
    active: dev  # 加载 application-dev.yml
```

---

## 3️⃣ 常用配置片段

### 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
```

### 日志配置

```yaml
logging:
  level:
    root: INFO
    com.agent: DEBUG
    org.springframework: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
  file:
    name: logs/app.log
```

### 服务器配置

```yaml
server:
  port: 8080
  servlet:
    context-path: /api
  compression:
    enabled: true
    min-response-size: 1024
```

### 自定义配置

```yaml
app:
  name: my-app
  version: 1.0.0
  features:
    cache: true
    monitor: false
```

```java
@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppConfig {
    private String name;
    private String version;
    private Features features;
    
    @Data
    public static class Features {
        private boolean cache;
        private boolean monitor;
    }
}
```

---

## 4️⃣ 环境特定配置

### 开发环境（application-dev.yml）

```yaml
logging:
  level:
    com.agent: DEBUG

server:
  port: 8080

spring:
  jpa:
    show-sql: true
```

### 生产环境（application-prod.yml）

```yaml
logging:
  level:
    root: WARN
    com.agent: WARN

server:
  port: 8080
  compression:
    enabled: true

spring:
  jpa:
    show-sql: false
```

---

## 5️⃣ 配置验证

```java
@Component
@ConfigurationProperties(prefix = "app")
@Data
@Validated
public class AppConfig {
    
    @NotNull
    private String name;
    
    @Min(1) 
    @Max(65535)
    private int port;
    
    @Email
    private String email;
}
```

---

## 6️⃣ 激活不同环境

### 方式 1：修改配置文件
```yaml
spring:
  profiles:
    active: prod
```

### 方式 2：命令行参数
```bash
java -jar app.jar --spring.profiles.active=prod
```

### 方式 3：环境变量
```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar app.jar
```

### 方式 4：IDE 配置（IntelliJ）
```
Run → Edit Configurations
VM options: -Dspring.profiles.active=dev
```

---

## 7️⃣ 配置最佳实践

### ✅ 应该做

```java
// 1. 使用 @ConfigurationProperties 组织一组配置
@ConfigurationProperties(prefix = "cache")
public class CacheConfig {
    private long ttl;
    private int size;
}

// 2. 使用类型安全的配置类
private CacheConfig cache;  // 类型安全

// 3. 添加验证注解
@NotNull @Min(1000) 
private long ttl;

// 4. 记录配置加载日志
@PostConstruct
public void logConfig() {
    log.info("缓存配置: TTL={}, Size={}", ttl, size);
}
```

### ❌ 避免做

```java
// 1. 过度使用 @Value
@Value("${cache.ttl}") private long ttl;
@Value("${cache.size}") private int size;
// → 改为使用 @ConfigurationProperties

// 2. 混合多种注入方式
@Autowired
private CacheConfig cache;  // 这个
@Value("${cache.ttl}")
private long ttl;  // 和这个

// 3. 直接在代码中写死配置
int MAX_SIZE = 1000;  // → 应该放在配置文件

// 4. 输出敏感信息到日志
log.info("API 密钥: {}", apiKey);  // 危险！
```

---

## 8️⃣ 常见问题速查

| 问题 | 解决方案 |
|-----|--------|
| 配置未生效 | 检查 `spring.profiles.active` 和文件名 |
| 属性为 null | 检查配置文件路径和 @Value 的属性名 |
| 类型转换错误 | 检查 YAML 类型是否匹配 Java 类型 |
| 循环引用 | 使用 @Lazy 或 ObjectProvider 延迟注入 |
| 测试配置冲突 | 使用 @TestPropertySource 覆盖 |

---

## 9️⃣ 项目配置调试

### 查看所有加载的属性

```java
@Component
public class ConfigDebugger {
    
    @Autowired
    private Environment env;
    
    @PostConstruct
    public void printAllProperties() {
        MutablePropertySources sources = 
            ((AbstractEnvironment) env).getPropertySources();
        sources.forEach(ps -> {
            log.info("PropertySource: {}", ps.getName());
            if (ps instanceof EnumerablePropertySource) {
                EnumerablePropertySource epc = 
                    (EnumerablePropertySource) ps;
                epc.getPropertyNames().forEach(name -> {
                    System.out.println(name + "=" + env.getProperty(name));
                });
            }
        });
    }
}
```

### 查看当前激活的环境

```bash
# 查看日志中是否有以下信息
# The following profiles are active: dev,test

# 或编程查询
String[] profiles = env.getActiveProfiles();
```

---

## 🔟 配置合并规则

```
【规则】高优先级配置会完全覆盖低优先级配置

application.yml:
  llm:
    temperature: 0.7
    max-tokens: 4096

application-dev.yml:
  llm:
    temperature: 1.0

【结果】
  llm:
    temperature: 1.0  ← 被覆盖
    max-tokens: 4096  ← 保留基础值
```

---

## 📚 演示端点快速调用

```bash
# 1. 配置链
curl http://localhost:8080/api/agent/demo-config-chain

# 2. ConfigurationProperties
curl http://localhost:8080/api/agent/demo-config-properties

# 3. Value 注入
curl http://localhost:8080/api/agent/demo-value-injection

# 4. 多环境
curl http://localhost:8080/api/agent/demo-multi-environment

# 5. Environment 访问
curl http://localhost:8080/api/agent/demo-environment-access

# 6. 验证和转换
curl http://localhost:8080/api/agent/demo-config-validation
```

美化输出：
```bash
curl http://localhost:8080/api/agent/demo-config-chain | jq '.'
```

---

## 🎯 总结决策树

```
需要配置一个值？
├─ 只是 1-2 个简单属性
│  └─ 用 @Value
├─ 一组相关的属性
│  └─ 用 @ConfigurationProperties
└─ 需要条件判断或动态读取
   └─ 用 Environment
```

---

## 🚀 下一步行动

1. ✅ 理解三种注入方式的使用场景
2. ✅ 在你的项目中创建 @ConfigurationProperties 类
3. ✅ 创建 application-test.yml 和 application-prod.yml
4. ✅ 调用演示端点理解配置加载流程
5. 🎯 开始阶段 3：业务逻辑层设计

---

## 💡 记住这些

1. **单一职责**：一个配置类对应一组相关配置
2. **类型安全**：使用 @ConfigurationProperties 获得类型检查
3. **环境隔离**：不同环境用不同的 application-*.yml
4. **敏感信息**：密钥用环境变量而不是配置文件
5. **默认值**：用 @Value("${key:default}") 提供合理默认值
