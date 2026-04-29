import type { DeepDiveRequest } from '../types/deepDive'
import { streamPostEvents } from './streamClient'

export function streamDeepDive(
  request: DeepDiveRequest,
  onChunk: (text: string) => void,
  onError?: (error: string) => void,
  onAgentStep?: (keyword: string, notes: string[]) => void,
): Promise<void> {
  return streamPostEvents(
    '/interview/deep-dive/stream',
    request,
    (event) => {
      if (event.type === 'agent_step') {
        try {
          const parsed = JSON.parse(event.data)
          onAgentStep?.(parsed.keyword ?? '', parsed.notes ?? [])
        } catch {
          // ignore malformed agent_step
        }
        return
      }
      if (event.type === 'progress') return
      onChunk(event.data)
    },
    onError,
  )
}
