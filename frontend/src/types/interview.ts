export interface HistoryEntry {
  questionId: string
  question: string
  answer: string | null
  skipped: boolean
}

export interface QuestionRequest {
  direction: string
  level: string
  history: HistoryEntry[]
}

export interface QuestionResponse {
  questionId: string
  question: string
  expectedKeywords: string[]
}

export interface FeedbackRequest {
  direction: string
  level: string
  question: string
  answer: string
  expectedKeywords: string[]
}

export interface RecommendedAnswerRequest {
  direction: string
  level: string
  question: string
  expectedKeywords: string[]
}

export interface KeywordHits {
  hit: string[]
  miss: string[]
}

export interface FeedbackResponse {
  keywordHits: KeywordHits
  score: number
}

export type Direction =
  | 'GO_BACKEND'
  | 'REACT_FRONTEND'
  | 'SYSTEM_DESIGN'
  | 'DATABASE_RELATED'
  | 'AI_CODING'
export type Level =
  | 'BASIC'
  | 'DEEP_PRINCIPLE'
  | 'PROJECT_PRACTICE'

export interface DirectionOption {
  value: Direction
  label: string
}

export interface LevelOption {
  value: Level
  label: string
}

export const DIRECTIONS: DirectionOption[] = [
  { value: 'GO_BACKEND', label: 'Go 后端' },
  { value: 'REACT_FRONTEND', label: 'React 前端' },
  { value: 'SYSTEM_DESIGN', label: '系统设计' },
  { value: 'DATABASE_RELATED', label: '数据库相关' },
  { value: 'AI_CODING', label: 'AI Agent 开发方向' },
]

export const LEVELS: LevelOption[] = [
  { value: 'BASIC', label: '基础八股' },
  { value: 'DEEP_PRINCIPLE', label: '深度原理' },
  { value: 'PROJECT_PRACTICE', label: '项目实战' },
]

export interface BatchQuestionItem {
  questionId: string
  question: string
  answer: string
  keywords: string[]
}

export interface BatchQuestionRequest {
  direction: string
  level: string
  count: number
}
