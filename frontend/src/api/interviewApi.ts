import type {
  QuestionRequest,
  QuestionResponse,
  FeedbackRequest,
  FeedbackResponse,
  RecommendedAnswerRequest,
  BatchQuestionItem,
  BatchQuestionRequest,
} from '../types/interview'
import { streamPost } from './streamClient'

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
  const res = await fetch(API_BASE + '/batch-questions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '批量出题失败')
  return json.data
}
