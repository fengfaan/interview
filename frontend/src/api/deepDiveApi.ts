import type { DeepDiveRequest } from '../types/deepDive'
import { streamPost } from './streamClient'

export function streamDeepDive(
  request: DeepDiveRequest,
  onChunk: (text: string) => void,
  onError?: (error: string) => void,
): Promise<void> {
  return streamPost('/interview/deep-dive/stream', request, onChunk, onError)
}
