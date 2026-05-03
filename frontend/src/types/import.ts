export interface CaptureRequest {
  url: string
}

export interface CaptureResponse {
  title: string
  content: string
  url: string
  capturedAt: string
}

export interface ParsedQuestion {
  question: string
  answer: string | null
  keywords: string[]
}

export interface ImportSaveRequest {
  items: ParsedQuestion[]
  sourceUrl: string
}

export interface ImportSaveResult {
  title: string
  success: boolean
  error: string | null
}
