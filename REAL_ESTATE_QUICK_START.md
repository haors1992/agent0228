# 🏠 房产行业数字员工 - 快速启动指南

## 3 分钟快速启动

### 第 1 步：导入知识库（30 秒）

```bash
chmod +x scripts/import_real_estate_knowledge.sh
./scripts/import_real_estate_knowledge.sh
```

✅ 会自动导入 4 个行业知识文档（交易流程、户型评估、异议处理、贷款知识）

### 第 2 步：配置行业类型（30 秒）

编辑 `src/main/resources/application.yml`：

```yaml
agent:
  industry: real_estate  # ← 改这一行
  max-iterations: 10
  timeout: 300
```

### 第 3 步：重启应用（2 分钟）

```bash
mvn clean package -DskipTests
java -jar target/agent0228-1.0.0.jar
```

🎉 完成！现在你的 Agent 已经成为房产经纪人了。

---

## 验证是否成功

### 方法 1：查看工具列表

```bash
curl http://localhost:8080/api/agent/tools | jq '.tools[] | select(.name | contains("housing"))'
```

应该看到 5 个房产工具：
- ✅ housing_estimate
- ✅ policy_query
- ✅ client_matching
- ✅ housing_evaluation
- ✅ transaction_fee

### 方法 2：查看知识库

```bash
curl http://localhost:8080/api/knowledge/stats | jq '.'
```

应该看到 `totalDocuments` 至少为 4。

### 方法 3：进行测试对话

```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "query": "我有 500 万预算，想在朝阳区买套 2 房 2 厅，怎样判断这个价格合理？"
  }' | jq '.result'
```

应该看到：
- Agent 自动问了几个深化理解的问题
- 调用了 `housing_estimate` 或 `policy_query` 工具
- 给出了专业建议和具体房源推荐

---

## 测试场景（复制即用）

### 场景 1：首次购房者（保守型）

**用户输入：**
```
我是首套房，预算 300 万，在海淀区工作，想买学区房，但不了解流程和政策
```

**预期 Agent 表现：**
✓ 询问具体的购房目的（自住还是投资）
✓ 询问何时需要入住
✓ 使用 `policy_query` 查询首套房优惠政策
✓ 使用 `housing_estimate` 估价合理房价
✓ 详细讲解交易流程

### 场景 2：投资客户（收益型）

**用户输入：**
```
我想在深圳投资买房，预算 500 万，主要看升值潜力和租赁收益，不会自住
```

**预期 Agent 表现：**
✓ 重点分析区域升值潜力
✓ 推荐地铁房、商圈房
✓ 计算租赁回报率
✓ 分析政策风险（限售、征税等）
✓ 建议投资组合（而不只是单套）

### 场景 3：改善型客户（体验型）

**用户输入：**
```
现在住在 60 平的 1 房，想换成 120 平的 3 房，家里有老人和小孩，最在乎学区和医疗配套
```

**预期 Agent 表现：**
✓ 重点关注学校排名和医疗距离
✓ 详细分析户型采光和通风
✓ 推荐有电梯的楼层（方便老人）
✓ 强调长期居住体验而非价格

### 场景 4：异议处理（高级）

**用户输入：**
```
这套房看起来不错，但为什么价格比隔壁小区便宜 50 万？我怕有猫腻
```

**预期 Agent 表现：**
✓ 认同客户的谨慎态度
✓ 列举可能原因（楼层、朝向、装修、楼龄等）
✓ 建议进行第三方检测
✓ 提供产权核实的具体流程

---

## 定制你自己的行业版本

如果你想定制其他行业（医疗、法律、金融等），按以下步骤：

### Step 1: 创建行业工具

```bash
# 创建新文件
src/main/java/com/agent/tool/industry/YOUR_INDUSTRY_Tools.java

# 参考模板：src/main/java/com/agent/tool/industry/RealEstateTools.java
# 用你的行业工具替换房产工具
```

### Step 2: 创建行业知识库导入脚本

```bash
# 创建新脚本
scripts/import_YOUR_INDUSTRY_knowledge.sh

# 参考：scripts/import_real_estate_knowledge.sh
# 根据你的行业定制知识文档
```

### Step 3: 创建行业系统提示词

```
src/main/resources/prompts/YOUR_INDUSTRY_agent.txt

参考：src/main/resources/prompts/real_estate_agent.txt
根据你的行业定制角色设定
```

### Step 4: 修改配置文件

```yaml
agent:
  industry: YOUR_INDUSTRY  # 改这一行
```

### Step 5: 重新编译和启动

```bash
mvn clean package -DskipTests
java -jar target/agent0228-1.0.0.jar
```

---

## 预设的行业模板

以下行业已经有基础模板，你可以直接扩展：

- ✅ **real_estate** (房产中介) - 已完整实现
- ⏳ **healthcare** (医疗咨询) - 工具代码框架
- ⏳ **finance** (财务顾问) - 工具代码框架
- ⏳ **legal** (法律助手) - 工具代码框架
- ⏳ **customer_service** (客服机器人) - 工具代码框架

---

## 常见定制问题

### Q: 如何连接真实的企业系统（CRM、ERP 等）？

**A:** 修改对应的工具类，替换模拟演示的方法为真实 API 调用：

```java
// 原来（演示）
public ToolResult matchClients(String input) {
    // ... 模拟返回房源
}

// 改成（真实）
public ToolResult matchClients(String input) {
    // 调用真实 CRM API
    List<Property> properties = crmService.queryProperties(filter);
    // 转换为结果格式
    return ToolResult.success(...);
}
```

### Q: 如何优化 Agent 的行业专业性？

**A:** 主要通过以下 3 个维度：

1. **知识库质量** - 纳入更多、更准确的行业知识
2. **工具准确性** - 工具返回的数据要真实、及时
3. **提示词深度** - 增加角色细节、工作流程、话术等

### Q: 如何评估 Agent 的效果？

**A:** 建议使用以下指标：

```
维度 1：对话自然度（像不像行业专家）
  • 有没有用行业术语
  • 有没有问出有深度的问题
  • 有没有给出数据支撑

维度 2：工具调用准确性
  • 有没有调用正确的工具
  • 调用工具的时机对不对
  • 有没有过度依赖工具

维度 3：业务成果
  • 客户有没有被说服
  • 有没有按 Agent 建议行动
  • 满意度评分多少

维度 4：成本效率
  • 均匀 LLM 调用成本（tokens）
  • 平均对话轮数
  • 首次成功率（不需要重新开始）
```

### Q: 能否一个 Agent 同时服务多个行业？

**A:** 可以，但推荐分离：

```yaml
# 方案 1：多 Agent 方案（推荐）
spring:
  application:
    instances:
      - agent-real-estate
      - agent-healthcare
      - agent-finance

# 方案 2：单 Agent 多行业方案（不推荐）
agent:
  multi-industry: true
  # 但这样会让提示词冲突，不专业
```

---

## 性能优化建议

如果你的 Agent 在生产环境运行，建议：

### 1. 知识库优化

```yaml
knowledge:
  enabled: true
  cache-enabled: true          # 启用知识库缓存
  cache-ttl: 3600              # 缓存 1 小时
  top-k: 5                      # 每次查询 5 个最相关文档
  min-similarity: 0.60          # 相似度阈值
```

### 2. LLM 调用优化

```yaml
llm:
  deepseek:
    temperature: 0.5            # 降低随机性提高准确度
    max-tokens: 2048            # 限制输出长度降低成本
    timeout: 120                # 合理超时避免卡住
```

### 3. 对话历史优化

```yaml
agent:
  conversation:
    max-history: 10             # 只保留最近 10 条
    context-window: 1500        # 压缩上下文窗口
    auto-summary: true          # 自动摘要长对话
```

---

## 问题排查

### 问题 1: Agent 不调用行业工具

**原因可能：**
1. 工具没有被 Spring 发现（检查 `@Component` 注解）
2. 工具注解参数不完整（检查 `@Tool` 的 name 和 description）
3. LLM 没有理解调用工具的时机

**解决：**
```bash
# 1. 检查工具是否被注册
curl http://localhost:8080/api/agent/tools | jq '.tools | length'

# 2. 检查工具具体信息
curl http://localhost:8080/api/agent/tools | jq '.tools[] | select(.name=="housing_estimate")'

# 3. 调整系统提示词，明确什么时候调用工具
```

### 问题 2: Agent 回答不够专业

**原因可能：**
1. 行业知识不够完整
2. LLM temperature 过高（太创意）
3. 系统提示词不够详细

**解决：**
```bash
# 1. 增加知识库文档
./scripts/import_real_estate_knowledge.sh

# 2. 降低 temperature
temperature: 0.3  # 改成 0.3-0.5 之间

# 3. 丰富系统提示词
# 编辑 src/main/resources/prompts/real_estate_agent.txt
```

### 问题 3: 对话速度慢

**原因可能：**
1. LLM API 响应慢
2. 知识库查询慢
3. 整体 Agent 循环次数过多

**解决：**
```yaml
# 1. 减少知识库查询
knowledge:
  top-k: 3  # 改成 3（而不是 5）

# 2. 限制迭代次数
agent:
  max-iterations: 5  # 改成 5（而不是 10）

# 3. 启用异步处理
enable-async: true
```

---

## 下一步建议

✅ **立即做**
1. 按上面的 3 分钟步骤启动房产 Agent
2. 用 4 个测试场景验证效果
3. 根据实际情况优化提示词和工具

🎯 **短期做（1-2 周）**
1. 连接真实企业系统（CRM、估价系统等）
2. 增加更多行业知识
3. 优化工具的返回数据格式

🚀 **中期做（1-3 个月）**
1. 实现多行业支持
2. 搭建 Agent 性能监控系统
3. 建立客户反馈机制，持续优化

💼 **长期做（3-12 个月）**
1. 考虑微调专属 LLM 模型
2. 实现行业特定的知识图谱
3. 建立 Agent 应用生态市场

---

## 技术支持

遇到问题？按以下顺序排查：

1. 查看应用日志：`target/logs/agent-*.log`
2. 查看 API 响应：用 curl 或 Postman 测试接口
3. 检查配置文件：`src/main/resources/application.yml`
4. 参考项目文档：`INDUSTRY_CUSTOMIZATION_GUIDE.md`
5. 检查社区讨论：项目 GitHub Issues

祝你成功打造自己的行业 AI 数字员工！🚀
