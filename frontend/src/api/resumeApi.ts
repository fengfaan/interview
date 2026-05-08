import type {
  AnalyzeRequest,
  AnalyzeResponse,
  RewriteStreamRequest,
  StructureAnalysisRequest,
  StructureAnalysisResponse,
  PolishStreamRequest,
  ImportFileResponse
} from '../types/resume'
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

export async function analyzeStructure(request: StructureAnalysisRequest): Promise<{ value: StructureAnalysisResponse, actualModel: string }> {
  const res = await fetch(API_BASE + '/structure-analysis', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '诊断失败')
  return json.data
}

export function streamRewrite(
  request: RewriteStreamRequest,
  onChunk: (text: string) => void,
  onError?: (error: string) => void,
): Promise<void> {
  return streamPost('/resume/rewrite/stream', request, onChunk, onError)
}

export function streamPolish(
  request: PolishStreamRequest,
  onChunk: (text: string) => void,
  onError?: (error: string) => void,
): Promise<void> {
  return streamPost('/resume/polish/stream', request, onChunk, onError)
}

export async function importFile(file: File): Promise<ImportFileResponse> {
  const formData = new FormData()
  formData.append('file', file)
  const res = await fetch(API_BASE + '/import-file', {
    method: 'POST',
    body: formData,
  })
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '文件解析失败')
  return json.data
}
