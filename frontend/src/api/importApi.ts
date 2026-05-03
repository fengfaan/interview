import type {
  CaptureRequest,
  CaptureResponse,
  ImportSaveRequest,
  ImportSaveResult,
} from '../types/import'
import { streamPostEvents } from './streamClient'

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

export async function streamParseQuestions(
  content: string,
  onItems: (items: import('../types/import').ParsedQuestion[]) => void,
  onProgress: (text: string) => void,
  onError?: (error: string) => void,
): Promise<void> {
  await streamPostEvents(
    API_BASE + '/parse/stream',
    { content },
    (event) => {
      if (event.type === 'progress') {
        onProgress(event.data)
        return
      }
      if (event.type === 'items') {
        try {
          const parsed = JSON.parse(event.data)
          if (parsed.items) {
            onItems(parsed.items)
          }
        } catch {
          // ignore parse errors for individual chunks
        }
      }
    },
    onError,
  )
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
