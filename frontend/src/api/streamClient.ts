const API_BASE = '/api'

export async function streamPost(
  url: string,
  body: unknown,
  onChunk: (text: string) => void,
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

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''

    let isErrorEvent = false
    for (const line of lines) {
      if (line.startsWith('event:') && line.includes('error')) {
        isErrorEvent = true
      } else if (line.startsWith('data:')) {
        const data = line.startsWith('data: ') ? line.slice(6) : line.slice(5)
        if (data === '[DONE]') return
        if (isErrorEvent) {
          try {
            const err = JSON.parse(data)
            onError?.(err.message || '流式生成出错')
          } catch {
            onError?.(data)
          }
          return
        }
        onChunk(data)
        isErrorEvent = false
      }
    }
  }
}
