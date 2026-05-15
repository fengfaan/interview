import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { AnalyzeResponse, Suggestion, StructureAnalysisResponse, HealthCheckupResponse, CheckupAnnotation } from '../types/resume'
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

  // match
  const analysisResult = ref<AnalyzeResponse | null>(null)
  const selectedSuggestion = ref<Suggestion | null>(null)
  const starRewrite = ref('')
  const starWorkspaceExpanded = ref(false)
  const isLoading = ref(false)
  const isStreaming = ref(false)
  const error = ref('')
  const dismissedSuggestions = ref<Set<string>>(new Set())

  // new features
  const activeTab = ref<'match' | 'structure' | 'polish' | 'checkup'>('match')

  function setActiveTab(tab: 'match' | 'structure' | 'polish' | 'checkup') {
    activeTab.value = tab
    error.value = ''
  }

  const structureResult = ref<{ value: StructureAnalysisResponse, actualModel: string } | null>(null)
  const isStructureAnalyzing = ref(false)

  const polishSourceText = ref('')
  const polishInput = ref('')
  const polishResult = ref('')
  const isPolishing = ref(false)
  const isImporting = ref(false)
  const importError = ref('')

  // health checkup
  const healthCheckupResult = ref<{ value: HealthCheckupResponse, actualModel: string } | null>(null)
  const isHealthChecking = ref(false)
  const expandedFunnelLayers = ref<Set<string>>(new Set(['ats']))

  const visibleSuggestions = computed(() => {
    if (!analysisResult.value) return []
    return analysisResult.value.suggestions.filter(s => !dismissedSuggestions.value.has(s.id))
  })

  const inputValidationMessage = computed(() => {
    const jd = jobDescription.value.trim()
    const cv = resume.value.trim()

    if (activeTab.value !== 'structure') {
      if (!jd && !cv) return '请先粘贴目标 JD 和简历内容'
      if (meaningfulLength(jd) < MIN_JD_MEANINGFUL_CHARS) return 'JD 内容太少，请粘贴至少一段完整的岗位职责或任职要求'
      if (!containsAny(jd, JD_SIGNALS)) return 'JD 缺少岗位职责、任职要求或技术关键词'
    } else {
      if (!cv) return '请先粘贴简历内容'
    }

    if (meaningfulLength(cv) < MIN_RESUME_MEANINGFUL_CHARS) return '简历内容太少，请至少填写一段包含项目、经历或技能的内容'
    if (!containsAny(cv, RESUME_SIGNALS)) return '简历缺少项目、经历、技能或技术关键词'
    return ''
  })

  const canAnalyze = computed(() => !inputValidationMessage.value && !isLoading.value)
  const canAnalyzeStructure = computed(() => !inputValidationMessage.value && !isStructureAnalyzing.value)
  const canHealthCheck = computed(() => {
    const cv = resume.value.trim()
    return meaningfulLength(cv) >= MIN_RESUME_MEANINGFUL_CHARS && containsAny(cv, RESUME_SIGNALS) && !isHealthChecking.value
  })

  function restore() {
    const saved = loadState(STORAGE_KEY)
    if (saved) {
      jobDescription.value = saved.jobDescription ?? ''
      resume.value = saved.resume ?? ''
      analysisResult.value = saved.analysisResult ?? null
      dismissedSuggestions.value = new Set(saved.dismissedSuggestions ?? [])
      activeTab.value = saved.activeTab ?? 'match'
      polishInput.value = saved.polishInput ?? ''
      healthCheckupResult.value = saved.healthCheckupResult ?? null
    }
  }

  function persist() {
    saveState(STORAGE_KEY, {
      jobDescription: jobDescription.value,
      resume: resume.value,
      analysisResult: analysisResult.value,
      dismissedSuggestions: [...dismissedSuggestions.value],
      activeTab: activeTab.value,
      polishInput: polishInput.value,
      healthCheckupResult: healthCheckupResult.value,
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

  async function analyzeStructure() {
    error.value = ''
    if (!resume.value || inputValidationMessage.value) {
      error.value = inputValidationMessage.value || '简历为空'
      return
    }
    isStructureAnalyzing.value = true
    structureResult.value = null
    try {
      const res = await api.analyzeStructure({ resume: resume.value })
      structureResult.value = res
    } catch (e: any) {
      error.value = e.message || '结构体检分析失败'
    } finally {
      isStructureAnalyzing.value = false
    }
  }

  function startPolish() {
    const text = polishInput.value.trim()
    if (!text) return null
    isPolishing.value = true
    polishSourceText.value = text
    polishResult.value = ''
    error.value = ''

    api.streamPolish(
      { sourceText: text, jobDescription: jobDescription.value },
      (text) => { polishResult.value += text },
      (err) => {
        error.value = err
        isPolishing.value = false
      }
    ).then(() => {
      isPolishing.value = false
    }).catch((err) => {
      error.value = err.message || '润色失败'
      isPolishing.value = false
    })
  }

  function clearStructureResult() {
    structureResult.value = null
  }

  function applyStructureRewrite(quote: string, rewrite: string) {
    if (quote && resume.value.includes(quote)) {
      resume.value = resume.value.replace(quote, rewrite)
      persist()
    }
  }

  function clearPolishResult() {
    polishResult.value = ''
  }

  async function healthCheckupAction() {
    error.value = ''
    const cv = resume.value.trim()
    if (meaningfulLength(cv) < MIN_RESUME_MEANINGFUL_CHARS) {
      error.value = '简历内容过短，请提供完整的简历'
      return
    }
    if (!containsAny(cv, RESUME_SIGNALS)) {
      error.value = '简历缺少项目、经历、技能或技术关键词'
      return
    }
    isHealthChecking.value = true
    healthCheckupResult.value = null
    try {
      const res = await api.healthCheckup({
        resume: cv,
        jobDescription: jobDescription.value || undefined,
      })
      healthCheckupResult.value = res
      expandedFunnelLayers.value = new Set(['ats'])
      persist()
    } catch (e: any) {
      error.value = e.message || '体检失败'
    } finally {
      isHealthChecking.value = false
    }
  }

  function applyAnnotationRewrite(annotation: CheckupAnnotation) {
    if (annotation.rewrite && annotation.quote && resume.value.includes(annotation.quote)) {
      resume.value = resume.value.replace(annotation.quote, annotation.rewrite)
      persist()
    }
  }

  function toggleFunnelLayer(layer: string) {
    const s = new Set(expandedFunnelLayers.value)
    if (s.has(layer)) s.delete(layer)
    else s.add(layer)
    expandedFunnelLayers.value = s
  }

  async function importFile(file: File) {
    const ext = file.name.toLowerCase()
    if (!ext.endsWith('.pdf') && !ext.endsWith('.docx')) {
      importError.value = '仅支持 PDF 和 DOCX 文件'
      return
    }
    isImporting.value = true
    importError.value = ''
    try {
      const result = await api.importFile(file)
      resume.value = result.text
      if (result.warning) {
        importError.value = result.warning
      }
      persist()
    } catch (e: any) {
      importError.value = e.message || '文件导入失败'
    } finally {
      isImporting.value = false
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
    visibleSuggestions, inputValidationMessage, canAnalyze, canAnalyzeStructure,
    activeTab, setActiveTab, structureResult, isStructureAnalyzing, polishInput, polishSourceText, polishResult, isPolishing,
    isImporting, importError,
    healthCheckupResult, isHealthChecking, canHealthCheck, expandedFunnelLayers,
    analyze, analyzeStructure, startPolish, clearStructureResult, clearPolishResult, applyStructureRewrite, importFile,
    selectSuggestion, dismissSuggestion, applyStarRewrite, persist,
    healthCheckupAction, applyAnnotationRewrite, toggleFunnelLayer,
  }
})
