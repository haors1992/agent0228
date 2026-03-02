# 监控指标快速参考

## 🚀 快速命令

```bash
# 打开仪表板
http://localhost:8080/monitoring-dashboard.html

# 健康检查
curl http://localhost:8080/api/monitoring/health | jq .

# 实时摘要
curl http://localhost:8080/api/monitoring/summary | jq .

# 最近 API 指标
curl 'http://localhost:8080/api/monitoring/realtime?type=API&limit=20' | jq .

# 系统资源
curl http://localhost:8080/api/monitoring/system | jq .

# 7 天历史
curl 'http://localhost:8080/api/monitoring/history?days=7' | jq .

# 性能统计
curl http://localhost:8080/api/monitoring/aggregate?type=API | jq .
```

---

## 📊 REST API 端点总览

| 端点 | 方法 | 说明 | 参数 |
|------|------|------|------|
| `/api/monitoring/summary` | GET | 摘要信息 | - |
| `/api/monitoring/realtime` | GET | 实时指标 | `type`, `limit` |
| `/api/monitoring/history` | GET | 历史数据 | `days` |
| `/api/monitoring/aggregate` | GET | 聚合统计 | `type` |
| `/api/monitoring/system` | GET | 系统资源 | - |
| `/api/monitoring/performance` | GET | 性能指标 | - |
| `/api/monitoring/health` | GET | 健康状态 | - |
| `/api/monitoring/dashboard` | GET | 仪表板数据 | - |
| `/api/monitoring/metrics` | DELETE | 清空数据 | - |

---

## 🎯 5 个关键指标

### 1. 响应时间
```bash
curl http://localhost:8080/api/monitoring/aggregate?type=API | \
  jq '.stats | {avg: .avgResponseTime, p95: .p95ResponseTime, max: .maxResponseTime}'
```
**阈值**: P95 应 < 500ms

### 2. 成功率
```bash
curl http://localhost:8080/api/monitoring/summary | \
  jq '.performance.successRate'
```
**阈值**: 应 > 99%

### 3. CPU 使用率
```bash
curl http://localhost:8080/api/monitoring/health | jq '.cpu'
```
**阈值**: < 75% (正常), 75-90% (警告), > 90% (严重)

### 4. 内存使用率
```bash
curl http://localhost:8080/api/monitoring/health | jq '.memory'
```
**阈值**: < 75% (正常), 75-90% (警告), > 90% (严重)

### 5. 活跃连接数
```bash
curl http://localhost:8080/api/monitoring/system | jq '.activeSseConnections'
```
**说明**: 当前流式连接数，应 <= 100

---

## 📈 日常监控检查表

| 检查项 | 命令 | 目标值 |
|--------|------|--------|
| 系统可用性 | `curl /health` | HEALTHY 🟢 |
| 响应时间 | `/aggregate?type=API` | <500ms (p95) |
| 错误率 | `/summary` | <1% |
| CPU 使用 | `/system` | <75% |
| 内存使用 | `/system` | <75% |
| 取数记录 | `/summary` | > 0 |

---

## 💡 常见问题排查

### 指标为 0
→ 需要先发送 API 请求生成指标

### CPU/内存异常高
→ 查看 `/realtime` 找出最慢的请求

### 查找慢查询
```bash
curl 'http://localhost:8080/api/monitoring/realtime' | \
  jq '.metrics | sort_by(-.responseTime) | .[0:5]'
```

### 找出失败的请求
```bash
curl 'http://localhost:8080/api/monitoring/realtime' | \
  jq '.metrics[] | select(.success == false)'
```

### 统计各端点的调用数
```bash
curl 'http://localhost:8080/api/monitoring/realtime?limit=1000' | \
  jq '[.metrics[].apiEndpoint] | group_by(.) | map({endpoint: .[0], count: length})'
```

---

## 🔧 代码集成示例

```java
// 1. 注入收集器
@Autowired
private MetricsCollector metricsCollector;

// 2. 记录推理指标
metricsCollector.recordReasoningMetrics(
  sessionId,           // 会话 ID
  userQuery,          // 用户查询
  iterations,         // 迭代次数
  thinkingSteps,      // 思维链步骤
  duration,           // 耗时（ms）
  success,            // 是否成功
  errorMsg            // 错误信息
);

// 3. 记录知识库检索
metricsCollector.recordKnowledgeBaseMetrics(
  sessionId,
  query,
  hitCount,           // 匹配数
  similarity,         // 平均相似度
  topK                // 返回数量
);

// API 指标自动收集，无需手动调用
```

---

## 🎨 仪表板快速导览

| 区域 | 内容 | 刷新频率 |
|------|------|---------|
| 顶部 | 健康状态徽章 | 实时 |
| 上方 | CPU/内存/线程/SSE 连接 | 5 秒 |
| 中部 | API 统计 + 推理统计 | 5 秒 |
| 左下 | 响应时间趋势图 | 5 秒 |
| 右下 | 请求类型分布饼图 | 5 秒 |
| 底部 | 最近 20 条指标表格 | 5 秒 |

---

## 📱 监控告警规则

设置外部监控时的推荐阈值：

```
告警级别: CRITICAL 🔴
  CPU > 90% 
  OR 内存 > 90%
  OR 成功率 < 95%
  OR P95 响应时间 > 5000ms

告警级别: WARNING 🟡
  CPU > 75%
  OR 内存 > 75%
  OR 成功率 < 98%
  OR P95 响应时间 > 1000ms

告警级别: INFO ℹ️
  新会话创建
  知识库更新
  系统重启
```

---

## 🔐 生产环保最佳实践

1. **定期备份指标**
   ```bash
   # 备份指标文件
   cp -r data/metrics data/metrics.backup.$(date +%Y%m%d)
   ```

2. **定期清理旧数据**
   - 删除超过 90 天的 JSONL 文件
   - 保留最近 7 天的详细指标

3. **监控磁盘空间**
   - 监控 `data/metrics/` 目录大小
   - 设置告警规则

4. **集成生产监控**
   - 导出到 Prometheus/Grafana
   - 集成 ELK Stack 日志分析
   - 设置 PagerDuty 告警

5. **性能调优**
   ```bash
   # 增加内存缓冲大小（如需要）
   # ModifiedMetricsStorageService.MAX_MEMORY_METRICS = 50000
   ```

---

## 📞 技术支持文档

- `MONITORING_GUIDE.md` - 完整指南
- `QUICK_REFERENCE.md` - 系统快速参考
- 源码：`src/main/java/com/agent/monitoring/`

**版本**: 1.0 | **更新**: 2026-03-02
