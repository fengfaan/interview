import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { DeepDiveContextType, ChatMessage, AgentStepInfo } from '../types/deepDive'
import * as api from '../api/deepDiveApi'

export const useDeepDiveStore = defineStore('deepDive', () => {
  const isOpen = ref(false)
  const sourceType = ref<DeepDiveContextType>('RECOMMENDED_ANSWER')
  const contextContent = ref('')
  const messages = ref<ChatMessage[]>([])
  const inputText = ref('')
  const isStreaming = ref(false)
  const streamingContent = ref('')
  const agentStepInfo = ref<AgentStepInfo | null>(null)

  function openDeepDive(type: DeepDiveContextType, content: string) {
    if (sourceType.value !== type || contextContent.value !== content) {
      messages.value = []
      streamingContent.value = ''
    }
    sourceType.value = type
    contextContent.value = content
    inputText.value = ''
    isStreaming.value = false
    isOpen.value = true
  }

  async function sendMessage(question: string, questionText: string, expectedKeywords: string[]) {
    if (!question.trim() || isStreaming.value) return

    const userMessage: ChatMessage = { role: 'USER', content: question }
    messages.value.push(userMessage)
    inputText.value = ''
    isStreaming.value = true
    streamingContent.value = ''
    agentStepInfo.value = null

    try {
      await api.streamDeepDive(
        {
          question: questionText,
          expectedKeywords,
          contextType: sourceType.value,
          contextContent: contextContent.value,
          messages: messages.value,
        },
        (chunk) => {
          streamingContent.value += chunk
        },
        (err) => {
          streamingContent.value = ''
          messages.value.pop()
          inputText.value = question
          isStreaming.value = false
          console.error('Deep dive streaming error:', err)
        },
        (keyword, notes) => {
          agentStepInfo.value = { keyword, notes }
        },
      )

      if (streamingContent.value) {
        messages.value.push({ role: 'ASSISTANT', content: streamingContent.value })
      }
      streamingContent.value = ''
    } catch (e: any) {
      messages.value.pop()
      inputText.value = question
    } finally {
      isStreaming.value = false
    }
  }

  function closeDeepDive() {
    isOpen.value = false
  }

  function reset() {
    isOpen.value = false
    messages.value = []
    inputText.value = ''
    contextContent.value = ''
    streamingContent.value = ''
    isStreaming.value = false
    agentStepInfo.value = null
  }

  return {
    isOpen, sourceType, contextContent, messages, inputText,
    isStreaming, streamingContent, agentStepInfo,
    openDeepDive, sendMessage, closeDeepDive, reset,
  }
})
