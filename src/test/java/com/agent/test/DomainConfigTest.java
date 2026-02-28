package com.agent.test;

import com.agent.config.DomainPromptConfig;
import com.agent.reasoning.prompt.SystemPromptBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 领域配置测试
 * 
 * 测试 DomainPromptConfig 和 SystemPromptBuilder 的功能
 */
@SpringBootTest
public class DomainConfigTest {
    
    @Autowired
    private DomainPromptConfig domainPromptConfig;
    
    @Autowired
    private SystemPromptBuilder systemPromptBuilder;
    
    /**
     * 测试 1：验证所有领域提示都能正确加载
     */
    @Test
    public void testAllDomainPromptsLoaded() {
        assertNotNull(domainPromptConfig.getMedicalPrompt(), "医疗提示不应为空");
        assertNotNull(domainPromptConfig.getLegalPrompt(), "法律提示不应为空");
        assertNotNull(domainPromptConfig.getProgrammingPrompt(), "编程提示不应为空");
        assertNotNull(domainPromptConfig.getFinancePrompt(), "财务提示不应为空");
        assertNotNull(domainPromptConfig.getScienceEducationPrompt(), "科学提示不应为空");
        assertNotNull(domainPromptConfig.getContentCreationPrompt(), "创意提示不应为空");
        assertNotNull(domainPromptConfig.getDataAnalyticsPrompt(), "数据分析提示不应为空");
    }
    
    /**
     * 测试 2：验证领域提示包含正确的内容
     */
    @Test
    public void testDomainPromptContent() {
        String medicalPrompt = domainPromptConfig.getMedicalPrompt();
        assertTrue(medicalPrompt.contains("Medical"), "医疗提示应包含 'Medical' 关键词");
        assertTrue(medicalPrompt.contains("healthcare professionals"), "医疗提示应包含专业指导");
        
        String programmingPrompt = domainPromptConfig.getProgrammingPrompt();
        assertTrue(programmingPrompt.contains("Programming"), "编程提示应包含 'Programming' 关键词");
        assertTrue(programmingPrompt.contains("Java"), "编程提示应包含编程语言");
    }
    
    /**
     * 测试 3：测试自动领域检测 - 医疗领域
     */
    @Test
    public void testAutoDetectMedicalDomain() {
        String[] medicalKeywords = {
            "我最近一直头痛，应该吃什么药？",
            "症状分析：咳嗽、发烧",
            "这个药物有副作用吗？",
            "medical advice needed"
        };
        
        for (String query : medicalKeywords) {
            String detectedPrompt = domainPromptConfig.detectAndGetDomainPrompt(query);
            assertTrue(detectedPrompt.contains("Medical"), 
                "查询 '" + query + "' 应该被识别为医疗领域");
        }
    }
    
    /**
     * 测试 4：测试自动领域检测 - 编程领域
     */
    @Test
    public void testAutoDetectProgrammingDomain() {
        String[] programmingKeywords = {
            "如何在 Java 中实现单例模式？",
            "Python 异步编程怎么写？",
            "JavaScript 闭包是什么？",
            "code review 该注意什么"
        };
        
        for (String query : programmingKeywords) {
            String detectedPrompt = domainPromptConfig.detectAndGetDomainPrompt(query);
            assertTrue(detectedPrompt.contains("Programming"), 
                "查询 '" + query + "' 应该被识别为编程领域");
        }
    }
    
    /**
     * 测试 5：测试自动领域检测 - 法律领域
     */
    @Test
    public void testAutoDetectLegalDomain() {
        String[] legalKeywords = {
            "租赁合同需要注意什么？",
            "我的权利被侵犯了怎么办？",
            "legal requirement for startup",
            "合同条款解释"
        };
        
        for (String query : legalKeywords) {
            String detectedPrompt = domainPromptConfig.detectAndGetDomainPrompt(query);
            assertTrue(detectedPrompt.contains("Legal"), 
                "查询 '" + query + "' 应该被识别为法律领域");
        }
    }
    
    /**
     * 测试 6：测试自动领域检测 - 财务领域
     */
    @Test
    public void testAutoDetectFinanceDomain() {
        String[] financeKeywords = {
            "基金投资有风险吗？",
            "股票分析方法",
            "投资回报率怎么计算？",
            "financial planning advice"
        };
        
        for (String query : financeKeywords) {
            String detectedPrompt = domainPromptConfig.detectAndGetDomainPrompt(query);
            assertTrue(detectedPrompt.contains("Financial"), 
                "查询 '" + query + "' 应该被识别为财务领域");
        }
    }
    
    /**
     * 测试 7：测试自动领域检测 - 科学领域
     */
    @Test
    public void testAutoDetectScienceDomain() {
        String[] scienceKeywords = {
            "物理学的万有引力怎么理解？",
            "化学反应方程式是什么？",
            "生物细胞如何分裂？",
            "science education content"
        };
        
        for (String query : scienceKeywords) {
            String detectedPrompt = domainPromptConfig.detectAndGetDomainPrompt(query);
            assertTrue(detectedPrompt.contains("Science"), 
                "查询 '" + query + "' 应该被识别为科学领域");
        }
    }
    
    /**
     * 测试 8：SystemPromptBuilder 是否能正确集成领域提示
     */
    @Test
    public void testSystemPromptBuilderIntegration() {
        String customPrompt = domainPromptConfig.getMedicalPrompt();
        String fullPrompt = systemPromptBuilder.buildSystemPrompt(customPrompt);
        
        // 完整提示应该包含医疗领域信息
        assertTrue(fullPrompt.contains("Medical"), "完整提示应该包含医疗信息");
        
        // 应该包含工具说明
        assertTrue(fullPrompt.contains("AVAILABLE TOOLS"), "完整提示应该包含工具说明");
        
        // 应该包含响应格式说明
        assertTrue(fullPrompt.contains("Thought:"), "完整提示应该包含思考格式");
        assertTrue(fullPrompt.contains("Action:"), "完整提示应该包含行动格式");
    }
    
    /**
     * 测试 9：测试自动领域检测与 SystemPromptBuilder 的协作
     */
    @Test
    public void testAutoDetectionWithSystemPromptBuilder() {
        String medicalQuery = "我最近经常感到疲劳，应该检查什么？";
        
        // 自动检测领域
        String detectedPrompt = domainPromptConfig.detectAndGetDomainPrompt(medicalQuery);
        
        // 使用检测到的提示构建完整系统提示
        String fullPrompt = systemPromptBuilder.buildSystemPrompt(detectedPrompt);
        
        // 验证完整提示的质量
        assertTrue(fullPrompt.contains("Medical"), "自动检测应该识别为医疗领域");
        assertTrue(fullPrompt.length() > 500, "完整提示应该有足够的内容");
    }
    
    /**
     * 测试 10：通用提示作为默认值
     */
    @Test
    public void testDefaultGeneralPrompt() {
        String randomQuery = "xyzAbcDef";  // 无关键词的随机查询
        String detectedPrompt = domainPromptConfig.detectAndGetDomainPrompt(randomQuery);
        
        // 应该返回通用提示
        assertTrue(detectedPrompt.contains("General"), "应该返回通用提示作为默认值");
    }
    
    /**
     * 性能测试：检测速度
     */
    @Test
    public void testDetectionPerformance() {
        String[] queries = {
            "医疗问题",
            "编程问题",
            "法律问题",
            "财务问题"
        };
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 1000; i++) {
            for (String query : queries) {
                domainPromptConfig.detectAndGetDomainPrompt(query);
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("1000次 * 4个查询 = 4000次检测耗时: " + duration + "ms");
        assertTrue(duration < 5000, "检测应该足够快（4000次在5秒内）");
    }
}
