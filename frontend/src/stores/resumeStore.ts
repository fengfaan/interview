import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { AnalyzeResponse, Suggestion } from '../types/resume'
import * as api from '../api/resumeApi'
import { loadState, saveState } from '../utils/localStorage'

const STORAGE_KEY = 'ai-career-prep.resume-optimizer'
const MIN_JD_MEANINGFUL_CHARS = 30
const MIN_RESUME_MEANINGFUL_CHARS = 50
const JD_SIGNALS = ['岗位', '职位', '职责', '要求', '任职', '招聘', '经验', '熟悉', '负责', '优先', 'Java', 'Spring', 'Vue', 'React', 'Go', 'Python', 'MySQL', 'Redis', '架构', '系统']
const RESUME_SIGNALS = ['项目', '工作', '经历', '经验', '技能', '教育', '公司', '负责', '开发', '优化', 'Java', 'Spring', 'Vue', 'React', 'Go', 'Python', 'MySQL', 'Redis', '架构', '系统']

function meaningfulLength(text: string) {
  return Array.from(text).filter((char) => /[\p{L}\p{N}]/u.test(char)).length
}

function containsAny(text: string, signals: string[]) {
  const normalized = text.toLowerCase()
  return signals.some((signal) => normalized.includes(signal.toLowerCase()))
}

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

  const inputValidationMessage = computed(() => {
    const jd = jobDescription.value.trim()
    const cv = resume.value.trim()
    if (!jd && !cv) return '请先粘贴目标 JD 和简历内容'
    if (meaningfulLength(jd) < MIN_JD_MEANINGFUL_CHARS) return 'JD 内容太少，请粘贴至少一段完整的岗位职责或任职要求'
    if (meaningfulLength(cv) < MIN_RESUME_MEANINGFUL_CHARS) return '简历内容太少，请至少填写一段包含项目、经历或技能的内容'
    if (!containsAny(jd, JD_SIGNALS)) return 'JD 缺少岗位职责、任职要求或技术关键词'
    if (!containsAny(cv, RESUME_SIGNALS)) return '简历缺少项目、经历、技能或技术关键词'
    return ''
  })

  const canAnalyze = computed(() => !inputValidationMessage.value && !isLoading.value)

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
    error.value = ''
    if (inputValidationMessage.value) {
      error.value = inputValidationMessage.value
      return
    }
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
    visibleSuggestions, inputValidationMessage, canAnalyze,
    analyze, selectSuggestion, dismissSuggestion, applyStarRewrite, persist,
  }
})
