import type {
  CaptureRequest,
  CaptureResponse,
  ParseRequest,
  ParseResponse,
  ImportSaveRequest,
  ImportSaveResult,
} from '../types/import'

const API_BASE = '/api/import'

export async function capturePage(request: CaptureRequest): Promise<CaptureResponse> {
  const res = await fetch(API_BASE + '/capture', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '网页抓取失败')
  return json.data
}

export async function parseQuestions(request: ParseRequest): Promise<ParseResponse> {
  const res = await fetch(API_BASE + '/parse', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '解析失败')
  return json.data
}

export async function saveImported(request: ImportSaveRequest): Promise<ImportSaveResult[]> {
  const res = await fetch(API_BASE + '/save', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '导入失败')
  return json.data
}
