# 🚀 向量数据库 - 快速参考

## 📍 核心组件位置

```
src/main/java/com/agent/
├── knowledge/
│   ├── model/
│   │   ├── Document.java          ← 文档模型
│   │   └── TextVector.java        ← 向量模型
│   └── service/
│       ├── EmbeddingService.java  ← 文本向量化
│       └── KnowledgeBaseManager.java  ← 核心管理器
├── controller/
│   └── KnowledgeBaseController.java  ← REST API
└── config/
    └── KnowledgeBaseConfig.java    ← Spring 初始化
```

## 🔌 API 端点列表

| 方法 | 端点 | 功能 |
|------|------|------|
| POST | `/api/knowledge/documents` | 添加文档 |
| GET | `/api/knowledge/documents` | 列出所有文档 |
| GET | `/api/knowledge/documents/{id}` | 获取单个文档 |
| DELETE | `/api/knowledge/documents/{id}` | 删除文档 |
| **GET** | **`/api/knowledge/search`** | **🔑 语义搜索** |
| GET | `/api/knowledge/search/keyword` | 关键字搜索 |
| DELETE | `/api/knowledge/clear` | 清空知识库 |
| GET | `/api/knowledge/stats` | 统计信息 |

## 💾 数据存储

**位置**: `./data/knowledge/`  
**格式**: JSON 文件，一个文件一个文档  
**文件名**: `{docId}.json`

## 🔧 配置

```yaml
# application.yml
agent:
  knowledge:
    enabled: true              # 启用知识库
    storage-path: ./data/knowledge
    top-k: 3                  # 返回前 3 结果
    embedding-model: local    # 本地嵌入
```

## 📊 快速命令

### 添加文档
```bash
curl -X POST http://localhost:8080/api/knowledge/documents \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Python 基础",
    "content": "Python 是...",
    "category": "编程",
    "source": "官方文档"
  }'
```

### 语义搜索
```bash
# 搜索相关文档
curl "http://localhost:8080/api/knowledge/search" \
  --data-urlencode "query=Python编程" \
  --data-urlencode "topK=5" \
  -G
```

### 获取统计
```bash
curl http://localhost:8080/api/knowledge/stats
```

### 列出文档
```bash
# 所有文档
curl http://localhost:8080/api/knowledge/documents

# 特定类别
curl "http://localhost:8080/api/knowledge/documents?category=编程"
```

## 🎯 工作原理

```
用户查询 → 向量化 → 相似度搜索 → 返回 top-K → 添加到提示 → 强化 AI
```

1. **向量化**: TextVector 100 维
2. **相似度**: 余弦相似度 [-1, 1]
3. **排序**: 降序 (最相关优先)
4. **集成**: 自动添加到 LLM 提示

## 📈 向量维度

- **行数**: 100 维向量
- **速度**: < 1ms 嵌入单个文档
- **存储**: ~50KB per 10 documents

## ✨ 关键特性

- ✅ 文本向量化
- ✅ 语义搜索
- ✅ 持久化存储
- ✅ 内存索引
- ✅ 自动上下文注入
- ✅ RESTful API

## 🧪 测试

```bash
# 运行完整测试
./test_knowledge_base.sh

# 运行集成测试
./test_kb_integration.sh
```

## 📚 文档

| 文档 | 内容 |
|------|------|
| `KNOWLEDGE_BASE.md` | 完整技术文档 |
| `FRONTEND_KB_INTEGRATION.md` | 前端集成指南 |
| `VECTOR_DB_SUMMARY.md` | 实现总结 |
| `IMPLEMENTATION_REPORT.md` | 最终报告 |

## 🎓 代码示例

### Java 使用

```java
// 注入知识库管理器
@Autowired
private KnowledgeBaseManager knowledgeBaseManager;

// 添加文档
Document doc = Document.create("标题", "内容", "类别");
knowledgeBaseManager.addDocument(doc);

// 语义搜索
List<SearchResult> results = knowledgeBaseManager
    .semanticSearch("查询文本", 5);

// 关键字搜索
List<Document> docs = knowledgeBaseManager
    .keywordSearch("关键词");

// 统计
KnowledgeBaseStats stats = knowledgeBaseManager.getStats();
```

### REST 使用

```javascript
// JavaScript/Fetch

// 添加文档
async function addDoc(title, content, category) {
  const response = await fetch('/api/knowledge/documents', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title, content, category })
  });
  return response.json();
}

// 语义搜索
async function search(query, topK = 5) {
  const response = await fetch(
    `/api/knowledge/search?query=${query}&topK=${topK}`
  );
  return response.json();
}
```

## 🔄 生命周期

```
应用启动
    ↓
KnowledgeBaseConfig 触发
    ↓
KnowledgeBaseManager.init()
    ↓
加载 ./data/knowledge/ 中的所有文档
    ↓
构建内存索引
    ↓
准备好开始使用
```

## ⚡ 性能

| 操作 | 延迟 |
|------|------|
| 添加文档 | < 10ms |
| 语义搜索 | < 100ms |
| 关键字搜索 | < 50ms |
| 获取文档 | < 5ms |

## 🚨 故障排查

### Q: 搜索返回空结果?
A: 确保知识库有文档 → 检查 `/api/knowledge/stats`

### Q: 速度很慢?
A: 检查文档数量 → 考虑优化或换大容量方案

### Q: 文档没有持久化?
A: 检查 `./data/knowledge/` 权限

### Q: 编译错误?
A: 确保 Java 8+ 和 Maven 3.6+

## 🎁 额外功能

- [x] 文档分类
- [x] 批量操作准备
- [x] 统计信息
- [x] 清空知识库
- [x] 导出搜索结果
- [ ] 前端 UI (进行中)
- [ ] 向量缓存 (计划中)
- [ ] FAISS 优化 (计划中)

## 🏆 用途

- 📖 产品文档库
- 📋 常见问题库
- 🔬 研究论文库
- 💡 创意集合
- 📚 教学材料库
- 🎯 最佳实践库

---

**最后更新**: 2026-03-01  
**版本**: 1.0  
**状态**: ✅ 生产可用
