import type { ApiKeyResponse, ApiKeyRequest } from '../types/settings'

const API_BASE = '/api/settings'

export async function getApiKeyMasked(): Promise<ApiKeyResponse> {
  const res = await fetch(`${API_BASE}/apikey`)
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '获取设置失败')
  return json.data
}

export async function saveApiKey(request: ApiKeyRequest): Promise<void> {
  const res = await fetch(`${API_BASE}/apikey`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '保存失败')
}
