export interface AnalyzeRequest {
  jobDescription: string
  resume: string
}

export interface Dimension {
  name: string
  score: number
  reason: string
}

export interface Suggestion {
  id: string
  priority: 'HIGH' | 'MEDIUM' | 'LOW'
  title: string
  reason: string
  sourceText: string
}

export interface AnalyzeResponse {
  score: number
  dimensions: Dimension[]
  suggestions: Suggestion[]
}

export interface RewriteStreamRequest {
  jobDescription: string
  resume: string
  suggestion: {
    id: string
    title: string
    sourceText: string
  }
}
