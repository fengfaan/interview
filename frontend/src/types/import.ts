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

export interface ConsolidatedCategory {
  name: string
  items: ParsedQuestion[]
}

export interface ConsolidateResult {
  categories: ConsolidatedCategory[]
  dedupCount: number
  totalCount: number
}

export interface ConsolidateRequest {
  items: ParsedQuestion[]
  sourceUrl: string
  title: string
}

export interface ConsolidatedSaveRequest {
  categories: ConsolidatedCategory[]
  sourceUrl: string
  title: string
}

export interface ConsolidatedSaveResult {
  filePath: string
  questionCount: number
  success: boolean
  error: string | null
}
