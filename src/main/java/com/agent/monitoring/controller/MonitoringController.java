package com.agent.monitoring.controller;

import com.agent.monitoring.service.MetricsCollector;
import com.agent.monitoring.service.MetricsStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 监控和指标收集 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    @Autowired
    private MetricsCollector metricsCollector;

    @Autowired
    private MetricsStorageService storageService;

    /**
     * 获取监控摘要
     * GET /api/monitoring/summary
     */
    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        log.info("📊 获取监控摘要");

        Map<String, Object> summary = new HashMap<>();

        // 系统指标
        summary.put("system", metricsCollector.getSystemMetrics());

        // 性能统计
        summary.put("performance", metricsCollector.getPerformanceStats());

        // 存储统计
        summary.put("storage", storageService.getSummary());

        // 时间戳
        summary.put("timestamp", System.currentTimeMillis());

        return summary;
    }

    /**
     * 获取实时指标（最近 N 条）
     * GET /api/monitoring/realtime?type=API&limit=100
     */
    @GetMapping("/realtime")
    public Map<String, Object> getRealtimeMetrics(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String type) {

        log.info("📈 获取实时指标，limit={}, type={}", limit, type);

        Map<String, Object> response = new HashMap<>();

        if (type != null) {
            response.put("metrics", storageService.getMetricsByType(type, limit));
            response.put("type", type);
        } else {
            response.put("metrics", storageService.getRecentMetrics(limit));
        }

        response.put("count", response.get("metrics"));
        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    /**
     * 获取历史数据（按日期）
     * GET /api/monitoring/history?days=7
     */
    @GetMapping("/history")
    public Map<String, Object> getHistoryMetrics(
            @RequestParam(defaultValue = "7") int days) {

        log.info("📅 获取历史指标，days={}", days);

        Map<String, Object> response = new HashMap<>();
        response.put("dailyStats", storageService.getDailyStats(days));
        response.put("days", days);
        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    /**
     * 获取聚合统计
     * GET /api/monitoring/aggregate?type=API
     */
    @GetMapping("/aggregate")
    public Map<String, Object> getAggregateStats(
            @RequestParam(required = false) String type) {

        log.info("📊 获取聚合统计，type={}", type);

        Map<String, Object> response = new HashMap<>();
        response.put("stats", storageService.getAggregateStats(type));
        response.put("type", type != null ? type : "ALL");
        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    /**
     * 获取系统资源
     * GET /api/monitoring/system
     */
    @GetMapping("/system")
    public Map<String, Object> getSystemMetrics() {
        log.info("💻 获取系统资源");

        Map<String, Object> response = new HashMap<>();
        response.put("metrics", metricsCollector.getSystemMetrics());
        response.put("timestamp", System.currentTimeMillis());
        response.put("activeSseConnections", metricsCollector.getActiveSseConnections());

        return response;
    }

    /**
     * 获取性能统计
     * GET /api/monitoring/performance
     */
    @GetMapping("/performance")
    public Map<String, Object> getPerformance() {
        log.info("⚡ 获取性能统计");

        Map<String, Object> response = new HashMap<>();
        response.put("stats", metricsCollector.getPerformanceStats());
        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    /**
     * 清空指标数据
     * DELETE /api/monitoring/metrics
     */
    @DeleteMapping("/metrics")
    public Map<String, String> clearMetrics() {
        log.warn("🗑️  清空指标数据");
        storageService.clearMetrics();
        metricsCollector.resetCounters();

        Map<String, String> response = new HashMap<>();
        response.put("message", "✅ 指标数据已清空");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));

        return response;
    }

    /**
     * 获取监控面板数据
     * GET /api/monitoring/dashboard
     */
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardData() {
        log.info("📊 获取监控面板数据");

        Map<String, Object> dashboard = new HashMap<>();

        // 1. 实时状态
        Map<String, Object> status = new HashMap<>();
        status.put("system", metricsCollector.getSystemMetrics());
        status.put("activeSseConnections", metricsCollector.getActiveSseConnections());
        dashboard.put("status", status);

        // 2. 性能指标
        dashboard.put("performance", metricsCollector.getPerformanceStats());

        // 3. 最近指标
        dashboard.put("recent", storageService.getRecentMetrics(50));

        // 4. 聚合统计
        Map<String, Object> aggregate = new HashMap<>();
        aggregate.put("api", storageService.getAggregateStats("API"));
        aggregate.put("reasoning", storageService.getAggregateStats("REASONING"));
        aggregate.put("knowledgeBase", storageService.getAggregateStats("KNOWLEDGE_BASE"));
        dashboard.put("aggregate", aggregate);

        // 5. 历史数据
        dashboard.put("history", storageService.getDailyStats(7));

        // 6. 时间戳
        dashboard.put("timestamp", System.currentTimeMillis());

        return dashboard;
    }

    /**
     * 获取健康状态
     * GET /api/monitoring/health
     */
    @GetMapping("/health")
    public Map<String, Object> getHealth() {
        Map<String, Object> health = new HashMap<>();

        Map<String, Object> systemMetrics = metricsCollector.getSystemMetrics();
        double cpuUsage = Double.parseDouble(
                systemMetrics.get("cpuUsage").toString().replace("%", ""));
        double memoryUsage = Double.parseDouble(
                systemMetrics.get("memoryUsage").toString().replace("%", ""));

        // 判断状态
        String status = "HEALTHY";
        if (cpuUsage > 90 || memoryUsage > 90) {
            status = "CRITICAL";
        } else if (cpuUsage > 75 || memoryUsage > 75) {
            status = "WARNING";
        }

        health.put("status", status);
        health.put("cpu", cpuUsage);
        health.put("memory", memoryUsage);
        health.put("threads", systemMetrics.get("threadCount"));
        health.put("sseConnections", metricsCollector.getActiveSseConnections());
        health.put("timestamp", System.currentTimeMillis());

        return health;
    }
}
