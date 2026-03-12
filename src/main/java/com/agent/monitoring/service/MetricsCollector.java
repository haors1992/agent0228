package com.agent.monitoring.service;

import com.agent.monitoring.model.Metrics;
import com.sun.management.OperatingSystemMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 指标收集器
 * 负责收集系统、API、推理引擎等各类指标
 */
@Slf4j
@Service
public class MetricsCollector {

    private final MetricsStorageService storageService;

    // 计数器
    private final AtomicInteger apiCallCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);
    private final ConcurrentHashMap<String, Integer> endpointStats = new ConcurrentHashMap<>();

    // 连接计数
    private final AtomicInteger activeSseConnections = new AtomicInteger(0);

    // 性能指标（使用 LinkedList 保存最近 1000 条）
    private final LinkedList<Long> responseTimesApi = new LinkedList<>();
    private final LinkedList<Long> responseTimesReasoning = new LinkedList<>();
    private static final int MAX_HISTORY = 1000;

    public MetricsCollector(MetricsStorageService storageService) {
        this.storageService = storageService;
        startSystemMetricsThread();
    }

    /**
     * 记录 API 调用指标
     */
    public void recordApiMetrics(String endpoint, String method, Integer statusCode,
            Long responseTime, Integer requestSize, Integer responseSize) {
        Metrics metrics = Metrics.builder()
                .metricsType("API")
                .apiEndpoint(endpoint)
                .httpMethod(method)
                .statusCode(statusCode)
                .responseTime(responseTime)
                .requestSize(requestSize)
                .responseSize(responseSize)
                .success(statusCode >= 200 && statusCode < 300)
                .build();

        // 更新计数
        apiCallCount.incrementAndGet();
        if (metrics.getSuccess()) {
            successCount.incrementAndGet();
        } else {
            errorCount.incrementAndGet();
        }

        // 记录端点统计
        endpointStats.merge(endpoint, 1, Integer::sum);

        // 记录响应时间
        synchronized (responseTimesApi) {
            responseTimesApi.addLast(responseTime);
            if (responseTimesApi.size() > MAX_HISTORY) {
                responseTimesApi.removeFirst();
            }
        }

        storageService.recordMetrics(metrics);
        log.trace("📊 API 指标已记录: {} - {} ms", endpoint, responseTime);
    }

    /**
     * 记录推理引擎指标
     */
    public void recordReasoningMetrics(String sessionId, String query, Integer iterations,
            Integer thinkingSteps, Long responseTime, Boolean success,
            String errorMessage) {
        Metrics metrics = Metrics.builder()
                .metricsType("REASONING")
                .sessionId(sessionId)
                .query(query)
                .iterations(iterations)
                .thinking(thinkingSteps)
                .responseTime(responseTime)
                .success(success)
                .errorMessage(errorMessage)
                .build();

        // 记录响应时间
        synchronized (responseTimesReasoning) {
            responseTimesReasoning.addLast(responseTime);
            if (responseTimesReasoning.size() > MAX_HISTORY) {
                responseTimesReasoning.removeFirst();
            }
        }

        storageService.recordMetrics(metrics);
        log.trace("🧠 推理指标已记录: 迭代 {} 次，耗时 {} ms", iterations, responseTime);
    }

    /**
     * 记录知识库指标
     */
    public void recordKnowledgeBaseMetrics(String sessionId, String query,
            Integer hits, Double avgSimilarity, Integer topK) {
        Metrics metrics = Metrics.builder()
                .metricsType("KNOWLEDGE_BASE")
                .sessionId(sessionId)
                .query(query)
                .knowledgeBaseHits(hits)
                .avgSimilarity(avgSimilarity)
                .topKResults(topK)
                .success(hits > 0)
                .build();

        storageService.recordMetrics(metrics);
        log.debug("📚 知识库指标已记录: 匹配 {} 个结果，相似度 {:.2f}", hits, avgSimilarity);
    }

    /**
     * 记录 SSE 连接指标
     */
    public void recordSseMetrics(Integer connections, Integer events, Long duration) {
        Metrics metrics = Metrics.builder()
                .metricsType("SSE")
                .sseConnections(connections)
                .sseEvents(events)
                .sseConnectionDuration(duration)
                .success(true)
                .build();

        storageService.recordMetrics(metrics);
        log.debug("📡 SSE 指标已记录: {} 个连接，{} 个事件，耗时 {} ms", connections, events, duration);
    }

    /**
     * SSE 连接加 1
     */
    public void incSseConnections() {
        activeSseConnections.incrementAndGet();
    }

    /**
     * SSE 连接减 1
     */
    public void decSseConnections() {
        activeSseConnections.decrementAndGet();
    }

    /**
     * 获取活跃 SSE 连接数
     */
    public int getActiveSseConnections() {
        return activeSseConnections.get();
    }

    /**
     * 获取系统资源指标
     */
    public Map<String, Object> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            // CPU 和内存
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

            // CPU 使用率 (%) - Java 8 兼容方式：使用系统 CPU 使用率
            double cpuLoad = osBean.getSystemCpuLoad();
            double cpuUsage = cpuLoad < 0 ? 0 : cpuLoad * 100;
            metrics.put("cpuUsage", String.format("%.2f%%", cpuUsage));

            // 内存使用率
            long heapUsed = memBean.getHeapMemoryUsage().getUsed();
            long heapMax = memBean.getHeapMemoryUsage().getMax();
            double memoryUsage = (heapUsed * 100.0) / heapMax;
            metrics.put("memoryUsage", String.format("%.2f%%", memoryUsage));
            metrics.put("heapUsedMB", heapUsed / 1024 / 1024);
            metrics.put("heapMaxMB", heapMax / 1024 / 1024);

            // 线程数
            metrics.put("threadCount", threadBean.getThreadCount());
            metrics.put("peakThreadCount", threadBean.getPeakThreadCount());

        } catch (Exception e) {
            log.error("❌ 获取系统指标失败: {}", e.getMessage());
        }

        return metrics;
    }

    /**
     * 获取性能统计
     */
    public Map<String, Object> getPerformanceStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalApiCalls", apiCallCount.get());
        stats.put("successCount", successCount.get());
        stats.put("errorCount", errorCount.get());

        if (apiCallCount.get() > 0) {
            double successRate = (successCount.get() * 100.0) / apiCallCount.get();
            stats.put("successRate", String.format("%.2f%%", successRate));
        }

        // 端点统计
        stats.put("topEndpoints", endpointStats.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .toArray());

        // 响应时间统计
        synchronized (responseTimesApi) {
            if (!responseTimesApi.isEmpty()) {
                OptionalDouble avgApi = responseTimesApi.stream()
                        .mapToLong(Long::longValue)
                        .average();
                stats.put("avgApiResponseTime",
                        avgApi.isPresent() ? String.format("%.2f ms", avgApi.getAsDouble()) : "N/A");
            }
        }

        synchronized (responseTimesReasoning) {
            if (!responseTimesReasoning.isEmpty()) {
                OptionalDouble avgReasoning = responseTimesReasoning.stream()
                        .mapToLong(Long::longValue)
                        .average();
                stats.put("avgReasoningTime",
                        avgReasoning.isPresent() ? String.format("%.2f ms", avgReasoning.getAsDouble()) : "N/A");
            }
        }

        // SSE 连接
        stats.put("activeSseConnections", activeSseConnections.get());

        return stats;
    }

    /**
     * 启动系统指标收集线程
     */
    private void startSystemMetricsThread() {
        Thread metricsThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // 每分钟收集一次

                    Map<String, Object> systemMetrics = getSystemMetrics();

                    Metrics metrics = Metrics.builder()
                            .metricsType("SYSTEM")
                            .cpuUsage(Double.parseDouble(
                                    systemMetrics.get("cpuUsage").toString().replace("%", "")))
                            .memoryUsage(Double.parseDouble(
                                    systemMetrics.get("memoryUsage").toString().replace("%", "")))
                            .threadCount((Integer) systemMetrics.get("threadCount"))
                            .success(true)
                            .build();

                    storageService.recordMetrics(metrics);

                } catch (InterruptedException e) {
                    log.debug("系统指标收集线程已停止");
                    break;
                } catch (Exception e) {
                    log.error("❌ 系统指标收集异常: {}", e.getMessage());
                }
            }
        }, "MetricsCollector-SystemMetrics");

        metricsThread.setDaemon(true);
        metricsThread.start();
    }

    /**
     * 清空所有计数器（用于测试）
     */
    public void resetCounters() {
        apiCallCount.set(0);
        successCount.set(0);
        errorCount.set(0);
        endpointStats.clear();
        log.info("🔄 计数器已重置");
    }
}
