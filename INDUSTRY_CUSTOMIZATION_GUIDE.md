# 🏭 行业数字员工定制指南

## 概述

将通用 Agent 框架改造成特定行业的数字员工，需要从 5 个维度定制：

```
┌─────────────────────────────────────────────┐
│    行业数字员工架构                         │
├─────────────────────────────────────────────┤
│ 1️⃣  知识库定制 ← 行业领域知识库             │
│ 2️⃣  工具扩展   ← 行业特定工具集             │
│ 3️⃣  提示词    ← 角色定义和行为规范         │
│ 4️⃣  数据集成  ← 连接行业系统/API           │
│ 5️⃣  参数优化  ← 行业特性的超参调优         │
└─────────────────────────────────────────────┘
```

---

## 方案1️⃣：知识库定制

### 目标
将行业的所有关键知识（流程、规范、案例等）纳入知识库，使 Agent 能在推理时引用。

### 步骤

#### 第1步：准备行业知识文档

以**房产中介**为例，创建 `data/industry_docs/` 结构：

```bash
mkdir -p data/industry_docs
```

创建几个知识文档：

**data/industry_docs/房产销售话术.md**
```markdown
# 房产销售话术库

## 看房流程
1. 热情接待，了解需求
2. 介绍房产位置、户型、配套
3. 指出卖点（朝向、采光、升值潜力）
4. 处理异议
5. 安排下一步

## 常见异议处理
Q: 价格太高了
A: 我们这个楼盘位于黄金商圈，周边地铁...，投资价值高

Q: 户型不够大
A: 这个户型利用率达到88%，比同类楼盘平均水准高...
```

**data/industry_docs/行业知识库.json**
```json
[
  {
    "title": "房产交易流程",
    "content": "二手房交易流程：1.看房 2.签订购房意向书 3.审查房产证 4.评估价格 5.申请贷款 6.签订合同 7.办理过户 8.支付尾款 9.交付房产",
    "category": "交易流程",
    "source": "公司标准操作流程"
  },
  {
    "title": "户型评估标准",
    "content": "评估户型时关注：1.户型方正度 2.采光通风 3.动静分区 4.干湿分区 5.面积利用率 6.朝向 7.楼层位置",
    "category": "专业知识",
    "source": "销售培训材料"
  }
]
```

#### 第2步：编写知识导入脚本

创建 `scripts/import_industry_knowledge.sh`：

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"

# 导入所有知识文档
echo "📚 导入行业知识库..."

# 房产交易流程
curl -X POST "${BASE_URL}/api/knowledge/documents" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "房产交易完整流程（二手房）",
    "content": "二手房交易流程分为以下阶段：\n1. 看房阶段：了解房产基本情况，评估卖点。\n2. 意向阶段：签订购房意向书，确认交易意向。\n3. 审查阶段：审查房产证、产权、抵押等信息。\n4. 评估阶段：评估房价，是否合理。\n5. 融资阶段：买方申请贷款，获得批准。\n6. 签约阶段：双方签订购房合同，明确权利义务。\n7. 过户阶段：办理产权过户手续，更新房产证。\n8. 支付阶段：买方支付剩余房款。\n9. 交付阶段：卖方交付房产，清空个人物品。",
    "category": "交易流程",
    "source": "公司标准操作手册"
  }'

# 户型评估知识
curl -X POST "${BASE_URL}/api/knowledge/documents" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "户型评估的八大关键指标",
    "content": "评估一个户型的质量，需要关注这八个指标：\n1. 户型方正度：房间应尽量方正，长宽比不超过1:2。\n2. 采光通风：客厅卧室应有窗户，南向为最佳。\n3. 动静分区：卧室应远离客厅，避免噪音干扰。\n4. 干湿分区：厨卫应远离卧室和客厅。\n5. 面积利用率：套内面积与建筑面积比例。\n6. 朝向评估：南向>东向>东南向>西向>北向。\n7. 楼层位置：中间楼层采光好，避免一层和顶层。\n8. 户型开间：应满足建筑规范（开间3.0-3.6米最优）。",
    "category": "专业知识",
    "source": "行业标准"
  }'

# 销售异议处理
curl -X POST "${BASE_URL}/api/knowledge/documents" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "房产销售常见异议及应对模板",
    "content": "顾客异议是销售中的正常情况，需要巧妙应对：\n\n异议1：价格太高了\n应对：\"我们这个楼盘位于黄金商圈，地铁口只需2分钟，周边商业繁荣，这个价格在同类楼盘中已经很有竞争力。而且从投资角度看，这片区域升值潜力大。\"\n\n异议2：户型太小\n应对：\"这个户型设计非常紧凑，利用率达到88%，比同类楼盘平均水准高5%。而且这样的户型非常好出租或出售，市场接受度很高。\"\n\n异议3：周边配套不足\n应对：\"这个楼盘到地铁站只需300米，步行5分钟即可到达；周边有三所学校，医院、超市、水果店一应俱全。\"\n\n异议4：贷款利率太高\n应对：\"现在的贷款利率确实是政策周期的一部分，但从长期看，现在上车反而是最佳时机，房价已经相对稳定。\"",
    "category": "销售技巧",
    "source": "销售培训"
  }'

echo "✅ 知识库导入完成"
```

运行脚本：
```bash
chmod +x scripts/import_industry_knowledge.sh
./scripts/import_industry_knowledge.sh
```

### 效果验证
```bash
curl http://localhost:8080/api/knowledge/stats
# 应该看到文档数量增加
```

---

## 方案2️⃣：工具扩展

### 目标
创建行业特定工具，使 Agent 能够调用行业系统（CRM、估价系统、政策查询等）。

### 步骤

#### 第1步：创建行业工具类

创建 `src/main/java/com/agent/tool/industry/RealEstateTools.java`：

```java
package com.agent.tool.industry;

import com.agent.tool.annotation.Tool;
import com.agent.tool.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 房产行业工具集
 * 提供房产估价、政策查询、客户管理等功能
 */
@Slf4j
@Component
public class RealEstateTools {
    
    /**
     * 房产估价工具
     * 根据位置、户型、年龄估算房价
     */
    @Tool(
        name = "housing_estimate",
        description = "根据房产信息（地点、户型、年龄）估算合理房价。输入格式：'地址,户型,年龄' 如 '朝阳区建国路,2房2厅,5年'"
    )
    public ToolResult estimateHousingPrice(String input) {
        try {
            String[] parts = input.split(",");
            if (parts.length != 3) {
                return ToolResult.failure("housing_estimate", "格式错误，请提供：地址,户型,年龄");
            }
            
            String location = parts[0].trim();
            String housingType = parts[1].trim();
            int age = Integer.parseInt(parts[2].trim());
            
            // 模拟估价逻辑（实际应接入实时房产数据库）
            double basePrice = estimateBasePrice(location);
            double typeMultiplier = getTypeMultiplier(housingType);
            double ageDiscount = 1.0 - (age * 0.02); // 每年贬值2%
            
            double estimatedPrice = basePrice * typeMultiplier * ageDiscount;
            
            String result = String.format(
                "📍 %s - %s 房产估价：%.0f 万元（基价：%.0f万，户型系数：%.2f，年龄折扣：%.2f）",
                location, housingType, estimatedPrice, basePrice, typeMultiplier, ageDiscount
            );
            
            return ToolResult.success("housing_estimate", result);
            
        } catch (Exception e) {
            return ToolResult.failure("housing_estimate", "估价失败：" + e.getMessage());
        }
    }
    
    /**
     * 政策查询工具
     * 查询限购、限售、税费等政策
     */
    @Tool(
        name = "policy_query",
        description = "查询房产相关政策（限购、限售、税费等）。输入为城市名，如：查询'北京'的限购政策"
    )
    public ToolResult queryPolicy(String city) {
        try {
            // 模拟政策库（应接入实时政策数据库）
            return switch (city) {
                case "北京" -> ToolResult.success("policy_query",
                    "北京限购政策：\n" +
                    "✓ 本市户口可购2套，外地户口限1套\n" +
                    "✓ 需要5年纳税或社保史\n" +
                    "✓ 新房需持证满2年才能转让\n" +
                    "✓ 契税：首套90㎡以下1%，90-144㎡1.5%，144㎡以上3%"
                );
                case "上海" -> ToolResult.success("policy_query",
                    "上海限购政策：\n" +
                    "✓ 本市户口可购2套，外地户口限1套\n" +
                    "✓ 需要交满5年社保或税\n" +
                    "✓ 房产满2年可交易，不满2年需交增值税\n" +
                    "✓ 契税：1.5%-3%（按房价分档）"
                );
                default -> ToolResult.failure("policy_query", 
                    "暂未收录 " + city + " 的政策，请咨询公司政策部"
                );
            };
        } catch (Exception e) {
            return ToolResult.failure("policy_query", "查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 客户匹配工具
     * 根据需求找到匹配的房产
     */
    @Tool(
        name = "client_matching",
        description = "根据客户需求（预算、户型、地点）推荐房产。输入格式：'预算(万),户型,地点' 如'500,2房2厅,朝阳区'"
    )
    public ToolResult matchClients(String input) {
        try {
            String[] parts = input.split(",");
            if (parts.length != 3) {
                return ToolResult.failure("client_matching", "格式错误");
            }
            
            double budget = Double.parseDouble(parts[0].trim());
            String type = parts[1].trim();
            String location = parts[2].trim();
            
            // 模拟房产库查询
            String recommendation = String.format(
                "✅ 根据您的需求（预算%.0f万、%s户型、%s）：\n" +
                "推荐房源1：%s小区，总价%.0f万，户型%s，楼层中层，南向阳光房\n" +
                "推荐房源2：%s公寓，总价%.0f万，户型%s，交通便利，配套齐全\n" +
                "推荐房源3：%s商住，总价%.0f万，户型%s，适合投资，升值潜力大",
                budget, type, location,
                location + "A", budget * 0.95, type,
                location + "B", budget * 1.0, type,
                location + "C", budget * 1.05, type
            );
            
            return ToolResult.success("client_matching", recommendation);
            
        } catch (Exception e) {
            return ToolResult.failure("client_matching", "匹配失败：" + e.getMessage());
        }
    }
    
    // ===== 辅助方法 =====
    
    private double estimateBasePrice(String location) {
        // 模拟不同地点的基础房价
        return switch (location) {
            case "朝阳区建国路" -> 600.0;
            case "浦东世纪大道" -> 650.0;
            case "海淀中关村" -> 550.0;
            default -> 400.0;
        };
    }
    
    private double getTypeMultiplier(String type) {
        return switch (type) {
            case "1房1厅" -> 0.8;
            case "2房2厅" -> 1.0;
            case "3房2厅" -> 1.3;
            case "4房3厅" -> 1.6;
            default -> 1.0;
        };
    }
}
```

#### 第2步：工具自动注册

验证工具被框架自动发现（无需手动配置）：

```bash
# 启动应用后，查看工具列表
curl http://localhost:8080/api/agent/tools | jq '.tools[] | select(.name | contains("housing_estimate"))'

# 应该看到新工具被注册
```

### 效果验证

在对话中测试工具：
```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "query": "帮我估算朝阳区建国路的一套2房2厅、5年楼龄的房产价格"
  }'

# Agent 会自动调用 housing_estimate 工具
```

---

## 方案3️⃣：系统提示词定制

### 目标
定义 Agent 的角色、专业风格、行业规范，让它像一个真正的行业专家。

### 步骤

#### 第1步：创建行业提示词配置

创建 `src/main/resources/prompts/real_estate_agent.txt`：

```
你是一位资深房产经纪人，拥有10年行业经验。

【角色设定】
- 姓名：李经理
- 职位：高级房产顾问
- 专长：二手房交易、投资建议、政策解读

【行为规范】
1. 专业性：每个建议都要基于行业知识和经验
2. 诚实性：不隐瞒房产缺陷，但要有技巧地提出优势
3. 客户至上：了解客户需求，而不是盲目推销
4. 合规性：严格遵守房产交易法规和公司政策

【工作流程】
第一步：理解客户需求（预算、户型、位置、用途）
第二步：推荐合适房源（使用 client_matching 工具）
第三步：详细介绍（使用行业知识库中的知识）
第四步：处理异议（参考销售异议处理库）
第五步：后续跟进（确认下一步行动）

【推荐话术】
- 开场：\"您好，我是李经理，很高兴为您服务。请问您是自住还是投资？\"
- 介绍：先展示房产的卖点，再详细讲解技术性细节
- 异议处理：先认同客户感受，再用数据和经验说服
- 成交：\"那我们什么时候看房？我可以帮您预约...\"

【禁止行为】
✗ 虚假宣传（如虚报面积、隐瞒瑕疵）
✗ 高压销售（强行推销、制造紧迫感）
✗ 违反政策（如帮助规避限购）
✗ 泄露客户隐私

【评价标准】
成功标志：客户了解房产真实情况，做出理性决策，愿意与你继续合作
```

#### 第2步：在系统提示生成中集成

修改 `src/main/java/com/agent/reasoning/prompt/SystemPromptBuilder.java`：

```java
// 添加行业提示词
private String getIndustryPrompt() {
    String industry = config.getProperty("agent.industry", "general");
    
    return switch (industry) {
        case "real_estate" -> loadPromptFromFile("prompts/real_estate_agent.txt");
        case "healthcare" -> loadPromptFromFile("prompts/healthcare_agent.txt");
        case "finance" -> loadPromptFromFile("prompts/finance_agent.txt");
        default -> "";
    };
}

private String loadPromptFromFile(String path) {
    try {
        Resource resource = resourceLoader.getResource("classpath:" + path);
        return new String(Files.readAllBytes(Paths.get(resource.getFile().toURI())));
    } catch (Exception e) {
        log.warn("Failed to load prompt file: {}", path);
        return "";
    }
}
```

#### 第3步：在配置文件中设置行业类型

编辑 `application.yml`：

```yaml
agent:
  industry: real_estate  # 改为你的行业：real_estate, healthcare, finance 等
  max-iterations: 10
  timeout: 300
  enable-streaming: false
```

---

## 方案4️⃣：数据集成

### 目标
将 Agent 连接到实际的行业系统（CRM、估价系统、政策库等），获取实时数据。

### 步骤

#### 第1步：创建 CRM 集成服务

创建 `src/main/java/com/agent/integration/CRMService.java`：

```java
package com.agent.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * CRM 系统集成
 * 用于客户管理、房源管理、交易记录查询
 */
@Slf4j
@Service
public class CRMService {
    
    /**
     * 查询客户历史记录
     */
    public String getClientHistory(String clientId) {
        // 模拟 API 调用 CRM 系统
        // 实际应调用：curl https://crm-api.company.com/clients/{clientId}/history
        
        return String.format(
            "客户 %s 的历史记录：\n" +
            "- 浏览房源：28套\n" +
            "- 看房记录：8套\n" +
            "- 感兴趣的地点：朝阳区、海淀区\n" +
            "- 预算范围：400-600万\n" +
            "- 最后互动：3天前（询问某套房源的税费）",
            clientId
        );
    }
    
    /**
     * 查询实时房源列表
     */
    public String getAvailableListings(String location, String type, double maxPrice) {
        // 模拟查询房源库
        return String.format(
            "在 %s 找到 5 套 %s 房源（预算≤%.0f万）：\n" +
            "1. 地址A，450万，朝向南北，新房装修\n" +
            "2. 地址B，520万，朝向东南，学区房\n" +
            "3. 地址C，580万，朝向南，投资潜力\n" +
            "4. 地址D，600万，朝向南，别墅社区\n" +
            "5. 地址E，630万，朝向北，高层视野",
            location, type, maxPrice
        );
    }
    
    /**
     * 记录客户互动
     */
    public boolean logInteraction(String clientId, String activityType, String content) {
        log.info("📝 记录客户互动 - 客户:{}, 类型:{}, 内容:{}", clientId, activityType, content);
        // 调用 CRM API 记录
        // POST https://crm-api.company.com/interactions
        return true;
    }
}
```

#### 第2步：在工具中使用 CRM 数据

```java
@Tool(
    name = "client_history",
    description = "查询客户历史记录和偏好"
)
public ToolResult getClientHistory(String clientId) {
    try {
        String history = crmService.getClientHistory(clientId);
        return ToolResult.success("client_history", history);
    } catch (Exception e) {
        return ToolResult.failure("client_history", "查询失败：" + e.getMessage());
    }
}
```

---

## 方案5️⃣：参数优化

### 目标
根据行业特点调整 LLM 参数，优化推理效果。

### 步骤

#### 第1步：创建行业参数配置

编辑 `application.yml`：

```yaml
llm:
  deepseek:
    api-key: ${DEEPSEEK_API_KEY}
    base-url: https://api.deepseek.com
    model: deepseek-chat
    max-tokens: 4096
    temperature: 0.5          # 房产销售需要准确性，降低随机性
    top-p: 0.8
    timeout: 120

agent:
  industry: real_estate
  max-iterations: 10          # 房产销售需要多轮对话
  timeout: 300
  
  # 行业特定参数
  knowledge:
    enabled: true
    top-k: 5                  # 房产销售需要多个知识片段
    min-similarity: 0.60
  
  # 对话历史管理
  conversation:
    max-history: 20           # 保留更长的对话历史
    context-window: 2000
```

#### 第2步：根据行业特点调整推理策略

创建 `src/main/java/com/agent/optimization/IndustryOptimization.java`：

```java
package com.agent.optimization;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 行业特定优化
 * 根据不同行业调整 Agent 的推理策略
 */
@Slf4j
@Service
public class IndustryOptimization {
    
    @Value("${agent.industry:general}")
    private String industry;
    
    /**
     * 获取行业特定的 temperature 值
     * 不同行业需要不同的创意性和准确性平衡
     */
    public double getIndustryTemperature() {
        return switch (industry) {
            case "real_estate" -> 0.5;  // 房产：需要准确和可信，低创意性
            case "healthcare" -> 0.3;   // 医疗：需要保守和合规，最低创意性
            case "finance" -> 0.4;      // 金融：需要准确，适度创意
            case "creative" -> 0.8;     // 创意：需要高创意性
            default -> 0.7;
        };
    }
    
    /**
     * 获取行业特定的最大迭代次数
     */
    public int getMaxIterations() {
        return switch (industry) {
            case "real_estate" -> 10;   // 房产销售需要多轮对话
            case "healthcare" -> 5;     // 医疗咨询要简洁
            case "customer_service" -> 15; // 客服需要多轮处理
            default -> 10;
        };
    }
    
    /**
     * 获取行业特定的知识库查询数量
     */
    public int getTopK() {
        return switch (industry) {
            case "real_estate" -> 5;    // 房产需要多个参考
            case "legal" -> 8;          // 法律需要全面参考
            default -> 3;
        };
    }
    
    /**
     * 是否需要强制遵循行业规范
     */
    public boolean shouldEnforceCompliance() {
        return switch (industry) {
            case "healthcare", "finance", "legal" -> true;
            default -> false;
        };
    }
}
```

---

## 🎯 完整示例：房产中介数字员工

### 假设场景

用户：\"我有500万预算，想在朝阳区买套2房2厅的房子自住，但怕被宰\"

### Agent 的工作流程

```
1️⃣ 理解需求
   ↓ 从消息中提取：预算500万、位置朝阳区、户型2房2厅、用途自住、关注价格合理性
   
2️⃣ 调用行业工具
   ↓ 使用 housing_estimate 估算市价
   ↓ 使用 client_matching 推荐房源
   ↓ 使用 policy_query 查询限购政策
   
3️⃣ 引用知识库
   ↓ 从\"房产交易流程\"知识库说明完整流程
   ↓ 从\"户型评估标准\"库介绍如何判断好户型
   ↓ 从\"异议处理\"库说明市价合理性
   
4️⃣ 生成建议
   ↓ \"根据我10年经验，您的预算和需求很合理...\"
   ↓ \"这个区域3个月的成交均价在X万，您的预算...\"
   ↓ \"建议我们先看2套房源，比对一下...\"
   
5️⃣ 专业话术
   ↓ 使用销售话术库中的经过验证的表达
   ↓ 展示数据而不仅仅是观点
   ↓ 主动处理客户忧虑
```

### 预期对话效果

```
【客户】：我有500万预算，想在朝阳区买套2房2厅的房子自住，但怕被宰

【李经理】：
您好！您的需求很清晰，我来帮您分析一下。

🏠 根据我们的数据：
- 朝阳区2房2厅的成交均价在480-550万之间
- 您的500万预算正好处于中等偏上的位置
- 选择好的位置和配套，是做出好决策的关键

📍 我现在给您推荐3套房源：
1. 建国路某小区 - 450万（比市价便宜5%，值得看）
2. 东三环某公馆 - 520万（学区房优势）  
3. 郎家园某社区 - 540万（投资潜力大）

🚀 接下来的建议：
第一步：这周末我们去看第1套和第3套，对比一下
第二步：评估这两套房产，找到最适合您的
第三步：如果满意，我会帮您理清购买流程和所有费用

💡 关于您的\"怕被宰\"顾虑：
- 我会基于市价帮您砍价（通常能砍3-5%）
- 看房时我会指出房产的真实优缺点
- 给您完整的费用清单，不会有隐藏费用

下周我们就能看房，如何？
```

---

## 📋 实施检查清单

- [ ] 1. 知识库
  - [ ] 创建行业知识文档
  - [ ] 导入知识库
  - [ ] 测试知识库查询

- [ ] 2. 工具扩展
  - [ ] 创建行业工具类
  - [ ] 验证工具注册
  - [ ] 测试工具调用

- [ ] 3. 提示词
  - [ ] 编写行业提示词
  - [ ] 配置行业类型
  - [ ] 验证提示词效果

- [ ] 4. 数据集成
  - [ ] 创建 CRM 集成
  - [ ] 配置 API 端点
  - [ ] 测试数据获取

- [ ] 5. 参数优化
  - [ ] 调整 temperature
  - [ ] 调整 max-iterations
  - [ ] 调整 top-k
  - [ ] 测试效果

---

## 🚀 快速启动（基于房产行业）

```bash
# 1. 导入行业知识库
./scripts/import_industry_knowledge.sh

# 2. 修改配置
vi src/main/resources/application.yml
# 设置 agent.industry: real_estate

# 3. 编译运行
mvn clean package -DskipTests
java -jar target/agent0228-1.0.0.jar

# 4. 测试
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "query": "我想在朝阳区买房，预算500万，帮我分析一下市场"
  }'
```

---

## 🔍 常见问题

**Q: 如何快速尝试另一个行业？**
A: 创建新的提示词文件、工具类、知识库，修改 `application.yml` 中的 `agent.industry` 即可。框架会自动加载。

**Q: 如何连接真实的 CRM 系统？**
A: 修改 `CRMService.java`，将模拟的 API 调用替换为真实的 HTTP 请求或 SDK 调用。

**Q: 如何评估定制的效果？**
A: 可以通过：
- 对话自然度（是否像行业专家）
- 工具调用频率（是否恰当使用工具）
- 知识库引用准确性
- 客户满意度反馈

**Q: 成本会增加吗？**
A: 主要成本来自 LLM API 调用。优化 temperature 和 max-iterations 可以降低成本。
```

这份指南包含了完整的实施路径。你可以选择其中任何一个行业，按步骤操作。要我帮你快速实现具体的行业定制吗？比如医疗咨询、财务顾问、法律助手等？