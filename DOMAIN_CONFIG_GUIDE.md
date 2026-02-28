/**
 * 使用示例：如何为特殊领域配置 SystemPromptBuilder
 * 
 * 本文件展示了4种不同的使用方式
 */

// ============ 方式 1：自动领域检测（推荐，最简单）============
// 原理：根据用户查询内容自动检测属于哪个领域
// 完全自动化，无需手工干预

String medicalQuery = "我最近一直头痛，应该吃什么药？";
// 自动检测到医疗领域，应用 MedicalPrompt
String systemPrompt = systemPromptBuilder.buildSystemPromptWithDomainDetection(medicalQuery);

String programmingQuery = "如何在 Java 中实现单例模式？";
// 自动检测到编程领域，应用 ProgrammingPrompt
String systemPrompt = systemPromptBuilder.buildSystemPromptWithDomainDetection(programmingQuery);


// ============ 方式 2：显式指定领域（手动，最灵活）============
// 原理：直接传递特定领域的系统提示
// 适合已经知道用户问题属于哪个领域的场景

String domainPrompt = domainPromptConfig.getMedicalPrompt();
String systemPrompt = systemPromptBuilder.buildSystemPrompt(domainPrompt);

// 或者：
String domainPrompt = domainPromptConfig.getProgrammingPrompt();
String systemPrompt = systemPromptBuilder.buildSystemPrompt(domainPrompt);


// ============ 方式 3：修改 ReasoningEngine 使用自动检测 ============
// 编辑 src/main/java/com/agent/reasoning/engine/ReasoningEngine.java

public ChatResponse reason(String query) {
    log.info("Starting agent reasoning for query: {}", query);
    
    ExecutionContext context = new ExecutionContext(query);
    // 使用自动领域检测而不是通用提示
    String systemPrompt = systemPromptBuilder.buildSystemPromptWithDomainDetection(query);
    
    // ... 其余代码保持不变
}


// ============ 方式 4：创建自定义领域（针对你的特殊需求）============

// Step 1: 在 DomainPromptConfig 中添加新方法
public String getYourCustomDomainPrompt() {
    return """
        === YOUR CUSTOM DOMAIN ===
        You are a specialized assistant for your domain with:
        - 深度专业知识
        - 特定的操作规程
        - 自定义的约束条件
        
        CUSTOM CONSTRAINTS:
        - 你的具体要求 1
        - 你的具体要求 2
        - 你的具体要求 3
        """;
}

// Step 2: 在 detectAndGetDomainPrompt() 中添加关键词检测
if (containsKeyword(lowerQuery, "你的关键词1", "你的关键词2")) {
    return getYourCustomDomainPrompt();
}

// Step 3: 使用自动检测
String result = systemPromptBuilder.buildSystemPromptWithDomainDetection(userQuery);


// ============ 实际应用场景示例 ============

// 案例 1: 医疗助手聊天应用
public class MedicalChatService {
    public String chat(String userMessage) {
        // 自动在医疗领域模式下运行
        String systemPrompt = systemPromptBuilder.buildSystemPromptWithDomainDetection(userMessage);
        return reasoningEngine.reason(userMessage); // 内部使用医疗提示
    }
}

// 案例 2: 编程学习平台
public class CodeTutorService {
    public String explainCode(String codeQuestion) {
        // 自动在编程领域模式下运行
        String systemPrompt = systemPromptBuilder.buildSystemPromptWithDomainDetection(codeQuestion);
        return reasoningEngine.reason(codeQuestion);
    }
}

// 案例 3: 多领域统一接口
public class UniversalAssistant {
    public String answer(String query) {
        // 自动检测并应用合适的领域提示
        // 医疗、法律、编程、财务等问题都能自动处理
        String systemPrompt = systemPromptBuilder.buildSystemPromptWithDomainDetection(query);
        return reasoningEngine.reason(query);
    }
}


// ============ 配置 REST API 端点来选择领域 ============

@PostMapping("/api/agent/chat-medical")
public ChatResponse chatMedical(@RequestBody ChatRequest request) {
    // 显式使用医疗领域提示
    String medicalPrompt = domainPromptConfig.getMedicalPrompt();
    return reasoningEngine.reasonWithCustomPrompt(request.getQuery(), medicalPrompt);
}

@PostMapping("/api/agent/chat-programming")
public ChatResponse chatProgramming(@RequestBody ChatRequest request) {
    // 显式使用编程领域提示
    String programmingPrompt = domainPromptConfig.getProgrammingPrompt();
    return reasoningEngine.reasonWithCustomPrompt(request.getQuery(), programmingPrompt);
}

@PostMapping("/api/agent/chat-auto")
public ChatResponse chatAuto(@RequestBody ChatRequest request) {
    // 自动检测领域
    return reasoningEngine.reason(request.getQuery());
}


// ============ 配置选择流程 ============

选择 AUTO（自动）:
  用户问题 → 自动检测关键词 → 确定领域 → 应用对应提示 → 得到专业回答
  优点：用户无需关心领域，系统自动处理
  缺点：边界情况可能检测不准

选择 MANUAL（手动）:
  前端显示"医疗"、"编程"等按钮 → 用户选择 → 应用特定提示 → 得到专业回答
  优点：精准控制，用户体验好
  缺点：需要用户做一步选择


// ============ 扩展建议 ============

1. 添加更多领域提示
   - 房地产投资
   - 家装设计
   - 美食烹饪
   - 旅游规划
   - 等等...

2. 为每个领域添加专门的工具
   - 医疗领域：症状检查工具、药物查询工具
   - 编程领域：代码检查工具、性能分析工具
   - 财务领域：利息计算工具、投资回本计算器
   - 等等...

3. 持久化领域配置
   - 保存用户选择的领域偏好
   - 记录用户的交互历史
   - 根据历史自动选择合适的领域

4. 动态提示更新
   - 根据用户反馈调整提示
   - A/B 测试不同的提示效果
   - 持续优化专业性
*/
