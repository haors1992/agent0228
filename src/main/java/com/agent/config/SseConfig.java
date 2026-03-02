package com.agent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SSE (Server-Sent Events) 异步配置
 * 支持流式响应的超时时间设置
 */
@Configuration
public class SseConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 设置异步请求的超时时间为 5 分钟
        configurer.setDefaultTimeout(300000L);
        // 设置线程池大小
        configurer.setTaskExecutor(null); // 使用默认线程池
    }
}
