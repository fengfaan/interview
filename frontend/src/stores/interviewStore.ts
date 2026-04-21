import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Direction, Level, HistoryEntry, KeywordHits } from '../types/interview'
import * as api from '../api/interviewApi'
import { loadState, saveState } from '../utils/localStorage'

const STORAGE_KEY = 'ai-career-prep.mock-interview'

function extractFollowUpQuestion(commentary: string): string {
  const match = commentary.match(/##\s*深度追问\s*\n([\s\S]+)/)
  if (!match) return ''
  const section = match[1].trim()
  const boldMatch = section.match(/\*\*(.+?)\*\*/)
  if (boldMatch) return boldMatch[1].replace(/^问题[：:]\s*/, '')
  const lines = section.split('\n').filter(l => l.trim())
  if (lines.length > 0) return lines[0].replace(/^[-*]\s*/, '').trim()
  return ''
}

export const useInterviewStore = defineStore('interview', () => {
  const direction = ref<Direction>('GO_BACKEND')
  const level = ref<Level>('DEEP_PRINCIPLE')
  const isStarted = ref(false)
  const currentQuestion = ref<{ questionId: string; question: string; expectedKeywords: string[] } | null>(null)
  const draftAnswer = ref('')
  const history = ref<HistoryEntry[]>([])
  const feedbackExpanded = ref(false)
  const keywordHits = ref<KeywordHits | null>(null)
  const score = ref<number | null>(null)
  const commentary = ref('')
  const followUpQuestion = ref('')
  const isLoading = ref(false)
  const isStreaming = ref(false)
  const error = ref('')

  function restore() {
    const saved = loadState(STORAGE_KEY)
    if (saved) {
      direction.value = saved.direction ?? 'GO_BACKEND'
      level.value = saved.level ?? 'DEEP_PRINCIPLE'
      isStarted.value = saved.isStarted ?? false
      currentQuestion.value = saved.currentQuestion ?? null
      draftAnswer.value = saved.draftAnswer ?? ''
      history.value = saved.history ?? []
      feedbackExpanded.value = saved.feedbackExpanded ?? false
      keywordHits.value = saved.keywordHits ?? null
      score.value = saved.score ?? null
      commentary.value = saved.commentary ?? ''
      followUpQuestion.value = saved.followUpQuestion ?? ''
    }
  }

  function persist() {
    saveState(STORAGE_KEY, {
      direction: direction.value,
      level: level.value,
      isStarted: isStarted.value,
      currentQuestion: currentQuestion.value,
      draftAnswer: draftAnswer.value,
      history: history.value,
      feedbackExpanded: feedbackExpanded.value,
      keywordHits: keywordHits.value,
      score: score.value,
      commentary: commentary.value,
      followUpQuestion: followUpQuestion.value,
    })
  }

  async function startSession() {
    error.value = ''
    isLoading.value = true
    feedbackExpanded.value = false
    keywordHits.value = null
    score.value = null
    commentary.value = ''
    followUpQuestion.value = ''
    try {
      const res = await api.generateQuestion({
        direction: direction.value,
        level: level.value,
        history: history.value,
      })
      currentQuestion.value = res
      isStarted.value = true
      persist()
    } catch (e: any) {
      error.value = e.message || '生成问题失败'
    } finally {
      isLoading.value = false
    }
  }

  async function generateQuestion() {
    error.value = ''
    isLoading.value = true
    feedbackExpanded.value = false
    keywordHits.value = null
    score.value = null
    commentary.value = ''
    followUpQuestion.value = ''
    try {
      const res = await api.generateQuestion({
        direction: direction.value,
        level: level.value,
        history: history.value,
      })
      currentQuestion.value = res
      persist()
    } catch (e: any) {
      error.value = e.message || '生成问题失败'
    } finally {
      isLoading.value = false
    }
  }

  async function submitAnswer() {
    if (!currentQuestion.value || !draftAnswer.value.trim()) return
    error.value = ''
    isLoading.value = true
    isStreaming.value = false
    commentary.value = ''

    try {
      const feedbackRes = await api.analyzeFeedback({
        direction: direction.value,
        level: level.value,
        question: currentQuestion.value.question,
        answer: draftAnswer.value,
        expectedKeywords: currentQuestion.value.expectedKeywords,
      })
      keywordHits.value = feedbackRes.keywordHits
      score.value = feedbackRes.score
      feedbackExpanded.value = true
      isLoading.value = false
      persist()

      isStreaming.value = true
      await api.streamFeedback(
        {
          direction: direction.value,
          level: level.value,
          question: currentQuestion.value.question,
          answer: draftAnswer.value,
          expectedKeywords: currentQuestion.value.expectedKeywords,
        },
        (chunk) => {
          commentary.value += chunk
        },
        (err) => {
          error.value = err
          isStreaming.value = false
        },
      )
      isStreaming.value = false
      followUpQuestion.value = extractFollowUpQuestion(commentary.value)

      history.value.push({
        questionId: currentQuestion.value.questionId,
        question: currentQuestion.value.question,
        answer: draftAnswer.value,
        skipped: false,
      })
      if (history.value.length > 10) history.value = history.value.slice(-10)
      persist()
    } catch (e: any) {
      error.value = e.message || '提交失败'
      isLoading.value = false
      isStreaming.value = false
    }
  }

  async function skipQuestion() {
    if (!currentQuestion.value) return
    history.value.push({
      questionId: currentQuestion.value.questionId,
      question: currentQuestion.value.question,
      answer: null,
      skipped: true,
    })
    draftAnswer.value = ''
    feedbackExpanded.value = false
    keywordHits.value = null
    commentary.value = ''
    await generateQuestion()
  }

  function continueChallenge() {
    if (!followUpQuestion.value) return
    currentQuestion.value = {
      questionId: 'q_followup',
      question: followUpQuestion.value,
      expectedKeywords: [],
    }
    draftAnswer.value = ''
    feedbackExpanded.value = false
    keywordHits.value = null
    score.value = null
    commentary.value = ''
    followUpQuestion.value = ''
    persist()
  }

  function newSession() {
    direction.value = 'GO_BACKEND'
    level.value = 'DEEP_PRINCIPLE'
    isStarted.value = false
    currentQuestion.value = null
    draftAnswer.value = ''
    history.value = []
    feedbackExpanded.value = false
    keywordHits.value = null
    score.value = null
    commentary.value = ''
    followUpQuestion.value = ''
    isLoading.value = false
    isStreaming.value = false
    error.value = ''
    persist()
  }

  restore()

  return {
    direction, level, isStarted, currentQuestion, draftAnswer, history,
    feedbackExpanded, keywordHits, score, commentary, followUpQuestion,
    isLoading, isStreaming, error,
    startSession, generateQuestion, submitAnswer, skipQuestion,
    continueChallenge, newSession, persist,
  }
})
