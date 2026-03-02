package com.agent.monitoring.interceptor;

import com.agent.monitoring.service.MetricsCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * API 监控拦截器
 * 自动收集所有 API 请求的指标
 */
@Slf4j
@Component
public class MetricsInterceptor implements HandlerInterceptor {

    @Autowired(required = false)
    private MetricsCollector metricsCollector;

    private static final String START_TIME = "metrics_start_time";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 记录请求开始时间
        request.setAttribute(START_TIME, System.currentTimeMillis());

        if (metricsCollector != null) {
            // 如果是 SSE 请求，增加连接计数
            if (request.getRequestURI().contains("/chat/stream")) {
                metricsCollector.incSseConnections();
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) {
        if (metricsCollector == null)
            return;

        try {
            Long startTime = (Long) request.getAttribute(START_TIME);
            if (startTime == null)
                return;

            String endpoint = request.getRequestURI();
            String method = request.getMethod();
            Integer statusCode = response.getStatus();
            Long responseTime = System.currentTimeMillis() - startTime;

            // 获取请求和响应大小 (近似)
            int requestSize = estimate(request.getContentLength());
            int responseSize = response.getBufferSize();

            // 排除某些不需要监控的端点
            if (!endpoint.startsWith("/api/monitoring") &&
                    !endpoint.startsWith("/health")) {
                metricsCollector.recordApiMetrics(endpoint, method, statusCode,
                        responseTime, requestSize, responseSize);
            }

            // SSE 连接结束
            if (endpoint.contains("/chat/stream")) {
                metricsCollector.decSseConnections();
            }

        } catch (Exception e) {
            log.error("❌ 指标记录失败: {}", e.getMessage());
        }
    }

    private int estimate(int contentLength) {
        return contentLength >= 0 ? contentLength : 0;
    }
}
