package com.agent.config;

import com.agent.knowledge.service.KnowledgeBaseManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 知识库配置
 * 初始化向量数据库和知识库
 */
@Slf4j
@Configuration
public class KnowledgeBaseConfig {

    @Bean
    public ApplicationRunner knowledgeBaseInitializer(KnowledgeBaseManager knowledgeBaseManager) {
        return args -> {
            log.info("📚 Initializing Knowledge Base...");
            knowledgeBaseManager.init();
            log.info("✅ Knowledge Base initialized successfully");
        };
    }
}
