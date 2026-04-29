export type DeepDiveContextType = 'RECOMMENDED_ANSWER' | 'FEEDBACK'

export type ChatRole = 'USER' | 'ASSISTANT'

export interface ChatMessage {
  role: ChatRole
  content: string
}

export interface DeepDiveRequest {
  question: string
  expectedKeywords: string[]
  contextType: DeepDiveContextType
  contextContent: string
  messages: ChatMessage[]
}

export interface AgentStepInfo {
  keyword: string
  notes: string[]
}
