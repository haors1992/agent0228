package com.agent.test;

import com.agent.reasoning.engine.ExecutionContext;
import com.agent.reasoning.engine.ReasoningEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration Test for Agent System
 * 
 * Quick verification that all layers work together
 */
@Slf4j
@SpringBootTest
@TestPropertySource(properties = {
    "agent.max-iterations=5",
    "agent.timeout=60"
})
public class AgentIntegrationTest {
    
    @Autowired
    private ReasoningEngine reasoningEngine;
    
    /**
     * Test 1: Simple Calculator Query
     */
    // @Test
    public void testCalculatorQuery() {
        log.info("=== Test 1: Calculator Query ===");
        
        String query = "计算 100 + 200 的结果";
        ExecutionContext context = reasoningEngine.execute(query);
        
        log.info("Query: {}", query);
        log.info("Result: {}", context.getFinalAnswer());
        log.info("Iterations: {}", context.getCurrentIteration());
        log.info("Duration: {}ms", context.getExecutionTimeMs());
        log.info("Complete: {}", context.getIsComplete());
        
        assert context.getIsComplete() : "Execution should complete";
        assert context.getFinalAnswer() != null : "Should have final answer";
        log.info("✅ Test 1 passed\n");
    }
    
    /**
     * Test 2: String Operation
     */
    // @Test
    public void testStringOperation() {
        log.info("=== Test 2: String Operation ===");
        
        String query = "将 'hello world' 转换为大写";
        ExecutionContext context = reasoningEngine.execute(query);
        
        log.info("Query: {}", query);
        log.info("Result: {}", context.getFinalAnswer());
        log.info("Iterations: {}", context.getCurrentIteration());
        
        assert context.getIsComplete() : "Execution should complete";
        log.info("✅ Test 2 passed\n");
    }
    
    /**
     * Test 3: Complex Multi-Step Task
     */
    // @Test
    public void testComplexTask() {
        log.info("=== Test 3: Complex Multi-Step Task ===");
        
        String query = "计算 50 * 3，然后把结果转换成大写英文";
        ExecutionContext context = reasoningEngine.execute(query);
        
        log.info("Query: {}", query);
        log.info("Result: {}", context.getFinalAnswer());
        log.info("Iterations: {}", context.getCurrentIteration());
        log.info("Tool Results: {}", context.getToolResults().size());
        
        assert context.getIsComplete() : "Execution should complete";
        assert context.getToolResults().size() > 0 : "Should have tool results";
        log.info("✅ Test 3 passed\n");
    }
    
    /**
     * Manual test runner
     */
    public static void main(String[] args) {
        log.info("Agent Integration Test Suite");
        log.info("Note: Tests are disabled by default. Uncomment @Test to run.");
    }
}
