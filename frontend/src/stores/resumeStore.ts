import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { AnalyzeResponse, Suggestion } from '../types/resume'
import * as api from '../api/resumeApi'
import { loadState, saveState } from '../utils/localStorage'

const STORAGE_KEY = 'ai-career-prep.resume-optimizer'

export const useResumeStore = defineStore('resume', () => {
  const jobDescription = ref('')
  const resume = ref('')
  const analysisResult = ref<AnalyzeResponse | null>(null)
  const selectedSuggestion = ref<Suggestion | null>(null)
  const starRewrite = ref('')
  const starWorkspaceExpanded = ref(false)
  const isLoading = ref(false)
  const isStreaming = ref(false)
  const error = ref('')
  const dismissedSuggestions = ref<Set<string>>(new Set())

  const visibleSuggestions = computed(() => {
    if (!analysisResult.value) return []
    return analysisResult.value.suggestions.filter(s => !dismissedSuggestions.value.has(s.id))
  })

  function restore() {
    const saved = loadState(STORAGE_KEY)
    if (saved) {
      jobDescription.value = saved.jobDescription ?? ''
      resume.value = saved.resume ?? ''
      analysisResult.value = saved.analysisResult ?? null
      dismissedSuggestions.value = new Set(saved.dismissedSuggestions ?? [])
    }
  }

  function persist() {
    saveState(STORAGE_KEY, {
      jobDescription: jobDescription.value,
      resume: resume.value,
      analysisResult: analysisResult.value,
      dismissedSuggestions: [...dismissedSuggestions.value],
    })
  }

  async function analyze() {
    if (!jobDescription.value.trim() || !resume.value.trim()) return
    error.value = ''
    isLoading.value = true
    analysisResult.value = null
    starWorkspaceExpanded.value = false
    try {
      const res = await api.analyzeResume({
        jobDescription: jobDescription.value,
        resume: resume.value,
      })
      analysisResult.value = res
      persist()
    } catch (e: any) {
      error.value = e.message || '分析失败'
    } finally {
      isLoading.value = false
    }
  }

  async function selectSuggestion(suggestion: Suggestion) {
    selectedSuggestion.value = suggestion
    starRewrite.value = ''
    starWorkspaceExpanded.value = true
    isStreaming.value = true
    error.value = ''

    try {
      await api.streamRewrite(
        {
          jobDescription: jobDescription.value,
          resume: resume.value,
          suggestion: {
            id: suggestion.id,
            title: suggestion.title,
            sourceText: suggestion.sourceText,
          },
        },
        (chunk) => {
          starRewrite.value += chunk
        },
        (err) => {
          error.value = err
          isStreaming.value = false
        },
      )
      isStreaming.value = false
    } catch (e: any) {
      error.value = e.message || '改写失败'
      isStreaming.value = false
    }
  }

  function dismissSuggestion() {
    if (selectedSuggestion.value) {
      dismissedSuggestions.value.add(selectedSuggestion.value.id)
    }
    starWorkspaceExpanded.value = false
    selectedSuggestion.value = null
    starRewrite.value = ''
    persist()
  }

  function applyStarRewrite() {
    if (!selectedSuggestion.value || !starRewrite.value) return
    const sourceText = selectedSuggestion.value.sourceText
    if (sourceText && resume.value.includes(sourceText)) {
      resume.value = resume.value.replace(sourceText, starRewrite.value)
    }
    starWorkspaceExpanded.value = false
    selectedSuggestion.value = null
    starRewrite.value = ''
    persist()
  }

  restore()

  return {
    jobDescription, resume, analysisResult, selectedSuggestion, starRewrite,
    starWorkspaceExpanded, isLoading, isStreaming, error, dismissedSuggestions,
    visibleSuggestions,
    analyze, selectSuggestion, dismissSuggestion, applyStarRewrite, persist,
  }
})
