export interface ApiKeyResponse {
  masked: string
  configured: boolean
}

export interface ApiKeyRequest {
  apiKey: string
}
