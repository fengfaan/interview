const API_BASE = '/api'

export interface SseEvent {
  type: string
  data: string
}

function readSseDataLine(line: string) {
  const index = line.indexOf(':')
  if (index === -1) {
    return { field: line, value: '' }
  }
  const field = line.slice(0, index)
  const rawValue = line.slice(index + 1)
  return {
    field,
    value: rawValue.startsWith(' ') ? rawValue.slice(1) : rawValue,
  }
}

export async function streamPost(
  url: string,
  body: unknown,
  onChunk: (text: string) => void,
  onError?: (error: string) => void,
): Promise<void> {
  return streamPostEvents(
    url,
    body,
    (event) => {
      if (event.type === 'progress') return
      onChunk(event.data)
    },
    onError,
  )
}

export async function streamPostEvents(
  url: string,
  body: unknown,
  onEvent: (event: SseEvent) => void,
  onError?: (error: string) => void,
): Promise<void> {
  const response = await fetch(`${API_BASE}${url}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })

  if (!response.ok) {
    try {
      const err = await response.json()
      onError?.(err.message || `请求失败: ${response.status}`)
    } catch {
      onError?.(`请求失败: ${response.status}`)
    }
    return
  }

  const reader = response.body?.getReader()
  if (!reader) {
    onError?.('无法读取响应流')
    return
  }

  const decoder = new TextDecoder()
  let buffer = ''

  function handleEvent(rawEvent: string) {
    if (!rawEvent.trim()) return false

    let eventType = 'message'
    const dataLines: string[] = []

    for (const line of rawEvent.split(/\r?\n/)) {
      if (!line || line.startsWith(':')) continue

      const { field, value } = readSseDataLine(line)
      if (field === 'event') {
        eventType = value
      } else if (field === 'data') {
        dataLines.push(value)
      }
    }

    if (!dataLines.length) return false

    const data = dataLines.join('\n')
    if (data === '[DONE]') return true

    if (eventType === 'error') {
      try {
        const err = JSON.parse(data)
        onError?.(err.message || '流式生成出错')
      } catch {
        onError?.(data)
      }
      return true
    }

    onEvent({ type: eventType, data })
    return false
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const events = buffer.split(/\r?\n\r?\n/)
    buffer = events.pop() || ''

    for (const event of events) {
      if (handleEvent(event)) return
    }
  }

  buffer += decoder.decode()
  if (buffer.trim()) {
    handleEvent(buffer)
  }
}
