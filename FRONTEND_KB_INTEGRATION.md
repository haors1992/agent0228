# 知识库前端集成指南

## 📱 概述

本指南说明如何在前端中集成向量数据库知识库管理界面。

## 🎨 前端功能需求

### 1. 知识库管理面板

在现有的 HTML 界面中添加知识库管理功能:

```html
<!-- 在 index.html 中添加标签页 -->
<div class="tabs">
  <button class="tab-button active" data-tab="chat">💬 对话</button>
  <button class="tab-button" data-tab="knowledge">📚 知识库</button>
  <button class="tab-button" data-tab="history">📜 历史</button>
</div>

<div id="chat-tab" class="tab-content active">
  <!-- 现有的对话面板 -->
</div>

<div id="knowledge-tab" class="tab-content hidden">
  <!-- 新增的知识库面板 -->
</div>
```

### 2. 知识库管理界面

建议包含以下功能:

#### A. 添加文档表单
```html
<div class="kb-upload-section">
  <h3>添加文档</h3>
  <form id="addDocForm">
    <input type="text" id="docTitle" placeholder="文档标题" required>
    <textarea id="docContent" placeholder="文档内容" rows="6" required></textarea>
    
    <div class="form-row">
      <select id="docCategory">
        <option value="">选择类别</option>
        <option value="编程">编程</option>
        <option value="数据">数据科学</option>
        <option value="web">Web 开发</option>
        <option value="其他">其他</option>
      </select>
      
      <input type="text" id="docSource" placeholder="来源">
    </div>
    
    <button type="submit" class="btn-primary">添加文档</button>
  </form>
</div>
```

#### B. 文档列表
```html
<div class="kb-list-section">
  <h3>知识库文档</h3>
  <div id="documentList" class="document-list">
    <!-- 动态加载的文档列表 -->
  </div>
</div>
```

#### C. 搜索面板
```html
<div class="kb-search-section">
  <h3>语义搜索</h3>
  <div class="search-box">
    <input type="text" id="searchQuery" placeholder="输入查询(如:Python编程)">
    <input type="number" id="topK" value="5" min="1" max="20">
    <button onclick="semanticSearch()" class="btn-secondary">搜索</button>
  </div>
  
  <div id="searchResults" class="search-results hidden">
    <!-- 搜索结果显示 -->
  </div>
</div>
```

### 3. JavaScript 实现

#### 3.1 添加文档
```javascript
async function addDocument() {
  const form = document.getElementById('addDocForm');
  const formData = new FormData(form);
  
  const requestBody = {
    title: document.getElementById('docTitle').value,
    content: document.getElementById('docContent').value,
    category: document.getElementById('docCategory').value || 'general',
    source: document.getElementById('docSource').value
  };
  
  try {
    const response = await fetch('/api/knowledge/documents', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(requestBody)
    });
    
    if (response.ok) {
      const result = await response.json();
      console.log('✅ 文档已添加:', result.docId);
      form.reset();
      loadDocuments();  // 刷新文档列表
    }
  } catch (error) {
    console.error('❌ 添加文档失败:', error);
  }
}
```

#### 3.2 加载文档列表
```javascript
async function loadDocuments() {
  try {
    const response = await fetch('/api/knowledge/documents');
    const data = await response.json();
    
    const listDiv = document.getElementById('documentList');
    listDiv.innerHTML = '';
    
    data.documents.forEach(doc => {
      const docElement = document.createElement('div');
      docElement.className = 'document-item';
      docElement.innerHTML = `
        <div class="doc-header">
          <h4>${doc.title}</h4>
          <span class="category">${doc.category}</span>
        </div>
        <p>${doc.summary}</p>
        <div class="doc-actions">
          <button onclick="deleteDocument('${doc.docId}')">删除</button>
        </div>
      `;
      listDiv.appendChild(docElement);
    });
  } catch (error) {
    console.error('❌ 加载文档失败:', error);
  }
}
```

#### 3.3 语义搜索
```javascript
async function semanticSearch() {
  const query = document.getElementById('searchQuery').value;
  if (!query) return;
  
  const topK = document.getElementById('topK').value;
  
  try {
    const response = await fetch(
      `/api/knowledge/search?query=${encodeURIComponent(query)}&topK=${topK}`
    );
    const data = await response.json();
    
    const resultsDiv = document.getElementById('searchResults');
    resultsDiv.classList.remove('hidden');
    resultsDiv.innerHTML = `<h4>搜索: "${query}"</h4>`;
    
    data.results.forEach((result, index) => {
      const similarity = (result.similarity * 100).toFixed(1);
      const resultElement = document.createElement('div');
      resultElement.className = 'search-result';
      resultElement.innerHTML = `
        <div class="result-rank">#${index + 1}</div>
        <div class="result-content">
          <h5>${result.title}</h5>
          <p>${result.summary}</p>
          <div class="result-meta">
            <span class="similarity">相似度: ${similarity}%</span>
            <span class="category">${result.category}</span>
          </div>
        </div>
      `;
      resultsDiv.appendChild(resultElement);
    });
  } catch (error) {
    console.error('❌ 搜索失败:', error);
  }
}
```

#### 3.4 删除文档
```javascript
async function deleteDocument(docId) {
  if (!confirm('确认删除此文档?')) return;
  
  try {
    const response = await fetch(`/api/knowledge/documents/${docId}`, {
      method: 'DELETE'
    });
    
    if (response.ok) {
      console.log('✅ 文档已删除');
      loadDocuments();  // 刷新列表
    }
  } catch (error) {
    console.error('❌ 删除失败:', error);
  }
}
```

#### 3.5 获取统计信息
```javascript
async function loadKnowledgeBaseStats() {
  try {
    const response = await fetch('/api/knowledge/stats');
    const data = response.json();
    
    const statsDiv = document.getElementById('kbStats');
    statsDiv.innerHTML = `
      📊 文档数: ${data.stats.totalDocuments} | 
      📝 字符数: ${data.stats.totalCharacters} | 
      🏷️ 分类: ${data.stats.categories}
    `;
  } catch (error) {
    console.error('❌ 获取统计失败:', error);
  }
}
```

## 🎨 CSS 样式建议

```css
/* 知识库面板样式 */
#knowledge-tab {
  padding: 20px;
  background: #f5f5f5;
}

.kb-upload-section {
  background: white;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.kb-upload-section form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.kb-upload-section input,
.kb-upload-section textarea,
.kb-upload-section select {
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-row {
  display: flex;
  gap: 10px;
}

.form-row input,
.form-row select {
  flex: 1;
}

.kb-list-section {
  background: white;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.document-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 400px;
  overflow-y: auto;
}

.document-item {
  border: 1px solid #ddd;
  padding: 12px;
  border-radius: 4px;
  background: #fafafa;
}

.doc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.category {
  display: inline-block;
  background: #e8f5e9;
  color: #2e7d32;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
}

.doc-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.doc-actions button {
  padding: 6px 12px;
  background: #ff5252;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.kb-search-section {
  background: white;
  padding: 20px;
  border-radius: 8px;
}

.search-box {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.search-box input {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.search-box input[type="number"] {
  width: 80px;
}

.search-results {
  max-height: 500px;
  overflow-y: auto;
}

.search-result {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-left: 4px solid #2196f3;
  background: #f5f5f5;
  margin-bottom: 10px;
  border-radius: 4px;
}

.result-rank {
  font-weight: bold;
  color: #2196f3;
  min-width: 40px;
}

.result-content {
  flex: 1;
}

.result-content h5 {
  margin: 0 0 5px 0;
  color: #333;
}

.result-content p {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #666;
}

.result-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
}

.similarity {
  color: #2e7d32;
  font-weight: bold;
}

.hidden {
  display: none;
}
```

## 🔗 集成到现有对话

在发送消息时，前端可以显示相关知识库内容:

```javascript
async function sendMessage() {
  const message = document.getElementById('messageInput').value;
  
  try {
    // 后端会自动使用知识库
    const response = await fetch('/api/agent/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        query: message,
        sessionId: currentSessionId
      })
    });
    
    const data = await response.json();
    
    // 显示 AI 回复
    displayMessage('assistant', data.result);
    
    // 可选: 显示使用的知识库来源
    if (data.knowledgeSources) {
      console.log('📚 使用的知识库source:', data.knowledgeSources);
    }
  } catch (error) {
    console.error('❌ 发送失败:', error);
  }
}
```

## 📋 实现检查清单

- [ ] 完成 HTML 知识库面板结构
- [ ] 添加选项卡切换功能
- [ ] 实现添加文档表单
- [ ] 实现文档列表加载
- [ ] 实现语义搜索功能
- [ ] 实现删除文档功能
- [ ] 添加 CSS 样式
- [ ] 测试所有功能
- [ ] 优化用户体验
- [ ] 添加加载指示器
- [ ] 添加错误提示
- [ ] 响应式设计调整

## 🚀 部署说明

1. **修改 `index.html`** - 添加知识库面板和标签页
2. **添加新 JavaScript** - 或整合到现有的 `script` 标签中
3. **更新 CSS** - 添加知识库相关样式
4. **测试** - 确保所有 API 调用正常

## 📞 API 快速参考

| 操作 | 端点 | 方法 |
|------|------|------|
| 添加文档 | `/api/knowledge/documents` | POST |
| 获取文档列表 | `/api/knowledge/documents` | GET |
| 删除文档 | `/api/knowledge/documents/{id}` | DELETE |
| 语义搜索 | `/api/knowledge/search` | GET |
| 获取统计 | `/api/knowledge/stats` | GET |

---

**建议**: 优先实现搜索功能，这是用户最常用的功能。添加文档的 UI 可以后期完善。
