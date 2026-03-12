package com.agent.monitoring.service;

import com.agent.monitoring.model.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 指标存储和检索服务
 */
@Slf4j
@Service
public class MetricsStorageService {

    private final Path metricsDir = Paths.get("data/metrics");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    // 内存缓冲：存储最近的指标 (最多 10000 条)
    private final LinkedList<Metrics> recentMetrics = new LinkedList<>();
    private static final int MAX_MEMORY_METRICS = 10000;

    public MetricsStorageService() {
        try {
            Files.createDirectories(metricsDir);
            log.info("✅ 指标存储目录已初始化: {}", metricsDir);
        } catch (IOException e) {
            log.error("❌ 创建指标目录失败: {}", e.getMessage());
        }
    }

    /**
     * 记录指标
     */
    public void recordMetrics(Metrics metrics) {
        if (metrics == null)
            return;

        metrics.setMetricsId(UUID.randomUUID().toString());
        metrics.setTimestamp(System.currentTimeMillis());

        try {
            // 1. 添加到内存缓冲
            addToMemoryBuffer(metrics);

            // 2. 持久化到文件
            persistMetrics(metrics);

            // 3. 记录关键日志
            logMetrics(metrics);

        } catch (Exception e) {
            log.error("❌ 记录指标失败: {}", e.getMessage());
        }
    }

    /**
     * 添加到内存缓冲
     */
    private synchronized void addToMemoryBuffer(Metrics metrics) {
        recentMetrics.addLast(metrics);

        // 防止内存溢出
        if (recentMetrics.size() > MAX_MEMORY_METRICS) {
            recentMetrics.removeFirst();
        }
    }

    /**
     * 持久化指标到文件
     */
    private void persistMetrics(Metrics metrics) throws IOException {
        // 从时间戳转换为日期格式
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(metrics.getTimestamp()),
                ZoneId.systemDefault());
        String dateStr = dateTime.format(dateFormatter);
        Path dayFile = metricsDir.resolve(dateStr + ".jsonl");

        // 使用 JSONL 格式 (每行一个 JSON)
        String jsonLine = parseMetricsToJson(metrics) + "\n";

        Files.write(dayFile, jsonLine.getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);

        log.trace("💾 指标已保存: {}", dateStr);
    }

    /**
     * 指标转 JSON
     */
    private String parseMetricsToJson(Metrics m) {
        return String.format(
                "{\"id\":\"%s\",\"type\":\"%s\",\"time\":\"%s\",\"endpoint\":\"%s\",\"status\":%d,\"responseTime\":%d,\"success\":%s,\"iterations\":%d}",
                m.getMetricsId(),
                m.getMetricsType() != null ? m.getMetricsType() : "UNKNOWN",
                m.getTimestamp(),
                m.getApiEndpoint() != null ? m.getApiEndpoint() : "",
                m.getStatusCode() != null ? m.getStatusCode() : 0,
                m.getResponseTime() != null ? m.getResponseTime() : 0,
                m.getSuccess() != null ? m.getSuccess() : false,
                m.getIterations() != null ? m.getIterations() : 0);
    }

    /**
     * 记录关键指标日志
     */
    private void logMetrics(Metrics metrics) {
        if ("API".equals(metrics.getMetricsType())) {
            Integer statusCode = metrics.getStatusCode();
            Long responseTime = metrics.getResponseTime();
            boolean isError = statusCode != null && statusCode >= 400;
            boolean isSlow = responseTime != null && responseTime >= 2000L;

            if (isError || isSlow) {
                log.info("📊 API(关键): {} {} - {} ms - {}",
                        metrics.getHttpMethod(),
                        metrics.getApiEndpoint(),
                        metrics.getResponseTime(),
                        metrics.getStatusCode());
            } else {
                log.trace("📊 API: {} {} - {} ms - {}",
                        metrics.getHttpMethod(),
                        metrics.getApiEndpoint(),
                        metrics.getResponseTime(),
                        metrics.getStatusCode());
            }
        } else if ("REASONING".equals(metrics.getMetricsType())) {
            log.debug("🧠 REASONING: {} - {} ms - {} 次迭代",
                    metrics.getQuery() != null
                            ? metrics.getQuery().substring(0, Math.min(20, metrics.getQuery().length()))
                            : "",
                    metrics.getResponseTime(),
                    metrics.getIterations());
        } else if ("KNOWLEDGE_BASE".equals(metrics.getMetricsType())) {
            log.debug("📚 KB: 命中 {} 个结果，平均相似度 {:.2f}",
                    metrics.getKnowledgeBaseHits(),
                    metrics.getAvgSimilarity() != null ? metrics.getAvgSimilarity() : 0);
        }
    }

    /**
     * 获取最近的指标（内存）
     */
    public List<Metrics> getRecentMetrics(int limit) {
        synchronized (recentMetrics) {
            return recentMetrics.stream()
                    .skip(Math.max(0, recentMetrics.size() - limit))
                    .collect(Collectors.toList());
        }
    }

    /**
     * 按类型过滤指标
     */
    public List<Metrics> getMetricsByType(String type, int limit) {
        synchronized (recentMetrics) {
            return recentMetrics.stream()
                    .filter(m -> type == null || type.equals(m.getMetricsType()))
                    .skip(Math.max(0, recentMetrics.size() - limit))
                    .collect(Collectors.toList());
        }
    }

    /**
     * 获取聚合统计
     */
    public Map<String, Object> getAggregateStats(String metricsType) {
        synchronized (recentMetrics) {
            List<Metrics> filtered = recentMetrics.stream()
                    .filter(m -> metricsType == null || metricsType.equals(m.getMetricsType()))
                    .collect(Collectors.toList());

            Map<String, Object> stats = new HashMap<>();

            if (filtered.isEmpty()) {
                stats.put("count", 0);
                return stats;
            }

            // 统计数量
            stats.put("count", filtered.size());

            // 响应时间统计
            List<Long> responseTimes = filtered.stream()
                    .filter(m -> m.getResponseTime() != null)
                    .map(Metrics::getResponseTime)
                    .sorted()
                    .collect(Collectors.toList());

            if (!responseTimes.isEmpty()) {
                stats.put("avgResponseTime", responseTimes.stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0));
                stats.put("minResponseTime", responseTimes.get(0));
                stats.put("maxResponseTime", responseTimes.get(responseTimes.size() - 1));
                stats.put("p95ResponseTime", responseTimes.get((int) (responseTimes.size() * 0.95)));
            }

            // 成功率
            long successCount = filtered.stream()
                    .filter(m -> m.getSuccess() != null && m.getSuccess())
                    .count();
            stats.put("successRate", String.format("%.2f%%", (successCount * 100.0 / filtered.size())));

            // 迭代统计
            OptionalDouble avgIterations = filtered.stream()
                    .filter(m -> m.getIterations() != null)
                    .mapToInt(Metrics::getIterations)
                    .average();
            if (avgIterations.isPresent()) {
                stats.put("avgIterations", String.format("%.2f", avgIterations.getAsDouble()));
            }

            return stats;
        }
    }

    /**
     * 获取历史数据（按日期）
     */
    public Map<String, Integer> getDailyStats(int days) {
        Map<String, Integer> dailyStats = new LinkedHashMap<>();

        try {
            for (int i = days - 1; i >= 0; i--) {
                LocalDateTime date = LocalDateTime.now().minusDays(i);
                String dateStr = date.format(dateFormatter);
                Path dayFile = metricsDir.resolve(dateStr + ".jsonl");

                int count = 0;
                if (Files.exists(dayFile)) {
                    count = (int) Files.lines(dayFile).count();
                }

                dailyStats.put(dateStr, count);
            }
        } catch (IOException e) {
            log.error("❌ 读取历史数据失败: {}", e.getMessage());
        }

        return dailyStats;
    }

    /**
     * 清空指标
     */
    public void clearMetrics() {
        synchronized (recentMetrics) {
            recentMetrics.clear();
        }
        log.info("🗑️  指标已清空");
    }

    /**
     * 获取统计摘要
     */
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();

        // 总计数
        summary.put("totalMetrics", recentMetrics.size());

        // 按类型统计
        Map<String, Long> typeCount = recentMetrics.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getMetricsType() != null ? m.getMetricsType() : "UNKNOWN",
                        Collectors.counting()));
        summary.put("byType", typeCount);

        // API 指标
        summary.put("api", getAggregateStats("API"));

        // 推理指标
        summary.put("reasoning", getAggregateStats("REASONING"));

        // 知识库指标
        summary.put("knowledgeBase", getAggregateStats("KNOWLEDGE_BASE"));

        return summary;
    }
}
