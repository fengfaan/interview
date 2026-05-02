import { defineStore } from 'pinia'
import { ref } from 'vue'
import { streamBatchQuestionEvents, streamBatchAnswer } from '../api/interviewApi'
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
  const generatedCount = ref(0)
  const progressText = ref('')
  const currentBatch = ref(0)
  const totalBatches = ref(0)
  const failedBatches = ref<number[]>([])
  const loadingAnswerIds = ref<Set<string>>(new Set())
  const answerErrors = ref<Map<string, string>>(new Map())

  const COUNT_OPTIONS = [10, 20, 50, 100]

  async function generate() {
    error.value = ''
    isLoading.value = true
    expandedIds.value = new Set()
    savedIds.value = new Set()
    generatedCount.value = 0
    progressText.value = '批量出题已开始'
    currentBatch.value = 0
    totalBatches.value = 0
    failedBatches.value = []
    loadingAnswerIds.value = new Set()
    answerErrors.value = new Map()

    const existing = questions.value.map(q => q.question)
    questions.value = []

    try {
      await streamBatchQuestionEvents(
        {
          direction: direction.value,
          level: level.value,
          count: count.value,
          existingQuestions: existing.length > 0 ? existing : undefined,
        },
        {
          onProgress: (payload) => {
            totalBatches.value = payload.batches
            progressText.value = payload.message
          },
          onBatchStart: (payload) => {
            currentBatch.value = payload.batchNumber
            totalBatches.value = payload.totalBatches
            progressText.value = `正在生成第 ${payload.batchNumber} / ${payload.totalBatches} 批`
          },
          onBatch: (items, payload) => {
            questions.value = [...questions.value, ...items]
              .sort((a, b) => a.questionId.localeCompare(b.questionId))
              .slice(0, count.value)
            generatedCount.value = questions.value.length
            progressText.value = `已生成 ${payload.generated} / ${payload.total} 题`
          },
          onBatchError: (payload) => {
            failedBatches.value = [...failedBatches.value, payload.batchNumber]
            generatedCount.value = payload.generated
            progressText.value = payload.message
          },
          onDone: (payload) => {
            generatedCount.value = payload.generated
            failedBatches.value = payload.failedBatches || []
            progressText.value = payload.failedBatches?.length
              ? `已生成 ${payload.generated} / ${payload.requested} 题，部分批次失败`
              : `已生成 ${payload.generated} / ${payload.requested} 题`
          },
          onError: (message) => {
            error.value = message
          },
        },
      )
    } catch (e: any) {
      error.value = e.message || '批量出题失败'
    } finally {
      isLoading.value = false
    }
  }

  async function loadAnswer(questionId: string) {
    const q = questions.value.find(item => item.questionId === questionId)
    if (!q || q.answer) return

    loadingAnswerIds.value = new Set([...loadingAnswerIds.value, questionId])
    answerErrors.value = new Map([...answerErrors.value.entries()].filter(([k]) => k !== questionId))

    let fullAnswer = ''
    try {
      await streamBatchAnswer(
        {
          direction: direction.value,
          level: level.value,
          question: q.question,
          expectedKeywords: q.keywords,
        },
        (chunk) => {
          fullAnswer += chunk
          const idx = questions.value.findIndex(item => item.questionId === questionId)
          if (idx >= 0) {
            questions.value[idx] = { ...questions.value[idx], answer: fullAnswer }
          }
        },
        (errMsg) => {
          answerErrors.value = new Map([...answerErrors.value, [questionId, errMsg]])
        },
      )
    } catch (e: any) {
      answerErrors.value = new Map([...answerErrors.value, [questionId, e.message || '答案生成失败']])
    } finally {
      loadingAnswerIds.value = new Set([...loadingAnswerIds.value].filter(id => id !== questionId))
    }
  }

  function toggleExpand(id: string) {
    const s = new Set(expandedIds.value)
    if (s.has(id)) {
      s.delete(id)
    } else {
      s.add(id)
      const q = questions.value.find(item => item.questionId === id)
      if (q && !q.answer && !loadingAnswerIds.value.has(id)) {
        loadAnswer(id)
      }
    }
    expandedIds.value = s
  }

  function isExpanded(id: string) {
    return expandedIds.value.has(id)
  }

  function isLoadingAnswer(id: string) {
    return loadingAnswerIds.value.has(id)
  }

  function getAnswerError(id: string) {
    return answerErrors.value.get(id)
  }

  async function saveOne(item: BatchQuestionItem) {
    if (!item.answer) return false
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
      if (!savedIds.value.has(q.questionId) && q.answer) {
        await saveOne(q)
      }
    }
  }

  function reset() {
    questions.value = []
    expandedIds.value = new Set()
    savedIds.value = new Set()
    error.value = ''
    generatedCount.value = 0
    progressText.value = ''
    currentBatch.value = 0
    totalBatches.value = 0
    failedBatches.value = []
    loadingAnswerIds.value = new Set()
    answerErrors.value = new Map()
  }

  return {
    direction, level, count, questions, expandedIds,
    isLoading, error, savedIds, generatedCount, progressText,
    currentBatch, totalBatches, failedBatches, COUNT_OPTIONS,
    loadingAnswerIds, answerErrors,
    generate, toggleExpand, isExpanded, isLoadingAnswer, getAnswerError,
    loadAnswer, saveOne, saveAll, reset,
  }
})
