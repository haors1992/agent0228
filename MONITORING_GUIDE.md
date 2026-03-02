# 📊 监控和指标收集系统

## 系统概述

完整的实时监控系统，自动收集和展示 AI Agent 系统的各种指标。

```
┌─────────────────────────────────────────────────┐
│     监控和指标收集系统                           │
├─────────────────────────────────────────────────┤
│ 🔍 自动收集                                     │
│  ├─ API 响应时间和状态                         │
│  ├─ 推理引擎性能                                │
│  ├─ 知识库检索结果                              │
│  ├─ SSE 连接统计                                │
│  └─ 系统资源使用                                │
│                                                 │
│ 💾 存储层                                       │
│  ├─ 内存缓冲（实时快速查询）                   │
│  ├─ 文件持久化（历史数据保留）                 │
│  └─ JSON 聚合统计                               │
│                                                 │
│ 🔌 REST API                                    │
│  ├─ /api/monitoring/summary - 摘要             │
│  ├─ /api/monitoring/realtime - 实时            │
│  ├─ /api/monitoring/history - 历史             │
│  └─ /api/monitoring/dashboard - 仪表板         │
│                                                 │
│ 📈 可视化仪表板                                │
│  ├─ 实时图表和指标                              │
│  ├─ 健康状态监测                                │
│  └─ 历史趋势分析                                │
└─────────────────────────────────────────────────┘
```

---

## 🚀 快速开始

### 1️⃣ 访问监控仪表板

打开浏览器访问：
```
http://localhost:8080/monitoring-dashboard.html
```

仪表板会自动：
- ✅ 每 5 秒刷新一次数据
- ✅ 显示实时系统资源（CPU、内存）
- ✅ 展示 API 性能指标
- ✅ 绘制趋势图表
- ✅ 监控健康状态

### 2️⃣ 查看实时指标

```bash
# 获取摘要信息
curl http://localhost:8080/api/monitoring/summary | jq .

# 获取最近 100 条指标
curl http://localhost:8080/api/monitoring/realtime?limit=100 | jq .

# 按类型过滤（API、REASONING、KNOWLEDGE_BASE、SSE、SYSTEM）
curl http://localhost:8080/api/monitoring/realtime?type=API | jq .

# 获取系统资源
curl http://localhost:8080/api/monitoring/system | jq .

# 获取健康状态
curl http://localhost:8080/api/monitoring/health | jq .
```

---

## 📊 收集的指标类型

### 1️⃣ API 指标

```json
{
  "metricsType": "API",
  "apiEndpoint": "/api/agent/chat",
  "httpMethod": "POST",
  "statusCode": 200,
  "responseTime": 1250,
  "requestSize": 256,
  "responseSize": 4096,
  "success": true
}
```

**关键指标**:
- 响应时间（毫秒）
- HTTP 状态码
- 请求/响应大小
- 成功/失败标记

### 2️⃣ 推理引擎指标

```json
{
  "metricsType": "REASONING",
  "sessionId": "xxx",
  "query": "什么是机器学习？",
  "iterations": 3,
  "thinking": 5,
  "responseTime": 2500,
  "success": true
}
```

**关键指标**:
- 推理迭代次数
- 思维链步骤数
- 总响应时间

### 3️⃣ 知识库指标

```json
{
  "metricsType": "KNOWLEDGE_BASE",
  "query": "什么是 Java？",
  "knowledgeBaseHits": 5,
  "avgSimilarity": 0.85,
  "topKResults": 5,
  "success": true
}
```

**关键指标**:
- 匹配文档数
- 平均相似度
- 返回结果数

### 4️⃣ SSE 指标

```json
{
  "metricsType": "SSE",
  "sseConnections": 3,
  "sseEvents": 25,
  "sseConnectionDuration": 5000,
  "success": true
}
```

**关键指标**:
- 活跃连接数
- 发送事件数
- 连接持续时间

### 5️⃣ 系统资源指标

```json
{
  "metricsType": "SYSTEM",
  "cpuUsage": 45.23,
  "memoryUsage": 62.15,
  "threadCount": 42
}
```

**关键指标**:
- CPU 使用率 %
- 内存使用率 %
- 活跃线程数

---

## 🔌 REST API 端点

### 获取摘要

```
GET /api/monitoring/summary
```

**响应**:
```json
{
  "system": {
    "cpuUsage": "45.23%",
    "memoryUsage": "62.15%",
    "threadCount": 42
  },
  "performance": {
    "totalApiCalls": 250,
    "successCount": 240,
    "errorCount": 10,
    "successRate": "96.00%",
    "avgApiResponseTime": "125.45 ms"
  },
  "storage": {
    "count": 1500,
    "byType": {
      "API": 800,
      "REASONING": 500,
      "KNOWLEDGE_BASE": 200
    }
  },
  "timestamp": 1772431500000
}
```

### 获取实时指标

```
GET /api/monitoring/realtime?type=API&limit=50
```

**参数**:
- `type` (可选): API, REASONING, KNOWLEDGE_BASE, SSE, SYSTEM
- `limit` (可选): 返回的记录数，默认 100

**响应**: 指标数组，最新的排在最后

### 获取历史数据

```
GET /api/monitoring/history?days=7
```

**参数**:
- `days`: 显示过去几天的数据，默认 7 天

**响应**:
```json
{
  "dailyStats": {
    "2026-03-01": 450,
    "2026-03-02": 520,
    "2026-03-03": 480
  },
  "days": 7,
  "timestamp": 1772431500000
}
```

### 获取聚合统计

```
GET /api/monitoring/aggregate?type=API
```

**参数**:
- `type` (可选): 按类型聚合

**响应**:
```json
{
  "stats": {
    "count": 800,
    "avgResponseTime": 125.5,
    "minResponseTime": 10,
    "maxResponseTime": 5000,
    "p95ResponseTime": 450,
    "successRate": "96.00%"
  },
  "type": "API",
  "timestamp": 1772431500000
}
```

### 获取系统资源

```
GET /api/monitoring/system
```

**响应**:
```json
{
  "metrics": {
    "cpuUsage": "45.23%",
    "memoryUsage": "62.15%",
    "heapUsedMB": 256,
    "heapMaxMB": 512,
    "threadCount": 42,
    "peakThreadCount": 50
  },
  "activeSseConnections": 3,
  "timestamp": 1772431500000
}
```

### 获取健康状态

```
GET /api/monitoring/health
```

**响应**:
```json
{
  "status": "HEALTHY",  // HEALTHY, WARNING, CRITICAL
  "cpu": 45.23,
  "memory": 62.15,
  "threads": 42,
  "sseConnections": 3,
  "timestamp": 1772431500000
}
```

### 获取完整仪表板数据

```
GET /api/monitoring/dashboard
```

返回所有指标和图表所需的数据，用于仪表板展示。

### 清空指标

```
DELETE /api/monitoring/metrics
```

清空所有收集的指标数据（谨慎使用）。

---

## 📈 仪表板功能

### 实时监控

- **CPU 和内存使用率**：可视化进度条
- **活跃线程数**：实时线程计数
- **SSE 连接数**：当前流式连接数

### 性能统计

- **总 API 调用数**
- **成功率**：成功/总数百分比
- **平均响应时间**：API 和推理引擎分别统计

### 图表展示

- **响应时间趋势**：折线图展示最近 20 条 API 请求的响应时间
- **请求类型分布**：饼图展示各类型指标的占比

### 最近事件表格

- 时间、指标类型、端点/查询、响应时间、成功/失败
- 最新的 20 条记录，倒序显示

### 健康状态

- 自动判断系统状态（HEALTHY/WARNING/CRITICAL）
- 状态徽章实时更新
- 阈值：CPU/内存 > 90% 为 CRITICAL，> 75% 为 WARNING

---

## 🔧 自动指标收集

### API 指标自动收集

通过 `MetricsInterceptor` 拦截器，所有 `/api/**` 请求都会自动收集：
- ✅ 请求方法和端点
- ✅ HTTP 状态码
- ✅ 响应时间
- ✅ 请求/响应大小

**排除的端点**:
- `/api/monitoring/*`（避免递归）
- `/health`

### 在代码中手动记录

```java
@Autowired
private MetricsCollector metricsCollector;

// 记录 API 指标
metricsCollector.recordApiMetrics(
    "/api/agent/chat",      // 端点
    "POST",                  // 方法
    200,                     // 状态码
    1250L,                   // 响应时间（ms）
    256,                     // 请求大小
    4096                     // 响应大小
);

// 记录推理指标
metricsCollector.recordReasoningMetrics(
    sessionId,               // 会话 ID
    "你的查询",             // 查询内容
    3,                       // 迭代次数
    5,                       // 思维步骤数
    2500L,                   // 响应时间
    true,                    // 是否成功
    null                     // 错误信息
);

// 记录知识库指标
metricsCollector.recordKnowledgeBaseMetrics(
    sessionId,
    "什么是 Java？",
    5,                       // 匹配数
    0.85,                    // 平均相似度
    5                        // 返回数
);
```

---

## 💾 数据存储

### 内存存储

- 最近 10,000 条指标保存在内存中
- 快速查询和实时显示
- 自动溢出时移除最旧的记录

### 文件存储

- 指标按日期保存为 JSONL 文件
- 位置：`data/metrics/{YYYY-MM-DD}.jsonl`
- 每行一条记录（JSON Lines 格式）
- 用于历史数据保留和长期分析

### 聚合统计

- 自动计算平均值、最小值、最大值、P95
- 统计成功率和错误率
- 按指标类型分组统计

---

## 🚨 告警阈值

当前内置的告警条件（通过健康检查）：

| 条件 | 阈值 | 状态 |
|------|------|------|
| CPU > 90% 或内存 > 90% | CRITICAL | 🔴 |
| CPU > 75% 或内存 > 75% | WARNING | 🟡 |
| 其他 | HEALTHY | 🟢 |

访问 `/api/monitoring/health` 获取当前状态。

---

## 📋 使用场景

### 场景 1：性能优化

```bash
# 1. 获取平均响应时间
curl 'http://localhost:8080/api/monitoring/aggregate?type=API' | jq '.stats.avgResponseTime'

# 2. 查看 P95 响应时间
curl 'http://localhost:8080/api/monitoring/aggregate?type=API' | jq '.stats.p95ResponseTime'

# 3. 分析慢查询
curl 'http://localhost:8080/api/monitoring/realtime?type=API&limit=100' | \
  jq '.metrics[] | select(.responseTime > 5000)'
```

### 场景 2：容量规划

```bash
# 查看历史增长
curl 'http://localhost:8080/api/monitoring/history?days=30' | jq '.dailyStats'

# 分析峰值
curl 'http://localhost:8080/api/monitoring/realtime' | \
  jq 'group_by(.timestamp) | map({time: .[0].timestamp, count: length})'
```

### 场景 3：问题诊断

```bash
# 查看成功率
curl 'http://localhost:8080/api/monitoring/summary' | jq '.performance.successRate'

# 找出失败的请求
curl 'http://localhost:8080/api/monitoring/realtime' | \
  jq '.metrics[] | select(.success == false)'

# 查看错误类型分布
curl 'http://localhost:8080/api/monitoring/realtime' | \
  jq '[.metrics[] | select(.success == false)] | group_by(.apiEndpoint)'
```

---

## 🎯 最佳实践

### 1. 定期检查仪表板

- 每天查看一次性能趋势
- 及时发现异常波动

### 2. 设置告警规则

- 监控 CPU 和内存使用率
- 当接近阈值时提前优化

### 3. 定期分析指标

- 每周生成性能报告
- 识别需要优化的瓶颈

### 4. 保留历史数据

- 指标自动持久化到文件
- 用于长期趋势分析

### 5. 集成到监控系统

- 导出指标到 Prometheus/ELK
- 与告警系统集成

---

## 🔍 故障排除

### 指标不显示

1. 检查应用是否运行：
   ```bash
   curl http://localhost:8080/api/agent/health
   ```

2. 检查指标收集器初始化：
   查看应用日志中是否有初始化消息

3. 发送几个 API 请求生成指标：
   ```bash
   curl -X POST http://localhost:8080/api/agent/chat \
     -H "Content-Type: application/json" \
     -d '{"query":"你好"}'
   ```

### 内存使用持续增长

1. 检查内存指标：
   ```bash
   curl http://localhost:8080/api/monitoring/system | jq '.metrics.memoryUsage'
   ```

2. 内存缓冲配置（最多 10,000 条记录）：
   - 如需增加，修改 `MetricsStorageService.MAX_MEMORY_METRICS`

3. 清空指标：
   ```bash
   curl -X DELETE http://localhost:8080/api/monitoring/metrics
   ```

---

## 📞 支持

详细配置见各组件源代码：
- `MetricsCollector.java` - 指标收集
- `MetricsStorageService.java` - 数据存储
- `MonitoringController.java` - REST API
- `MetricsInterceptor.java` - 自动拦截

---

**版本**: 1.0  
**最后更新**: 2026-03-02  
**状态**: ✅ 生产可用
