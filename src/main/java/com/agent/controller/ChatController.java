package com.agent.controller;

import com.agent.demo.BeanContainerDemoService;
import com.agent.demo.BeanDemoService;
import com.agent.demo.CircularDependencyDemoService;
import com.agent.demo.ConfigurationDemoService;
import com.agent.model.dto.ChatSession;
import com.agent.model.dto.ConversationMessage;
import com.agent.reasoning.engine.ExecutionContext;
import com.agent.reasoning.engine.ReasoningEngine;
import com.agent.service.SessionManager;
import com.agent.streaming.StreamingResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Chat Controller
 * 
 * REST API endpoints for agent interactions
 * 支持多轮对话历史存储
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
public class ChatController {

    private final ReasoningEngine reasoningEngine;
    private final SessionManager sessionManager;
    private final BeanDemoService beanDemoService;
    private final BeanContainerDemoService beanContainerDemoService;
    private final CircularDependencyDemoService circularDependencyDemoService; // ← 断依赖演示
    private final ConfigurationDemoService configurationDemoService; // ← 配置管理演示

    /**
     * 构造器注入
     * 
     * Spring 看到这个构造函数会自动注入所有依赖！
     * 此外，circularDependencyDemoService 提供的两个 Service
     * 其实有互相依赖，但 Spring 已自动解决！
     */
    public ChatController(ReasoningEngine reasoningEngine,
            SessionManager sessionManager,
            BeanDemoService beanDemoService,
            BeanContainerDemoService beanContainerDemoService,
            CircularDependencyDemoService circularDependencyDemoService,
            ConfigurationDemoService configurationDemoService) {
        this.reasoningEngine = reasoningEngine;
        this.sessionManager = sessionManager;
        this.beanDemoService = beanDemoService;
        this.beanContainerDemoService = beanContainerDemoService;
        this.circularDependencyDemoService = circularDependencyDemoService;
        this.configurationDemoService = configurationDemoService;
        log.info("✅ ChatController 构造完成，所有依赖已自动注入！");
    }

    /**
     * Chat endpoint - Execute agent reasoning for a user query
     * 支持会话历史存储
     * 
     * @param request Request body containing the user query and optional sessionId
     * @return Agent response with result, steps, duration and sessionId
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        log.info("Received chat request: {}", request.getQuery());

        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Query cannot be empty");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            // 获取或创建会话
            String sessionId = request.getSessionId();
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = UUID.randomUUID().toString();
            }
            ChatSession session = sessionManager.getOrCreateSession(sessionId);

            long startTime = System.currentTimeMillis();

            // 添加用户消息到历史
            session.addMessage("user", request.getQuery());

            // 构建对话历史上下文
            List<String> conversationHistory = session.getMessages().stream()
                    .map(msg -> msg.getRole().toUpperCase() + ": " + msg.getContent())
                    .collect(Collectors.toList());

            // Execute the agent reasoning with conversation context
            ExecutionContext context = reasoningEngine.execute(request.getQuery(), conversationHistory);

            // 添加助手回复到历史
            session.addMessage("assistant", context.getFinalAnswer());

            // 保存会话
            sessionManager.saveSession(session);

            long duration = System.currentTimeMillis() - startTime;

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("result", context.getFinalAnswer());
            response.put("iterations", context.getCurrentIteration());
            response.put("duration_ms", duration);
            response.put("is_complete", context.getIsComplete());
            response.put("messageCount", session.getMessageCount());

            // Add detailed steps if requested
            if (request.isIncludeDetails()) {
                response.put("steps", context.getThoughtActions().stream()
                        .map(ta -> {
                            Map<String, Object> step = new HashMap<>();
                            step.put("thought", ta.getThought() != null ? ta.getThought() : "");
                            step.put("action", ta.getAction() != null ? ta.getAction() : "");
                            step.put("action_input", ta.getActionInput() != null ? ta.getActionInput() : "");
                            return step;
                        })
                        .collect(Collectors.toList()));

                response.put("tool_results", context.getToolResults().stream()
                        .map(tr -> {
                            Map<String, Object> result = new HashMap<>();
                            result.put("tool_name", tr.getToolName());
                            result.put("result", tr.getResult());
                            result.put("success", tr.getSuccess());
                            result.put("execution_time_ms", tr.getExecutionTimeMs());
                            result.put("error", tr.getError() != null ? tr.getError() : "");
                            return result;
                        })
                        .collect(Collectors.toList()));
            }

            log.info("Chat request completed in {}ms with {} iterations, sessionId: {}",
                    duration, context.getCurrentIteration(), sessionId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing chat request", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 流式响应端点 - Server-Sent Events (SSE)
     * 实时流式传输 AI 响应
     * 
     * @param request 用户查询和会话 ID
     * @return SSE 流式响应，消息实时推送给客户端
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
        StreamingResponseHandler handler = new StreamingResponseHandler(emitter);

        // 在后台线程中异步处理请求
        handler.executeAsync(() -> {
            try {
                if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                    handler.sendError("Query cannot be empty");
                    try {
                        handler.close();
                    } catch (Exception e) {
                        log.error("❌ Error closing handler", e);
                    }
                    return;
                }

                log.info("🔄 Received streaming chat request: {}", request.getQuery());

                // 获取或创建会话
                String sessionId = request.getSessionId();
                if (sessionId == null || sessionId.isEmpty()) {
                    sessionId = UUID.randomUUID().toString();
                }
                ChatSession session = sessionManager.getOrCreateSession(sessionId);

                long startTime = System.currentTimeMillis();

                // 添加用户消息到历史
                session.addMessage("user", request.getQuery());

                // 发送会话 ID
                Map<String, String> sessionInfo = new HashMap<>();
                sessionInfo.put("sessionId", sessionId);
                handler.sendChunk("Session: " + sessionId + "\n");

                // 构建对话历史上下文
                List<String> conversationHistory = session.getMessages().stream()
                        .map(msg -> msg.getRole().toUpperCase() + ": " + msg.getContent())
                        .collect(Collectors.toList());

                // 发送开始信息
                handler.sendChunk("🤔 Reasoning...\n");

                // 执行推理引擎（可以逐步发送步骤信息）
                ExecutionContext context = reasoningEngine.execute(request.getQuery(), conversationHistory);

                // 发送逐个单词的流式响应
                String finalAnswer = context.getFinalAnswer();
                handler.sendChunk("\n📝 Response:\n");

                // 模拟流式传输：按句子分割返回
                String[] sentences = finalAnswer.split("(?<=[。！？；])|(?<=[.!?;])");
                for (String sentence : sentences) {
                    if (handler.isActive() && !sentence.trim().isEmpty()) {
                        handler.sendChunk(sentence.trim() + " ");
                        // 模拟延迟，使流式传输更明显
                        Thread.sleep(50);
                    }
                }

                // 添加助手回复到历史
                session.addMessage("assistant", finalAnswer);

                // 保存会话
                sessionManager.saveSession(session);

                long duration = System.currentTimeMillis() - startTime;

                // 构建最终结果
                Map<String, Object> finalResult = new HashMap<>();
                finalResult.put("sessionId", sessionId);
                finalResult.put("messageCount", session.getMessageCount());
                finalResult.put("duration_ms", duration);
                finalResult.put("iterations", context.getCurrentIteration());
                finalResult.put("is_complete", context.getIsComplete());

                // 如果请求了详细信息
                if (request.isIncludeDetails()) {
                    finalResult.put("steps", context.getThoughtActions().stream()
                            .map(ta -> {
                                Map<String, Object> step = new HashMap<>();
                                step.put("thought", ta.getThought() != null ? ta.getThought() : "");
                                step.put("action", ta.getAction() != null ? ta.getAction() : "");
                                step.put("action_input", ta.getActionInput() != null ? ta.getActionInput() : "");
                                return step;
                            })
                            .collect(Collectors.toList()));
                }

                // 发送完成标记
                handler.sendComplete(finalResult);

                log.info("✅ Streaming response completed in {}ms, sessionId: {}", duration, sessionId);

            } catch (Exception e) {
                log.error("❌ Error in streaming chat", e);
                handler.sendError("Error: " + e.getMessage());
            } finally {
                try {
                    handler.close();
                } catch (Exception e) {
                    log.error("❌ 关闭流式处理器时出错: {}", e.getMessage());
                }
            }
        });

        return emitter;
    }

    /**
     * 【演示 5】容器统计信息 - Spring 管理的所有 Bean
     * 
     * 访问 /api/agent/demo-container-stats
     * 查看 Spring 容器中有多少个 Bean，它们如何被管理
     */
    @GetMapping("/demo-container-stats")
    public ResponseEntity<Map<String, Object>> demoContainerStats() {
        log.info("📊 演示端点被调用：/demo-container-stats");

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> stats = beanContainerDemoService.getContainerStatistics();

        result.put("✅ Spring 容器统计信息", stats);
        result.put("说明", "以上数据来自 Spring ApplicationContext 容器，" +
                "它是一个中央管理器，管理所有的 Bean");

        return ResponseEntity.ok(result);
    }

    /**
     * 【演示 6】Bean 唯一性 - 为什么不会乱
     * 
     * 访问 /api/agent/demo-bean-uniqueness
     * 看同一个 Bean 多次获取是否是同一个对象
     */
    @GetMapping("/demo-bean-uniqueness")
    public ResponseEntity<Map<String, Object>> demoBeanUniqueness() {
        log.info("🔑 演示端点被调用：/demo-bean-uniqueness");

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> uniqueness = beanContainerDemoService.demonstrateUniqueness();

        result.put("🔐 Bean 的唯一性演示", uniqueness);
        result.put("核心结论", "✅ 同一个 Bean 多次获取，HashCode 完全相同，" +
                "说明是同一个对象！Spring 保证了 Bean 的唯一性和一致性");

        return ResponseEntity.ok(result);
    }

    /**
     * 【演示 7】Bean 隔离性 - 不同 Bean 互不干扰
     * 
     * 访问 /api/agent/demo-bean-isolation
     * 看不同的 Bean 是否是不同的对象
     */
    @GetMapping("/demo-bean-isolation")
    public ResponseEntity<Map<String, Object>> demoBeanIsolation() {
        log.info("🛡️ 演示端点被调用：/demo-bean-isolation");

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> isolation = beanContainerDemoService.demonstrateIsolation();

        result.put("🔒 Bean 的隔离性演示", isolation);
        result.put("核心结论", "✅ 不同的 Bean 有不同的 HashCode，" +
                "是完全不同的对象，互不干扰，保证了系统的独立性");

        return ResponseEntity.ok(result);
    }

    /**
     * 【演示 8】中央管理 - Spring 容器的强大
     * 
     * 访问 /api/agent/demo-central-management
     * 看 Spring 如何完全控制 Bean 的生命周期
     */
    @GetMapping("/demo-central-management")
    public ResponseEntity<Map<String, Object>> demoCentralManagement() {
        log.info("🎛️ 演示端点被调用：/demo-central-management");

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> management = beanContainerDemoService.demonstrateCentralManagement();

        result.put("🏭 Spring 容器的中央管理", management);
        result.put("核心结论", "✅ Spring 容器是一个中央管理器，完全控制了 Bean 的整个生命周期，" +
                "从创建到销毁，你不用担心任何事情");

        return ResponseEntity.ok(result);
    }

    /**
     * 【演示 9】状态一致性 - 为什么不会状态不一致
     * 
     * 访问 /api/agent/demo-consistency
     * 看为什么使用 Spring 的 Bean 不会状态混乱
     */
    @GetMapping("/demo-consistency")
    public ResponseEntity<Map<String, Object>> demoConsistency() {
        log.info("📝 演示端点被调用：/demo-consistency");

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> consistency = beanContainerDemoService.demonstrateConsistency();

        result.put("🔄 Bean 状态一致性演示", consistency);
        result.put("核心结论", "✅ 因为所有 Bean 都是 Singleton（单例），" +
                "所以不会有多个实例导致状态不一致的问题");

        return ResponseEntity.ok(result);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        return ResponseEntity.ok(response);
    }

    /**
     * 【演示 10】循环依赖详解
     * 
     * 访问 /api/agent/demo-circular-dependency
     * 彻底理解 Spring 如何解决循环依赖
     */
    @GetMapping("/demo-circular-dependency")
    public ResponseEntity<Map<String, Object>> demoCircularDependency() {
        log.info("📌 演示端点被调用：/demo-circular-dependency");

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> explanation = circularDependencyDemoService.explainCircularDependency();

        result.put("🔄 循环依赖详解", explanation);
        result.put("说明", "循环依赖本是一个很难的问题，但 Spring 用三级缓存机制自动解决了");

        return ResponseEntity.ok(result);
    }

    /**
     * 【演示 11】没有 Spring 的灾难
     * 
     * 访问 /api/agent/demo-without-spring
     * 看看没有 Spring 时会发生什么
     */
    @GetMapping("/demo-without-spring")
    public ResponseEntity<Map<String, Object>> demoWithoutSpring() {
        log.info("💔 演示端点被调用：/demo-without-spring");

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> disaster = circularDependencyDemoService.demonstrateWithoutSpring();

        result.put("💔 没有 Spring 的灾难", disaster);
        result.put("结论", "❌ 循环依赖会导致栈溢出，程序无法启动");

        return ResponseEntity.ok(result);
    }

    /**
     * 【演示 12】Spring 的三级缓存魔法
     * 
     * 访问 /api/agent/demo-three-level-cache
     * 看 Spring 如何用三级缓存自动解决循环依赖
     */
    @GetMapping("/demo-three-level-cache")
    public ResponseEntity<Map<String, Object>> demoThreeLevelCache() {
        log.info("✨ 演示端点被调用：/demo-three-level-cache");

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> magic = circularDependencyDemoService.demonstrateSpringMagic();

        result.put("✨ Spring 的三级缓存魔法", magic);
        result.put("核心", "Spring 用三级缓存（一级、二级、三级）巧妙地解决了循环依赖");

        return ResponseEntity.ok(result);
    }

    /**
     * 【演示 13】循环依赖真实证明
     * 
     * 访问 /api/agent/demo-circular-proof
     * 实际验证循环依赖的存在
     */
    @GetMapping("/demo-circular-proof")
    public ResponseEntity<Map<String, Object>> demoCircularProof() {
        log.info("🔍 演示端点被调用：/demo-circular-proof");

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> proof = circularDependencyDemoService.demonstrateProof();

        result.put("🔍 循环依赖真实存在的证明", proof);
        result.put("关键点", "ServiceA 有 ServiceB，ServiceB 有 ServiceA，形成了循环");

        return ResponseEntity.ok(result);
    }

    /**
     * 【演示 14】Spring 无法解决的循环依赖
     * 
     * 访问 /api/agent/demo-unsolvable
     * 了解那些 Spring 自动解决不了的循环依赖
     */
    @GetMapping("/demo-unsolvable-circular")
    public ResponseEntity<Map<String, Object>> demoUnsolvableCircular() {
        log.info("⚠️ 演示端点被调用：/demo-unsolvable-circular");

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> unsolvable = circularDependencyDemoService.demonstrateUnsolvable();

        result.put("❌ Spring 无法自动解决的循环依赖", unsolvable);
        result.put("重点", "不是所有循环依赖都能被解决，某些情况下需要手动干预");

        return ResponseEntity.ok(result);
    }

    /**
     * 【演示 15】解决循环依赖的最佳实践
     * 
     * 访问 /api/agent/demo-resolve-circular
     * 学习如何从根本上解决循环依赖问题
     */
    @GetMapping("/demo-resolve-circular")
    public ResponseEntity<Map<String, Object>> demoResolveCircular() {
        log.info("💡 演示端点被调用：/demo-resolve-circular");

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> practices = circularDependencyDemoService.demonstrateBestPractices();

        result.put("💡 解决循环依赖的最佳实践", practices);
        result.put("建议", "优先采用方案 3（重新设计架构），这是最根本的解决方案");

        return ResponseEntity.ok(result);
    }

    // ==================== 阶段 2：配置管理演示端点 ====================

    /**
     * 【阶段 2 演示 1】配置文件加载链
     * 
     * 访问 /api/agent/demo-config-chain
     * 了解 Spring Boot 如何加载和合并配置文件
     */
    @GetMapping("/demo-config-chain")
    public ResponseEntity<Map<String, Object>> demoConfigChain() {
        log.info("📍 演示端点被调用：/demo-config-chain");

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> chain = configurationDemoService.explainConfigurationChain();

        result.put("🔄 配置文件加载链", chain);
        result.put("关键点", "Spring Boot 优先级：application.yml < application-{profile}.yml < 环境变量 < 命令行参数");

        return ResponseEntity.ok(result);
    }

    /**
     * 【阶段 2 演示 2】@ConfigurationProperties 配置绑定
     * 
     * 访问 /api/agent/demo-config-properties
     * 学习类型安全的配置绑定模式
     */
    @GetMapping("/demo-config-properties")
    public ResponseEntity<Map<String, Object>> demoConfigProperties() {
        log.info("📍 演示端点被调用：/demo-config-properties");

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> props = configurationDemoService.demonstrateConfigurationProperties();

        result.put("🔧 @ConfigurationProperties 配置绑定", props);
        result.put("优势", "类型安全、结构清晰、易于维护、支持验证");

        return ResponseEntity.ok(result);
    }

    /**
     * 【阶段 2 演示 3】@Value 注解注入单个属性
     * 
     * 访问 /api/agent/demo-value-injection
     * 学习如何使用 @Value 注入配置
     */
    @GetMapping("/demo-value-injection")
    public ResponseEntity<Map<String, Object>> demoValueInjection() {
        log.info("📍 演示端点被调用：/demo-value-injection");

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> values = configurationDemoService.demonstrateValueAnnotation();

        result.put("💎 @Value 注解注入单个属性", values);
        result.put("场景", "简单配置用 @Value，复杂配置用 @ConfigurationProperties");

        return ResponseEntity.ok(result);
    }

    /**
     * 【阶段 2 演示 4】多环境配置
     * 
     * 访问 /api/agent/demo-multi-environment
     * 学习如何配置不同的开发、测试、生产环境
     */
    @GetMapping("/demo-multi-environment")
    public ResponseEntity<Map<String, Object>> demoMultiEnvironment() {
        log.info("📍 演示端点被调用：/demo-multi-environment");

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> env = configurationDemoService.demonstrateMultiEnvironment();

        result.put("🌍 多环境配置", env);
        result.put("建议", "使用 spring.profiles.active 切换环境，实现环境隔离");

        return ResponseEntity.ok(result);
    }

    /**
     * 【阶段 2 演示 5】使用 Environment 动态读取配置
     * 
     * 访问 /api/agent/demo-environment-access
     * 学习如何在运行时动态读取配置
     */
    @GetMapping("/demo-environment-access")
    public ResponseEntity<Map<String, Object>> demoEnvironmentAccess() {
        log.info("📍 演示端点被调用：/demo-environment-access");

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> env = configurationDemoService.demonstrateEnvironmentAccess();

        result.put("🔍 使用 Environment 动态读取配置", env);
        result.put("场景", "需要条件判断配置、动态读取、编写通用工具类");

        return ResponseEntity.ok(result);
    }

    /**
     * 【阶段 2 演示 6】配置验证和类型转换
     * 
     * 访问 /api/agent/demo-config-validation
     * 学习配置参数验证和自动类型转换
     */
    @GetMapping("/demo-config-validation")
    public ResponseEntity<Map<String, Object>> demoConfigValidation() {
        log.info("📍 演示端点被调用：/demo-config-validation");

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> validation = configurationDemoService.demonstrateConfigValidation();

        result.put("✔️ 配置验证和类型转换", validation);
        result.put("关键点", "Spring 自动进行类型转换，可以添加验证注解进行参数校验");

        return ResponseEntity.ok(result);
    }

    // ==================== 【演示 1】Bean 的基本信息 开始 ====================

    /**
     * 【演示 1】Bean 的基本信息
     * 
     * 访问 /api/agent/demo-bean-basic
     * 查看 BeanDemoService Bean 的详细信息
     */
    @GetMapping("/demo-bean-basic")
    public ResponseEntity<Map<String, Object>> demoBeanBasic() {
        log.info("📍 演示端点被调用：/demo-bean-basic");
        log.info("📍 beanDemoService 对象已通过构造器注入！");

        // 使用被注入的 Bean
        Map<String, Object> result = new HashMap<>();
        result.put("演示", "这是 BeanDemoService Bean 提供的信息");
        result.put("Bean 信息", beanDemoService.getBeanInfo());
        result.put("说明", "上面的数据都来自被注入的 beanDemoService 对象！");

        return ResponseEntity.ok(result);
    }

    /**
     * 【演示 2】Bean 的单例性（最重要的特性！）
     * 
     * 访问 /api/agent/demo-bean-singleton
     * 多次调用会看到相同的 HashCode，证明是同一个 Bean 对象
     */
    @GetMapping("/demo-bean-singleton")
    public ResponseEntity<Map<String, Object>> demoBeanSingleton() {
        log.info("📍 演示端点被调用：/demo-bean-singleton");

        Map<String, Object> result = new HashMap<>();

        // 获取 Bean 的 HashCode（对象唯一标识）
        String hashCode1 = beanDemoService.getHashCode();
        String hashCode2 = beanDemoService.getHashCode(); // 第二次获取

        result.put("第一次获取 Bean 的 HashCode", hashCode1);
        result.put("第二次获取 Bean 的 HashCode", hashCode2);
        result.put("相同吗?", hashCode1.equals(hashCode2));
        result.put("解释", "✅ 相同！说明 Spring 容器中只有一个 BeanDemoService 对象（单例）");
        result.put("关键发现", "⭐ 每次注入 BeanDemoService 时，Spring 都返回同一个对象！");

        return ResponseEntity.ok(result);
    }

    /**
     * 【演示 3】Bean 的配置数据注入（@Value 注解）
     * 
     * 访问 /api/agent/demo-bean-values
     * 查看如何从 application.yml 注入值到 Bean
     */
    @GetMapping("/demo-bean-values")
    public ResponseEntity<Map<String, Object>> demoBeanValues() {
        log.info("📍 演示端点被调用：/demo-bean-values");

        Map<String, Object> result = new HashMap<>();

        // 从 Bean 获取注入的值
        Map<String, Object> beanInfo = beanDemoService.getBeanInfo();

        result.put("📋 通过 @Value 从 application.yml 注入的值:", new HashMap<String, Object>() {
            {
                put("应用名称 (applicationName)", beanInfo.get("injectedApplicationName"));
                put("LLM 模型 (llmModel)", beanInfo.get("injectedLLMModel"));
            }
        });

        result.put("说明", "这些值来自 application.yml，由 Spring 自动注入到 BeanDemoService Bean 中");
        result.put("Bean 创建时间", beanInfo.get("createdAt"));
        result.put("当前时间", beanInfo.get("currentTime"));

        return ResponseEntity.ok(result);
    }

    /**
     * 【演示 4】Bean 的完整生命周期
     * 
     * 访问 /api/agent/demo-bean-lifecycle
     * 查看 Bean 从创建到现在的所有信息
     */
    @GetMapping("/demo-bean-lifecycle")
    public ResponseEntity<Map<String, Object>> demoBeanLifecycle() {
        log.info("📍 演示端点被调用：/demo-bean-lifecycle");

        Map<String, Object> result = new HashMap<>();

        result.put("🔄 Bean 的完整生命周期:", new HashMap<String, Object>() {
            {
                put("1. 扫描阶段", "Spring 找到 BeanDemoService 类上的 @Component 注解");
                put("2. 创建阶段", "Spring 用反射创建 BeanDemoService 对象实例");
                put("3. 注入阶段", "Spring 发现 @Value 注解，从 application.yml 注入数据");
                put("4. 初始化阶段", "Spring 发现 @PostConstruct 注解，调用 init() 方法");
                put("5. 就绪阶段", "✅ Bean 已创建、初始化完成，可以使用！");
                put("6. 使用阶段", "通过构造器注入到 ChatController，被调用");
            }
        });

        result.put("当前 Bean 的详细信息", beanDemoService.getBeanInfo());
        result.put("✨ 核心发现", "你看到的所有信息都来自一个被 Spring 管理的 Bean！");

        return ResponseEntity.ok(result);
    }

    /**
     * Request body for chat endpoint
     * 支持 sessionId 用于多轮对话
     * 支持 conversationHistory 用于提供上下文
     */
    public static class ChatRequest {
        private String query;
        private String sessionId;
        private List<String> conversationHistory;
        private boolean includeDetails = false;

        public ChatRequest() {
        }

        public ChatRequest(String query) {
            this.query = query;
        }

        public ChatRequest(String query, String sessionId) {
            this.query = query;
            this.sessionId = sessionId;
        }

        public ChatRequest(String query, String sessionId, boolean includeDetails) {
            this.query = query;
            this.sessionId = sessionId;
            this.includeDetails = includeDetails;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public List<String> getConversationHistory() {
            return conversationHistory;
        }

        public void setConversationHistory(List<String> conversationHistory) {
            this.conversationHistory = conversationHistory;
        }

        public boolean isIncludeDetails() {
            return includeDetails;
        }

        public void setIncludeDetails(boolean includeDetails) {
            this.includeDetails = includeDetails;
        }

        @Override
        public String toString() {
            return "ChatRequest{" +
                    "query='" + query + '\'' +
                    ", sessionId='" + sessionId + '\'' +
                    ", conversationHistory=" + (conversationHistory != null ? conversationHistory.size() : 0) +
                    ", includeDetails=" + includeDetails +
                    '}';
        }
    }
}
