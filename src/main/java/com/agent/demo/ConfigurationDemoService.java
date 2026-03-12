package com.agent.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置管理演示服务
 * 
 * 展示 Spring Boot 中配置的加载、注入和使用
 */
@Slf4j
@Service
public class ConfigurationDemoService {

    @Autowired
    private Environment environment; // Spring 的环境对象，包含所有配置

    @Autowired
    private AppConfigProperties appConfig;

    @Autowired
    private ConfigValueDemo valueDemo;

    /**
     * 演示 1：配置的加载链
     */
    public Map<String, Object> explainConfigurationChain() {
        Map<String, Object> result = new HashMap<>();

        result.put("📁 配置文件加载链", new HashMap<String, Object>() {
            {
                put("第 1 步", "JVM 启动时读取 application.yml（主配置）");
                put("第 2 步", "根据 spring.profiles.active 读取 application-{profile}.yml");
                put("第 3 步", "合并配置（后加载的覆盖先加载的同名属性）");
                put("第 4 步", "环境变量和命令行参数优先级最高");
                put("第 5 步", "所有配置存储在 Spring 的 Environment 对象中");
            }
        });

        result.put("🎯 你项目的配置加载顺序", new HashMap<String, Object>() {
            {
                put("1️⃣  加载 application.yml", "基础配置，总是加载");
                put("2️⃣  读取激活的 profile",
                        "当前设置为: " + (environment.getActiveProfiles().length > 0 ? 
                              environment.getActiveProfiles()[0] : "未设置"));
                put("3️⃣  合并特定环境配置", "如果有 application-dev.yml 等");
                put("4️⃣  配置准备就绪", "✅ Bean 可以使用 @Value 注入了");
            }
        });

        return result;
    }

    /**
     * 演示 2：@ConfigurationProperties 绑定
     */
    public Map<String, Object> demonstrateConfigurationProperties() {
        Map<String, Object> result = new HashMap<>();

        result.put("🔧 @ConfigurationProperties 配置绑定", new HashMap<String, Object>() {
            {
                put("原理", "自动将 YAML 配置绑定到 Java Bean");
                put("前缀", "通过 @ConfigurationProperties(prefix=\"app\") 指定");
                put("YAML 结构", "app: { name: xxx, version: 1.0, features: { caching: true } }");
                put("Java 对象", "AppConfigProperties 中的对应字段");
                put("自动转换", "✅ YAML → Java 类型（String、int、boolean 等）");
            }
        });

        result.put("加载的配置值（来自 AppConfigProperties）", new HashMap<String, Object>() {
            {
                put("应用名", appConfig.getName() != null ? appConfig.getName() : "未配置");
                put("版本", appConfig.getVersion() != null ? appConfig.getVersion() : "未配置");
                put("缓存功能启用", appConfig.getFeatures().isCaching());
                put("监控功能启用", appConfig.getFeatures().isMonitoring());
                put("最大连接数", appConfig.getFeatures().getMaxConnections());
            }
        });

        result.put("✨ 优势", new HashMap<String, Object>() {
            {
                put("1", "✅ 类型安全：自动转换和类型检查");
                put("2", "✅ 结构清晰：复杂配置可以组织成嵌套对象");
                put("3", "✅ 易于维护：一个 Java 类对应一组配置");
                put("4", "✅ 支持验证：可以添加 @Valid 进行验证");
            }
        });

        return result;
    }

    /**
     * 演示 3：@Value 注入单个属性
     */
    public Map<String, Object> demonstrateValueAnnotation() {
        Map<String, Object> result = new HashMap<>();

        result.put("💎 @Value 注解注入单个属性", new HashMap<String, Object>() {
            {
                put("原理", "@Value 提取配置文件中的单个值");
                put("语法", "@Value(\"${属性路径}\")");
                put("默认值", "@Value(\"${属性:默认值}\")");
                put("支持类型", "String、int、boolean、double 等");
                put("支持表达式", "@Value(\"#{expression}\")");
            }
        });

        result.put("注入的配置值（来自 ConfigValueDemo）", new HashMap<String, Object>() {
            {
                put("应用名", valueDemo.getApplicationName());
                put("LLM 模型", valueDemo.getLlmModel());
                put("温度参数", valueDemo.getTemperature());
                put("最大 Token", valueDemo.getMaxTokens());
                put("自定义功能启用", valueDemo.isCustomFeatureEnabled());
            }
        });

        result.put("🎯 @Value vs @ConfigurationProperties", new HashMap<String, Object>() {
            {
                put("@Value", "• 注入单个属性\n• 简单配置\n• 代码分散");
                put("@ConfigurationProperties", "• 注入一组属性\n• 复杂配置\n• 代码集中\n• 推荐用于大型配置");
                put("建议", "简单配置用 @Value，复杂配置用 @ConfigurationProperties");
            }
        });

        return result;
    }

    /**
     * 演示 4：多环境配置
     */
    public Map<String, Object> demonstrateMultiEnvironment() {
        Map<String, Object> result = new HashMap<>();

        result.put("🌍 多环境配置（环境隔离）", new HashMap<String, Object>() {
            {
                put("目标", "同一套代码，在不同环境配置不同参数");
                put("文件结构", "application.yml（共通）\n" +
                        "application-dev.yml（开发环境）\n" +
                        "application-test.yml（测试环境）\n" +
                        "application-prod.yml（生产环境）");
                put("激活方式", "spring.profiles.active = dev/test/prod");
                put("你项目的设置",
                        "当前激活: " + (environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0]
                                : "未设置（使用默认）"));
            }
        });

        result.put("📋 配置文件对比示例", new HashMap<String, Object>() {
            {
                put("application.yml（共通）", new HashMap<String, Object>() {
                    {
                        put("server.port", "8080");
                        put("logging.level", "INFO");
                        put("llm.deepseek.model", "deepseek-chat");
                    }
                });

                put("application-dev.yml（开发）", new HashMap<String, Object>() {
                    {
                        put("server.port", "9090 ✓覆盖");
                        put("logging.level", "DEBUG ✓覆盖");
                        put("llm.deepseek.temperature", "1.0（创意调试）");
                    }
                });

                put("application-prod.yml（生产）", new HashMap<String, Object>() {
                    {
                        put("server.port", "8080 ✓保持");
                        put("logging.level", "WARN ✓严格");
                        put("llm.deepseek.temperature", "0.3（保守生成）");
                    }
                });
            }
        });

        result.put("🔧 如何切换环境", new HashMap<String, Object>() {
            {
                put("方式 1", "修改 application.yml 中的 spring.profiles.active");
                put("方式 2", "运行时参数: java -jar xxx.jar --spring.profiles.active=prod");
                put("方式 3", "环境变量: SPRING_PROFILES_ACTIVE=prod");
                put("方式 4", "IDE 配置: 在 VM options 中设置 -Dspring.profiles.active=dev");
            }
        });

        return result;
    }

    /**
     * 演示 5：使用 Environment 对象动态读取配置
     */
    public Map<String, Object> demonstrateEnvironmentAccess() {
        Map<String, Object> result = new HashMap<>();

        result.put("🔍 使用 Environment 动态读取配置", new HashMap<String, Object>() {
            {
                put("什么是 Environment", "Spring 的环境对象，包含所有配置");
                put("优势", "可以在运行时动态读取配置，不需要预先注入");
                put("使用场景", "条件判断、动态配置切换");
            }
        });

        result.put("Environment 读取配置示例", new HashMap<String, Object>() {
            {
                put("environment.getProperty(\"llm.deepseek.model\")",
                        environment.getProperty("llm.deepseek.model"));
                put("environment.getProperty(\"server.port\")",
                        environment.getProperty("server.port"));
                put("environment.getProperty(\"不存在的属性\", \"默认值\")",
                        environment.getProperty("不存在的属性", "默认值"));
                put("environment.getActiveProfiles()[0]（当前激活的环境）",
                        environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0] : "未设置");
            }
        });

        result.put("✅ 何时使用 Environment", new HashMap<String, Object>() {
            {
                put("场景 1", "需要条件判断配置值");
                put("场景 2", "动态读取配置（配置可能在运行时改变）");
                put("场景 3", "编写通用工具类，需要访问多个配置");
                put("场景 4", "不想使用 @Value 或 @ConfigurationProperties");
            }
        });

        return result;
    }

    /**
     * 演示 6：配置验证和类型转换
     */
    public Map<String, Object> demonstrateConfigValidation() {
        Map<String, Object> result = new HashMap<>();

        result.put("✔️ 配置验证和类型转换", new HashMap<String, Object>() {
            {
                put("Spring 的自动转换", new HashMap<String, Object>() {
                    {
                        put("YAML string → Java String", "直接转换");
                        put("YAML number → Java int/long/double", "自动解析");
                        put("YAML boolean → Java boolean", "true/false 转换");
                        put("YAML 列表 → Java List/Set", "集合转换");
                        put("YAML 块 → Java 对象", "递归绑定");
                    }
                });
            }
        });

        result.put("配置验证示例", new HashMap<String, Object>() {
            {
                put("@Min/@Max", "验证数字范围: @Min(1) @Max(100) int port;");
                put("@NotNull/@NotEmpty", "验证非空: @NotEmpty String apiKey;");
                put("@Pattern", "正则验证: @Pattern(regexp=\"...\") String email;");
                put("自定义验证", "实现 Validator 接口");
            }
        });

        result.put("💡 最佳实践", new HashMap<String, Object>() {
            {
                put("1", "对配置类添加 @Validated 注解");
                put("2", "在字段上添加验证注解");
                put("3", "Spring 会在启动时验证，配置错误立即报错");
                put("4", "这样可以尽早发现配置错误，而不是运行时崩溃");
            }
        });

        return result;
    }
}
