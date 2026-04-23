import { defineStore } from 'pinia'
import { ref } from 'vue'
import { generateBatchQuestions } from '../api/interviewApi'
import type { BatchQuestionItem } from '../types/interview'
import { useKnowledgeStore } from './knowledgeStore'

export const useRapidQuestionStore = defineStore('rapidQuestion', () => {
  const direction = ref('GO_BACKEND')
  const level = ref('BASIC')
  const count = ref(10)
  const questions = ref<BatchQuestionItem[]>([])
  const expandedIds = ref<Set<string>>(new Set())
  const isLoading = ref(false)
  const error = ref('')
  const savedIds = ref<Set<string>>(new Set())

  const COUNT_OPTIONS = [10, 20, 50, 100]

  async function generate() {
    error.value = ''
    isLoading.value = true
    expandedIds.value = new Set()
    savedIds.value = new Set()
    try {
      questions.value = await generateBatchQuestions({
        direction: direction.value,
        level: level.value,
        count: count.value,
      })
    } catch (e: any) {
      error.value = e.message || '批量出题失败'
    } finally {
      isLoading.value = false
    }
  }

  function toggleExpand(id: string) {
    const s = new Set(expandedIds.value)
    if (s.has(id)) {
      s.delete(id)
    } else {
      s.add(id)
    }
    expandedIds.value = s
  }

  function isExpanded(id: string) {
    return expandedIds.value.has(id)
  }

  async function saveOne(item: BatchQuestionItem) {
    const knowledgeStore = useKnowledgeStore()
    const ok = await knowledgeStore.saveNote({
      title: item.question,
      direction: direction.value,
      content: `## 题目\n\n${item.question}\n\n## 参考答案\n\n${item.answer}\n\n## 考察要点\n\n${item.keywords.join(', ')}`,
      tags: item.keywords.slice(0, 5),
      questionId: item.questionId,
      source: 'rapid-question',
    })
    if (ok) savedIds.value = new Set([...savedIds.value, item.questionId])
    return ok
  }

  async function saveAll() {
    for (const q of questions.value) {
      if (!savedIds.value.has(q.questionId)) {
        await saveOne(q)
      }
    }
  }

  function reset() {
    questions.value = []
    expandedIds.value = new Set()
    savedIds.value = new Set()
    error.value = ''
  }

  return {
    direction, level, count, questions, expandedIds,
    isLoading, error, savedIds, COUNT_OPTIONS,
    generate, toggleExpand, isExpanded, saveOne, saveAll, reset,
  }
})
