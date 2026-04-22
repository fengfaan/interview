import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as api from '../api/settingsApi'
import type { PromptFile } from '../types/settings'

export const useSettingsStore = defineStore('settings', () => {
  const maskedKey = ref('')
  const configured = ref(false)
  const isLoading = ref(false)
  const isSaving = ref(false)
  const isModelLoading = ref(false)
  const isModelSaving = ref(false)
  const isPromptLoading = ref(false)
  const isPromptSaving = ref(false)
  const isPromptImproving = ref(false)
  const error = ref('')
  const successMessage = ref('')
  const currentModel = ref('')
  const defaultModel = ref('')
  const modelOptions = ref<string[]>([])
  const modelDraft = ref('')
  const promptFiles = ref<PromptFile[]>([])
  const selectedPromptPath = ref('')
  const promptContent = ref('')
  const promptDraft = ref('')
  const improveInstruction = ref('')
  const improvedDraft = ref('')

  const selectedPrompt = computed(() =>
    promptFiles.value.find((file) => file.path === selectedPromptPath.value) || null,
  )

  const hasPromptChanges = computed(() => promptDraft.value !== promptContent.value)
  const hasModelChanges = computed(() => modelDraft.value.trim() !== currentModel.value)

  async function loadApiKey() {
    error.value = ''
    isLoading.value = true
    try {
      const res = await api.getApiKeyMasked()
      maskedKey.value = res.masked
      configured.value = res.configured
    } catch (e: any) {
      error.value = e.message || '加载设置失败'
    } finally {
      isLoading.value = false
    }
  }

  async function saveApiKey(key: string) {
    error.value = ''
    successMessage.value = ''
    isSaving.value = true
    try {
      await api.saveApiKey({ apiKey: key })
      successMessage.value = 'API Key 保存成功，已立即生效'
      await loadApiKey()
    } catch (e: any) {
      error.value = e.message || '保存失败'
    } finally {
      isSaving.value = false
    }
  }

  async function loadModel() {
    error.value = ''
    isModelLoading.value = true
    try {
      const res = await api.getModel()
      currentModel.value = res.model
      defaultModel.value = res.defaultModel
      modelOptions.value = res.options
      modelDraft.value = res.model
    } catch (e: any) {
      error.value = e.message || '加载模型设置失败'
    } finally {
      isModelLoading.value = false
    }
  }

  async function saveModel() {
    const model = modelDraft.value.trim()
    if (!model) return
    error.value = ''
    successMessage.value = ''
    isModelSaving.value = true
    try {
      const res = await api.saveModel({ model })
      currentModel.value = res.model
      defaultModel.value = res.defaultModel
      modelOptions.value = res.options
      modelDraft.value = res.model
      successMessage.value = '模型设置已保存，后续 AI 请求会使用新模型'
    } catch (e: any) {
      error.value = e.message || '保存模型失败'
    } finally {
      isModelSaving.value = false
    }
  }

  async function loadPrompts() {
    error.value = ''
    isPromptLoading.value = true
    try {
      promptFiles.value = await api.listPrompts()
      if (!selectedPromptPath.value && promptFiles.value.length) {
        await selectPrompt(promptFiles.value[0].path)
      }
    } catch (e: any) {
      error.value = e.message || '加载提示词失败'
    } finally {
      isPromptLoading.value = false
    }
  }

  async function selectPrompt(path: string) {
    error.value = ''
    successMessage.value = ''
    selectedPromptPath.value = path
    improvedDraft.value = ''
    isPromptLoading.value = true
    try {
      const res = await api.getPromptContent(path)
      promptContent.value = res.content
      promptDraft.value = res.content
    } catch (e: any) {
      error.value = e.message || '读取提示词失败'
    } finally {
      isPromptLoading.value = false
    }
  }

  async function savePrompt() {
    if (!selectedPromptPath.value || !promptDraft.value.trim()) return
    error.value = ''
    successMessage.value = ''
    isPromptSaving.value = true
    try {
      const res = await api.savePromptContent({
        path: selectedPromptPath.value,
        content: promptDraft.value,
      })
      promptContent.value = res.content
      promptDraft.value = res.content
      successMessage.value = '提示词已保存，下一次 AI 请求会使用新版本'
      await loadPrompts()
    } catch (e: any) {
      error.value = e.message || '保存提示词失败'
    } finally {
      isPromptSaving.value = false
    }
  }

  async function improvePrompt() {
    if (!selectedPromptPath.value || !promptDraft.value.trim()) return
    error.value = ''
    successMessage.value = ''
    improvedDraft.value = ''
    isPromptImproving.value = true
    try {
      const res = await api.improvePrompt({
        path: selectedPromptPath.value,
        content: promptDraft.value,
        instruction: improveInstruction.value,
      })
      improvedDraft.value = res.content.trim()
      successMessage.value = 'AI 已生成优化草稿，请确认后再应用或保存'
    } catch (e: any) {
      error.value = e.message || 'AI 优化失败'
    } finally {
      isPromptImproving.value = false
    }
  }

  function applyImprovedDraft() {
    if (!improvedDraft.value.trim()) return
    promptDraft.value = improvedDraft.value
    improvedDraft.value = ''
    successMessage.value = '优化草稿已应用到编辑区，保存后才会正式生效'
  }

  return {
    maskedKey, configured, isLoading, isSaving,
    isModelLoading, isModelSaving,
    isPromptLoading, isPromptSaving, isPromptImproving,
    error, successMessage,
    currentModel, defaultModel, modelOptions, modelDraft, hasModelChanges,
    promptFiles, selectedPromptPath, selectedPrompt, promptContent, promptDraft,
    improveInstruction, improvedDraft, hasPromptChanges,
    loadApiKey, saveApiKey, loadModel, saveModel, loadPrompts, selectPrompt, savePrompt,
    improvePrompt, applyImprovedDraft,
  }
})
