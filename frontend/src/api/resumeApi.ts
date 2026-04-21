import type { AnalyzeRequest, AnalyzeResponse, RewriteStreamRequest } from '../types/resume'
import { streamPost } from './streamClient'

const API_BASE = '/api/resume'

export async function analyzeResume(request: AnalyzeRequest): Promise<AnalyzeResponse> {
  const res = await fetch(API_BASE + '/analyze', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '分析失败')
  return json.data
}

export function streamRewrite(
  request: RewriteStreamRequest,
  onChunk: (text: string) => void,
  onError?: (error: string) => void,
): Promise<void> {
  return streamPost('/resume/rewrite/stream', request, onChunk, onError)
}
