#!/bin/bash

# 知识库测试脚本
# 测试语义搜索和文档管理功能

BASE_URL="http://localhost:8080/api/knowledge"
echo "🧪 知识库系统测试"
echo "================================================"

# 1. 获取初始统计
echo -e "\n📊 [1] 获取初始统计信息..."
curl -s "${BASE_URL}/stats" | jq .

# 2. 添加示例文档 - 编程
echo -e "\n\n📝 [2] 添加第一个文档：Python 编程基础..."
curl -s -X POST "${BASE_URL}/documents" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Python 编程基础",
    "content": "Python 是一种高级编程语言，具有简洁的语法和强大的功能。它支持面向对象、函数式编程等多种编程范型。Python 的标准库非常丰富，包含了许多实用的模块。",
    "category": "编程",
    "source": "官方文档",
    "metadata": "python,tutorial"
  }' | jq .

# 3. 添加第二个文档 - 前端
echo -e "\n\n📝 [3] 添加第二个文档：JavaScript 前端开发..."
curl -s -X POST "${BASE_URL}/documents" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "JavaScript 前端开发",
    "content": "JavaScript 是 Web 浏览器的标准编程语言。现代 JavaScript 框架如 React、Vue、Angular 等使得前端开发变得更加高效。异步编程、Promise 和 async/await 是 JavaScript 的重要特性。",
    "category": "编程",
    "source": "MDN 文档",
    "metadata": "javascript,frontend"
  }' | jq .

# 4. 添加第三个文档 - 数据科学
echo -e "\n\n📝 [4] 添加第三个文档：数据科学基础..."
curl -s -X POST "${BASE_URL}/documents" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "数据科学基础",
    "content": "数据科学结合了统计学、数学和计算机科学。机器学习是数据科学的核心，包括监督学习和无监督学习。数据可视化和数据探索对于理解数据很关键。",
    "category": "数据",
    "source": "科学期刊",
    "metadata": "data-science,ml"
  }' | jq .

# 5. 获取所有文档
echo -e "\n\n📚 [5] 获取所有已添加的文档..."
curl -s "${BASE_URL}/documents" | jq .

# 6. 语义搜索 - 查询 "编程"
echo -e "\n\n🔍 [6] 语义搜索：'编程语言'..."
curl -s "${BASE_URL}/search?query=编程语言&topK=3" | jq .

# 7. 语义搜索 - 查询 "前端"
echo -e "\n\n🔍 [7] 语义搜索：'Web 开发'..."
curl -s "${BASE_URL}/search?query=Web%20开发&topK=3" | jq .

# 8. 语义搜索 - 查询 "机器学习"
echo -e "\n\n🔍 [8] 语义搜索：'机器学习'..."
curl -s "${BASE_URL}/search?query=机器学习&topK=3" | jq .

# 9. 关键字搜索
echo -e "\n\n🔎 [9] 关键字搜索：'标准库'..."
curl -s "${BASE_URL}/search/keyword?keyword=标准库" | jq .

# 10. 按类别过滤文档
echo -e "\n\n📂 [10] 按类别过滤：编程类..."
curl -s "${BASE_URL}/documents?category=编程" | jq .

# 11. 获取最终统计
echo -e "\n\n📊 [11] 获取最终统计信息..."
curl -s "${BASE_URL}/stats" | jq .

echo -e "\n\n================================================"
echo "✅ 知识库测试完成！"
echo "================================================"
