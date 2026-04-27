import type {
  ApiKeyResponse,
  ApiKeyRequest,
  ModelRequest,
  ModelResponse,
  PromptContentResponse,
  PromptFile,
  PromptImproveRequest,
  PromptImproveResponse,
  PromptSaveRequest,
} from '../types/settings'

const API_BASE = '/api/settings'

export async function getApiKeyMasked(provider: string): Promise<ApiKeyResponse> {
  const res = await fetch(`${API_BASE}/apikey?provider=${encodeURIComponent(provider)}`)
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

export async function getModel(): Promise<ModelResponse> {
  const res = await fetch(`${API_BASE}/model`)
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '获取模型设置失败')
  return json.data
}

export async function saveModel(request: ModelRequest): Promise<ModelResponse> {
  const res = await fetch(`${API_BASE}/model`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '保存模型失败')
  return json.data
}

export async function listPrompts(): Promise<PromptFile[]> {
  const res = await fetch(`${API_BASE}/prompts`)
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '获取提示词列表失败')
  return json.data
}

export async function getPromptContent(path: string): Promise<PromptContentResponse> {
  const res = await fetch(`${API_BASE}/prompts/content?path=${encodeURIComponent(path)}`)
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '获取提示词失败')
  return json.data
}

export async function savePromptContent(request: PromptSaveRequest): Promise<PromptContentResponse> {
  const res = await fetch(`${API_BASE}/prompts/content`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '保存提示词失败')
  return json.data
}

export async function improvePrompt(request: PromptImproveRequest): Promise<PromptImproveResponse> {
  const res = await fetch(`${API_BASE}/prompts/improve`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  const json = await res.json()
  if (!json.success) throw new Error(json.message || 'AI 优化失败')
  return json.data
}

export async function getVaultConfig(): Promise<{ configured: boolean; path: string | null }> {
  const res = await fetch(`${API_BASE}/vault`)
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '获取 Vault 配置失败')
  return json.data
}

export async function saveVaultConfig(path: string): Promise<{ configured: boolean; path: string | null }> {
  const res = await fetch(`${API_BASE}/vault`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ path }),
  })
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '保存 Vault 配置失败')
  return json.data
}
