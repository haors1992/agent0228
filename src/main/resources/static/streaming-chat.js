/**
 * 流式响应处理器
 * 支持 Server-Sent Events (SSE) 实时数据接收
 */

class StreamingChat {
    constructor(containerId = 'messages') {
        this.container = document.getElementById(containerId);
        this.currentSession = null;
    }

    /**
     * 开始流式聊天
     * @param {string} message 用户消息
     * @param {string} sessionId 可选的会话 ID
     */
    async startStreamingChat(message, sessionId = null) {
        if (!message.trim()) {
            alert('请输入消息');
            return;
        }

        console.log('🚀 [DEBUG] 开始流式聊天');
        console.log('🚀 [DEBUG] 消息:', message);

        // 显示用户消息
        this.appendMessage('user', message);

        // 创建 AI 消息容器
        const aiMessageId = 'ai-message-' + Date.now();
        const contentElement = this.appendMessage('assistant', '', aiMessageId, true);

        try {
            console.log('🚀 [DEBUG] 发起 fetch 请求到 /api/agent/chat/stream');
            
            // 创建流式请求
            const response = await fetch('/api/agent/chat/stream', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    query: message,
                    sessionId: sessionId || null,
                    includeDetails: false
                })
            });

            console.log('🚀 [DEBUG] 收到响应，状态码:', response.status, '内容类型:', response.headers.get('content-type'));

            if (!response.ok) {
                throw new Error('流式请求失败: ' + response.statusText);
            }

            // 处理 SSE 流 - 标准 SSE 事件格式解析
            console.log('🚀 [DEBUG] 开始读取流...');
            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = ''; // 缓冲不完整的行
            let eventBuffer = {}; // 缓冲事件属性
            let chunkCount = 0;

            while (true) {
                const { done, value } = await reader.read();
                if (done) {
                    console.log('🚀 [DEBUG] 流读取完成');
                    break;
                }

                chunkCount++;
                const chunk = decoder.decode(value, { stream: true });
                console.log(`🚀 [DEBUG] 收到数据块 #${chunkCount}，长度: ${value.length} 字节`);
                console.log(`🚀 [DEBUG] 原始数据:`, chunk);

                buffer += chunk;
                const lines = buffer.split('\n');
                
                console.log(`🚀 [DEBUG] 分割后行数: ${lines.length}`);
                
                // 保留最后一个不完整的行到下一次读取
                buffer = lines[lines.length - 1];

                for (let i = 0; i < lines.length - 1; i++) {
                    const line = lines[i].trim();
                    console.log(`🚀 [DEBUG] 处理行 #${i}: "${line}"`);
                    
                    // 空行表示事件结束
                    if (line === '') {
                        console.log('🚀 [DEBUG] 发现空行，事件完成，eventBuffer:', eventBuffer);
                        if (Object.keys(eventBuffer).length > 0) {
                            this.processSSEEvent(eventBuffer, contentElement);
                        }
                        eventBuffer = {};
                        continue;
                    }

                    // 解析事件属性（标准 SSE 格式：键: 值）
                    if (line.startsWith('event:') || line.startsWith('event: ')) {
                        // 匹配 "event:" 或 "event: " 两种情况
                        eventBuffer.eventType = line.substring(line.indexOf(':') + 1).trim();
                        console.log('🚀 [DEBUG] 事件类型:', eventBuffer.eventType);
                    } else if (line.startsWith('data:') || line.startsWith('data: ')) {
                        // 匹配 "data:" 或 "data: " 两种情况
                        const data = line.substring(line.indexOf(':') + 1).trim();
                        eventBuffer.data = data;
                        console.log('🚀 [DEBUG] 数据:', data);
                    } else if (line.startsWith('id:') || line.startsWith('id: ')) {
                        // 匹配 "id:" 或 "id: " 两种情况
                        eventBuffer.id = line.substring(line.indexOf(':') + 1).trim();
                        console.log('🚀 [DEBUG] ID:', eventBuffer.id);
                    } else {
                        // 忽略其他字段（如 retry）
                        console.log('🚀 [DEBUG] 忽略行:', line);
                    }
                }
            }

            // 处理最后剩余的缓冲
            if (buffer.trim() && Object.keys(eventBuffer).length > 0) {
                console.log('🚀 [DEBUG] 处理剩余缓冲:', buffer);
                this.processSSEEvent(eventBuffer, contentElement);
            }

            console.log('✅ 流式聊天完成');

        } catch (error) {
            console.error('❌ 流式聊天错误:', error);
            if (contentElement) {
                contentElement.textContent = '❌ 错误: ' + error.message;
                contentElement.style.color = '#f44336';
            }
        }
    }

    /**
     * 处理单个 SSE 事件
     */
    processSSEEvent(eventBuffer, contentElement) {
        console.log('🚀 [DEBUG] 处理 SSE 事件:', eventBuffer);
        try {
            const eventType = eventBuffer.eventType || 'message';
            const dataStr = eventBuffer.data || '';

            console.log(`🚀 [DEBUG] eventType = ${eventType}, dataStr 长度 = ${dataStr.length}`);

            if (!dataStr) {
                console.log('🚀 [DEBUG] 数据为空，跳过');
                return;
            }

            let eventObj = null;

            // 尝试解析为 JSON，如果失败则作为普通文本
            try {
                eventObj = JSON.parse(dataStr);
                eventObj.type = eventType;
                console.log('🚀 [DEBUG] 成功解析为 JSON:', eventObj);
            } catch (e) {
                // 不是 JSON，作为文本消息处理
                eventObj = {
                    type: 'message',
                    data: dataStr
                };
                console.log('🚀 [DEBUG] 作为纯文本处理:', eventObj);
            }

            console.log(`📨 收到事件: ${eventType}`, eventObj);
            this.handleStreamEvent(eventObj, contentElement);

        } catch (error) {
            console.error('❌ 处理事件失败:', error);
        }
    }

    /**
     * 处理流式事件
     * @param {object} event SSE 事件对象
     * @param {element} container 消息容器
     */
    handleStreamEvent(event, container) {
        console.log(`🚀 [DEBUG] handleStreamEvent 调用，event.type = ${event.type}`);
        const eventType = event.type || 'message';
        
        switch (eventType) {
            case 'message':
                console.log('🚀 [DEBUG] 处理 message 事件');
                if (container) {
                    // 对于文本消息，data 是字符串内容
                    if (typeof event.data === 'string') {
                        console.log('🚀 [DEBUG] 追加文本到 container');
                        container.textContent += event.data;
                    } else if (event.data === undefined && event.content) {
                        // 某些情况下数据可能在 content 属性
                        console.log('🚀 [DEBUG] 从 content 属性追加文本');
                        container.textContent += event.content;
                    }
                    // 自动滚动到底部
                    if (container.parentElement) {
                        container.parentElement.scrollTop = container.parentElement.scrollHeight;
                    }
                }
                break;

            case 'step':
                console.log(`🚀 [DEBUG] 处理 step 事件: ${event.step}`);
                if (container && container.parentElement) {
                    const stepInfo = document.createElement('div');
                    stepInfo.className = 'stream-step';
                    stepInfo.style.padding = '8px';
                    stepInfo.style.marginTop = '8px';
                    stepInfo.style.backgroundColor = '#e8f5e9';
                    stepInfo.style.borderLeft = '3px solid #4caf50';
                    stepInfo.innerHTML = `<strong>🔍 ${event.step}:</strong> ${event.content}`;
                    container.parentElement.appendChild(stepInfo);
                }
                break;

            case 'search_result':
                console.log(`🚀 [DEBUG] 处理 search_result 事件: ${event.title}`);
                if (container && container.parentElement) {
                    const resultDiv = document.createElement('div');
                    resultDiv.className = 'search-result-badge';
                    resultDiv.style.padding = '8px';
                    resultDiv.style.marginTop = '8px';
                    resultDiv.style.backgroundColor = '#fff3e0';
                    resultDiv.style.borderLeft = '3px solid #ff9800';
                    resultDiv.innerHTML = `<strong>📚 ${event.title}</strong> (相似度: ${(event.similarity * 100).toFixed(1)}%)`;
                    container.parentElement.appendChild(resultDiv);
                }
                break;

            case 'complete':
                console.log('🚀 [DEBUG] 处理 complete 事件');
                if (event.sessionId) {
                    this.currentSession = event.sessionId;
                }
                if (container && container.parentElement) {
                    const statsDiv = document.createElement('div');
                    statsDiv.className = 'completion-stats';
                    statsDiv.style.padding = '8px';
                    statsDiv.style.marginTop = '8px';
                    statsDiv.style.backgroundColor = '#f3e5f5';
                    statsDiv.style.borderLeft = '3px solid #9c27b0';
                    statsDiv.style.fontSize = '12px';
                    statsDiv.innerHTML = `
                        ✅ 完成 | ⏱️ ${event.duration_ms}ms | 🔄 ${event.iterations} 次 | 📝 ${event.messageCount} 条消息
                    `;
                    container.parentElement.appendChild(statsDiv);
                }
                break;

            case 'error':
                console.error('🚀 [DEBUG] 处理 error 事件:', event.error || event.data);
                if (container) {
                    const errorMsg = typeof event.data === 'string' ? event.data : (event.error || '未知错误');
                    container.textContent = '❌ 错误: ' + errorMsg;
                    container.style.color = '#f44336';
                }
                break;

            default:
                console.warn('🚀 [DEBUG] 未知事件类型:', eventType, event);
                if (container && typeof event.data === 'string') {
                    container.textContent += event.data;
                }
        }
    }

    /**
     * 添加消息到容器
     * @param {string} role 角色 ('user' 或 'assistant')
     * @param {string} content 消息内容
     * @param {string} id 可选的元素 ID
     * @param {boolean} isStreaming 是否为流式消息
     */
    appendMessage(role, content, id = null, isStreaming = false) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `message message-${role}`;
        
        if (id) {
            messageDiv.id = id;
        }

        const contentDiv = document.createElement('div');
        contentDiv.className = 'message-content';
        contentDiv.textContent = content || (isStreaming ? '🤔 正在思考...' : '');
        
        // 保存对 contentDiv 的引用，以便后期更新
        if (id) {
            contentDiv.id = `${id}-content`;
        }

        const avatar = document.createElement('div');
        avatar.className = 'message-avatar';
        avatar.textContent = role === 'user' ? '👤' : '🤖';

        messageDiv.appendChild(avatar);
        messageDiv.appendChild(contentDiv);
        
        this.container.appendChild(messageDiv);
        this.container.scrollTop = this.container.scrollHeight;

        return contentDiv;
    }

    /**
     * 获取当前会话 ID
     */
    getSessionId() {
        return this.currentSession;
    }

    /**
     * 清空消息
     */
    clearMessages() {
        this.container.innerHTML = '';
        this.currentSession = null;
    }
}

// 导出供全局使用
window.StreamingChat = StreamingChat;
