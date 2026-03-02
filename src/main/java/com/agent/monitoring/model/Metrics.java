package com.agent.monitoring.model;

import lombok.*;
import java.io.Serializable;

/**
 * 指标数据模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Metrics implements Serializable {

    private static final long serialVersionUID = 1L;

    // 基本信息
    private String metricsId; // 指标 ID
    private String metricsType; // 指标类型 (API, REASONING, KNOWLEDGE_BASE, SSE, SYSTEM)
    private Long timestamp; // 时间戳（毫秒）

    // API 指标
    private String apiEndpoint; // API 端点
    private String httpMethod; // HTTP 方法
    private Integer statusCode; // 响应状态码
    private Long responseTime; // 响应时间 (ms)
    private Integer requestSize; // 请求大小 (字节)
    private Integer responseSize; // 响应大小 (字节)

    // 业务指标
    private String sessionId; // 会话 ID
    private String query; // 查询内容
    private Integer iterations; // 推理迭代次数
    private Integer thinking; // 思维链步骤数
    private Boolean success; // 是否成功
    private String errorMessage; // 错误信息

    // 知识库指标
    private Integer knowledgeBaseHits; // 知识库匹配数
    private Double avgSimilarity; // 平均相似度
    private Integer topKResults; // 返回结果数

    // SSE 指标
    private Integer sseConnections; // 活跃 SSE 连接数
    private Integer sseEvents; // 发送的事件数
    private Long sseConnectionDuration; // 连接持续时间 (ms)

    // 系统资源
    private Double cpuUsage; // CPU 使用率 (%)
    private Double memoryUsage; // 内存使用率 (%)
    private Long totalMemory; // 总内存 (MB)
    private Long freeMemory; // 可用内存 (MB)
    private Integer threadCount; // 线程数

    // 自定义标签
    private String source; // 数据来源
    private String userId; // 用户 ID
    private String version; // 版本号

    @Override
    public String toString() {
        return String.format("Metrics{type=%s, endpoint=%s, time=%d ms}",
                metricsType, apiEndpoint, responseTime);
    }
}
