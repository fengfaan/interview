import type { NoteItem, NoteDetail, CreateNoteRequest } from '../types/knowledge'

const API_BASE = '/api/knowledge'

export async function listNotes(direction?: string): Promise<NoteItem[]> {
  const params = direction ? `?direction=${encodeURIComponent(direction)}` : ''
  const res = await fetch(`${API_BASE}/notes${params}`)
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '获取笔记列表失败')
  return json.data
}

export async function getNote(noteId: string): Promise<NoteDetail> {
  const res = await fetch(`${API_BASE}/note?id=${encodeURIComponent(noteId)}`)
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '获取笔记详情失败')
  return json.data
}

export async function createNote(request: CreateNoteRequest): Promise<NoteItem> {
  const res = await fetch(`${API_BASE}/notes`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '保存笔记失败')
  return json.data
}

export async function searchNotes(keyword: string): Promise<NoteItem[]> {
  const res = await fetch(`${API_BASE}/search?keyword=${encodeURIComponent(keyword)}`)
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '搜索笔记失败')
  return json.data
}
