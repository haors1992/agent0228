package com.agent.config;

import com.agent.service.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 会话管理初始化配置
 */
@Slf4j
@Configuration
public class SessionConfig {

    /**
     * 应用启动时初始化会话管理器
     */
    @Bean
    public ApplicationRunner sessionInitializer(SessionManager sessionManager) {
        return args -> {
            log.info("📝 Initializing Session Manager...");
            sessionManager.init();
            log.info("✅ Session Manager initialized successfully");
        };
    }
}
