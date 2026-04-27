package com.interviewassistant.dto.interview;

import java.util.List;

/**
 * 批量出题 SSE 事件载荷，从 InterviewController 提取到 DTO 层。
 */
public final class BatchSseEvents {

    private BatchSseEvents() {
    }

    public record BatchProgress(String message, int total, int batchSize, int batches, int generated, int failed) {
    }

    public record BatchStart(int batchNumber, int totalBatches, int startIndex, int count, int total, int generated) {
    }

    public record BatchPayload(int batchNumber, List<BatchQuestionItem> items, int generated, int total) {
    }

    public record BatchError(int batchNumber, String message, int generated, int total) {
    }

    public record BatchDone(int requested, int generated, List<Integer> failedBatches) {
    }
}
