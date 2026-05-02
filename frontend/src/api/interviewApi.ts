import type {
  QuestionRequest,
  QuestionResponse,
  FeedbackRequest,
  FeedbackResponse,
  RecommendedAnswerRequest,
  BatchQuestionItem,
  BatchQuestionRequest,
} from '../types/interview'
import { streamPost, streamPostEvents } from './streamClient'

const API_BASE = '/api/interview'

export async function generateQuestion(request: QuestionRequest): Promise<QuestionResponse> {
  const res = await fetch(API_BASE + '/question', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '生成问题失败')
  return json.data
}

export async function analyzeFeedback(request: FeedbackRequest): Promise<FeedbackResponse> {
  const res = await fetch(API_BASE + '/feedback', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '分析失败')
  return json.data
}

export function streamFeedback(
  request: FeedbackRequest,
  onChunk: (text: string) => void,
  onError?: (error: string) => void,
): Promise<void> {
  return streamPost('/interview/feedback/stream', request, onChunk, onError)
}

export function streamRecommendedAnswer(
  request: RecommendedAnswerRequest,
  onChunk: (text: string) => void,
  onError?: (error: string) => void,
): Promise<void> {
  return streamPost('/interview/recommended-answer/stream', request, onChunk, onError)
}

export async function generateBatchQuestions(request: BatchQuestionRequest): Promise<BatchQuestionItem[]> {
  const items: BatchQuestionItem[] = []
  await streamBatchQuestions(request, (batch) => {
    items.push(...batch)
  })
  return items
}

export function streamBatchQuestions(
  request: BatchQuestionRequest,
  onBatch: (items: BatchQuestionItem[]) => void,
  onError?: (error: string) => void,
): Promise<void> {
  return streamBatchQuestionEvents(request, {
    onBatch,
    onError,
  })
}

export interface BatchProgressPayload {
  message: string
  total: number
  batchSize: number
  batches: number
  generated: number
  failed: number
}

export interface BatchStartPayload {
  batchNumber: number
  totalBatches: number
  startIndex: number
  count: number
  total: number
  generated: number
}

export interface BatchPayload {
  batchNumber: number
  items: BatchQuestionItem[]
  generated: number
  total: number
}

export interface BatchErrorPayload {
  batchNumber: number
  message: string
  generated: number
  total: number
}

export interface BatchDonePayload {
  requested: number
  generated: number
  failedBatches: number[]
}

export interface BatchQuestionStreamHandlers {
  onProgress?: (payload: BatchProgressPayload) => void
  onBatchStart?: (payload: BatchStartPayload) => void
  onBatch?: (items: BatchQuestionItem[], payload: BatchPayload) => void
  onBatchError?: (payload: BatchErrorPayload) => void
  onDone?: (payload: BatchDonePayload) => void
  onError?: (error: string) => void
}

export function streamBatchQuestionEvents(
  request: BatchQuestionRequest,
  handlers: BatchQuestionStreamHandlers,
): Promise<void> {
  return streamPostEvents('/interview/batch-questions/stream', request, (event) => {
    try {
      const payload = JSON.parse(event.data)
      if (event.type === 'progress') {
        handlers.onProgress?.(payload)
      } else if (event.type === 'batch_start') {
        handlers.onBatchStart?.(payload)
      } else if (event.type === 'batch') {
        handlers.onBatch?.(payload.items || [], payload)
      } else if (event.type === 'batch_error') {
        handlers.onBatchError?.(payload)
      } else if (event.type === 'done') {
        handlers.onDone?.(payload)
      }
    } catch {
      handlers.onError?.('批量出题响应解析失败')
    }
  }, handlers.onError)
}

export function streamBatchAnswer(
  request: RecommendedAnswerRequest,
  onChunk: (text: string) => void,
  onError?: (error: string) => void,
): Promise<void> {
  return streamPost('/interview/batch-answer/stream', request, onChunk, onError)
}
