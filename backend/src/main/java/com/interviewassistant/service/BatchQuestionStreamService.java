package com.interviewassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewassistant.common.AiErrorUtils;
import com.interviewassistant.common.SseUtils;
import com.interviewassistant.dto.interview.*;
import com.interviewassistant.dto.interview.BatchSseEvents.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 负责批量出题的 SSE 流编排逻辑。
 * 从 InterviewController 中提取，使 Controller 只负责创建 SseEmitter 和启动任务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchQuestionStreamService {

    private static final int MAX_CONSECUTIVE_BATCH_FAILURES = 3;
    private static final long BATCH_INTERVAL_MS = 400L;

    private final InterviewAiService interviewService;
    private final ObjectMapper objectMapper;

    /**
     * 在当前线程中执行批量出题流程，向 emitter 推送 SSE 事件。
     * 调用方应在异步线程池中调用此方法。
     */
    public void executeBatchStream(SseEmitter emitter, AtomicBoolean closed,
                                    InterviewDirection direction, InterviewLevel level, int total) {
        int batchSize = interviewService.batchSize();
        int batches = (total + batchSize - 1) / batchSize;
        if (!sendEvent(emitter, "progress",
                new BatchProgress("批量出题已开始", total, batchSize, batches, 0, 0))) {
            closed.set(true);
            return;
        }

        int emitted = 0;
        int consecutiveFailures = 0;
        Exception lastError = null;
        List<Integer> failedBatches = new ArrayList<>();

        for (int batch = 0; batch < batches; batch++) {
            if (closed.get()) {
                return;
            }
            try {
                int batchNumber = batch + 1;
                int startIndex = batch * batchSize + 1;
                int batchCount = Math.min(batchSize, total - batch * batchSize);

                if (!sendEvent(emitter, "batch_start",
                        new BatchStart(batchNumber, batches, startIndex, batchCount, total, emitted))) {
                    closed.set(true);
                    return;
                }

                List<BatchQuestionItem> chunk = interviewService.generateBatchQuestionChunk(
                        direction, level, batchCount, batchNumber, startIndex);

                if (chunk.isEmpty()) {
                    consecutiveFailures++;
                    failedBatches.add(batchNumber);
                    if (!sendEvent(emitter, "batch_error",
                            new BatchError(batchNumber, "第 " + batchNumber + " 批没有生成有效题目", emitted, total))) {
                        closed.set(true);
                        return;
                    }
                    if (consecutiveFailures >= MAX_CONSECUTIVE_BATCH_FAILURES) {
                        SseUtils.sendError(emitter, "BATCH_GENERATION_FAILED", "连续多批生成失败，请稍后重试或减少题目数量");
                        return;
                    }
                    pauseBetweenBatches(batch, batches);
                    continue;
                }

                emitted += chunk.size();
                consecutiveFailures = 0;
                if (!sendEvent(emitter, "batch", new BatchPayload(batchNumber, chunk, emitted, total))) {
                    closed.set(true);
                    return;
                }
                pauseBetweenBatches(batch, batches);

            } catch (Exception e) {
                lastError = e;
                consecutiveFailures++;
                int batchNumber = batch + 1;
                failedBatches.add(batchNumber);
                log.warn("Batch question chunk failed: {}", AiErrorUtils.compactMessage(e));

                if (AiErrorUtils.isRateLimit(e) || AiErrorUtils.isUnauthorized(e)) {
                    closed.set(true);
                    SseUtils.sendAiError(emitter, e, "批量出题失败");
                    return;
                }

                if (!sendEvent(emitter, "batch_error",
                        new BatchError(batchNumber, "第 " + batchNumber + " 批生成失败，已跳过继续生成", emitted, total))) {
                    closed.set(true);
                    return;
                }
                if (consecutiveFailures >= MAX_CONSECUTIVE_BATCH_FAILURES) {
                    SseUtils.sendError(emitter, "BATCH_GENERATION_FAILED", "连续多批生成失败，请稍后重试或减少题目数量");
                    return;
                }
                pauseBetweenBatches(batch, batches);
            }
        }

        if (closed.get()) {
            return;
        }
        if (emitted == 0) {
            SseUtils.sendAiError(emitter,
                    lastError != null ? lastError : new IllegalStateException("All batch chunks failed"),
                    "批量出题失败，请稍后重试或减少题目数量");
            return;
        }
        if (!sendEvent(emitter, "done", new BatchDone(total, emitted, failedBatches))) {
            closed.set(true);
            return;
        }
        emitter.complete();
    }

    private boolean sendEvent(SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(objectMapper.writeValueAsString(payload), MediaType.APPLICATION_JSON));
            return true;
        } catch (Exception e) {
            log.warn("Batch question stream send failed: {}", AiErrorUtils.compactMessage(e));
            return false;
        }
    }

    private void pauseBetweenBatches(int batchIndex, int batches) {
        if (batchIndex >= batches - 1) {
            return;
        }
        try {
            Thread.sleep(BATCH_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("批量出题已中断", e);
        }
    }
}
