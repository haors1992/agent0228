package com.agent.streaming;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 流式响应处理器
 * 支持 Server-Sent Events (SSE) 实时流式传输
 */
@Slf4j
public class StreamingResponseHandler {

    private final SseEmitter emitter;
    private final ExecutorService executorService;
    private volatile boolean cancelled = false;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public StreamingResponseHandler(SseEmitter emitter) {
        this.emitter = emitter;
        this.executorService = Executors.newSingleThreadExecutor();

        // 设置超时时间为 5 分钟
        this.emitter.onTimeout(() -> {
            log.info("🔌 SSE 连接超时");
            this.cancelled = true;
        });

        this.emitter.onCompletion(() -> {
            log.info("✅ SSE 连接完成");
            this.executorService.shutdown();
        });

        this.emitter.onError(throwable -> {
            log.error("❌ SSE 错误: {}", throwable.getMessage());
            this.cancelled = true;
        });
    }

    /**
     * 手工构建标准 SSE 消息
     */
    private void sendSSEMessage(String eventType, String data) throws IOException {
        if (cancelled)
            return;

        // 构建标准 SSE 格式
        StringBuilder message = new StringBuilder();
        message.append("id: ").append(System.currentTimeMillis()).append("\n");
        message.append("event: ").append(eventType).append("\n");
        message.append("data: ").append(data).append("\n");
        message.append("\n");

        log.debug("📤 发送 SSE 消息: {}", message.toString().replace("\n", "\\n"));
        emitter.send(message.toString());
    }

    /**
     * 发送文字块（流式内容）
     */
    public void sendChunk(String content) {
        if (cancelled)
            return;

        try {
            sendSSEMessage("message", content);
            log.debug("📤 发送数据块: {}", content.substring(0, Math.min(50, content.length())));
        } catch (IOException e) {
            log.error("❌ 发送数据块失败: {}", e.getMessage());
            this.cancelled = true;
        }
    }

    /**
     * 发送执行步骤
     */
    public void sendStep(String stepName, String stepContent) {
        if (cancelled)
            return;

        try {
            Map<String, String> stepData = new HashMap<>();
            stepData.put("step", stepName);
            stepData.put("content", stepContent);
            String jsonData = objectMapper.writeValueAsString(stepData);

            sendSSEMessage("step", jsonData);
            log.debug("🔍 发送步骤: {} - {}", stepName, stepContent.substring(0, Math.min(50, stepContent.length())));
        } catch (Exception e) {
            log.error("❌ 发送步骤失败: {}", e.getMessage());
            this.cancelled = true;
        }
    }

    /**
     * 发送搜索结果
     */
    public void sendSearchResult(String docId, String title, double similarity) {
        if (cancelled)
            return;

        try {
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("docId", docId);
            resultData.put("title", title);
            resultData.put("similarity", similarity);
            String jsonData = objectMapper.writeValueAsString(resultData);

            sendSSEMessage("search_result", jsonData);
            log.debug("📚 发送搜索结果: {}", title);
        } catch (Exception e) {
            log.error("❌ 发送搜索结果失败: {}", e.getMessage());
            this.cancelled = true;
        }
    }

    /**
     * 发送完成标记
     */
    public void sendComplete(Map<String, Object> finalResult) {
        if (cancelled)
            return;

        try {
            String jsonData = objectMapper.writeValueAsString(finalResult);
            sendSSEMessage("complete", jsonData);
            log.info("✅ 流式传输完成");
        } catch (Exception e) {
            log.error("❌ 发送完成标记失败: {}", e.getMessage());
            this.cancelled = true;
        }
    }

    /**
     * 发送错误信息
     */
    public void sendError(String errorMessage) {
        if (cancelled)
            return;

        try {
            Map<String, String> errorData = new HashMap<>();
            errorData.put("error", errorMessage);
            errorData.put("timestamp", String.valueOf(System.currentTimeMillis()));
            String jsonData = objectMapper.writeValueAsString(errorData);

            sendSSEMessage("error", jsonData);
            log.error("❌ 发送错误: {}", errorMessage);
        } catch (Exception e) {
            log.error("❌ 发送错误信息失败: {}", e.getMessage());
            this.cancelled = true;
        }
    }

    /**
     * 在后台线程中执行操作
     */
    public void executeAsync(Runnable task) {
        executorService.submit(task);
    }

    /**
     * 检查连接是否仍然开放
     */
    public boolean isActive() {
        return !cancelled;
    }

    /**
     * 关闭连接
     * 
     * @throws IOException 如果关闭失败
     */
    public void close() throws IOException {
        this.cancelled = true;
        try {
            this.emitter.complete();
        } finally {
            this.executorService.shutdown();
        }
    }
}
