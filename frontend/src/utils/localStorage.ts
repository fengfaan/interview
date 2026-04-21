const MAX_SESSIONS = 3

interface SessionEntry {
  timestamp: number
  data: unknown
}

export function loadState(key: string): any {
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return null
    const entry: SessionEntry = JSON.parse(raw)
    return entry.data
  } catch {
    return null
  }
}

export function saveState(key: string, data: unknown): void {
  try {
    const entry: SessionEntry = { timestamp: Date.now(), data }
    localStorage.setItem(key, JSON.stringify(entry))
  } catch {
    // storage full, try cleanup
    cleanupOldSessions()
    try {
      const entry: SessionEntry = { timestamp: Date.now(), data }
      localStorage.setItem(key, JSON.stringify(entry))
    } catch {
      // give up silently
    }
  }
}

function cleanupOldSessions() {
  const keys = ['ai-career-prep.mock-interview', 'ai-career-prep.resume-optimizer']
  for (const key of keys) {
    try {
      localStorage.removeItem(key + '.old')
    } catch { /* ignore */ }
  }
}
