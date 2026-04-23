export interface NoteItem {
  id: string
  title: string
  direction: string
  tags: string[]
  created: string
  fileName: string
}

export interface NoteDetail extends NoteItem {
  content: string
}

export interface CreateNoteRequest {
  title: string
  direction: string
  content: string
  tags: string[]
  questionId?: string
  source: string
}

export interface VaultConfig {
  configured: boolean
  path: string | null
}
