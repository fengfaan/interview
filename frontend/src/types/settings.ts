export interface ApiKeyResponse {
  masked: string
  configured: boolean
}

export interface ApiKeyRequest {
  provider: string
  apiKey: string
}

export interface ModelResponse {
  provider: string
  model: string
  defaultModel: string
  options: string[]
}

export interface ModelRequest {
  provider: string
  model: string
}

export interface PromptFile {
  path: string
  group: string
  name: string
  size: number
  lastModified: string
}

export interface PromptContentResponse {
  path: string
  content: string
}

export interface PromptSaveRequest {
  path: string
  content: string
}

export interface PromptImproveRequest {
  path: string
  content: string
  instruction?: string
}

export interface PromptImproveResponse {
  content: string
}
