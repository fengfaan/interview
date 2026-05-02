import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as api from '../api/settingsApi'
import type { PromptFile } from '../types/settings'

const PROVIDER_PRESETS = {
  zhipu: {
    label: '智谱 AI',
    docsUrl: 'https://open.bigmodel.cn/',
    docsLabel: '获取 API Key',
    modelPlaceholder: '例如 glm-4-flash',
    defaultModel: 'glm-4-flash',
    apiKeyHelp: '适合继续使用智谱的 OpenAI 兼容接口。',
  },
  openrouter: {
    label: 'OpenRouter',
    docsUrl: 'https://openrouter.ai/docs/quick-start',
    docsLabel: '查看接入文档',
    modelPlaceholder: '例如 openrouter/free 或 qwen/qwen3-coder:free',
    defaultModel: 'openrouter/free',
    apiKeyHelp: '可直接使用 OpenAI 兼容接口；免费模型更适合低频测试和原型验证。',
  },
  mimo: {
    label: '小米 MiMo',
    docsUrl: 'https://platform.xiaomimimo.com/docs/zh-CN/quickstart/first-api-call',
    docsLabel: '获取 API Key',
    modelPlaceholder: '例如 mimo-v2-flash',
    defaultModel: 'mimo-v2-flash',
    apiKeyHelp: 'Token Plan 订阅模式，API Key 格式 tp-xxxxx，Base URL: token-plan-cn.xiaomimimo.com',
  },
} as const

type ProviderKey = keyof typeof PROVIDER_PRESETS

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
  const currentProvider = ref<ProviderKey>('zhipu')
  const providerDraft = ref<ProviderKey>('zhipu')
  const currentModel = ref('')
  const defaultModel = ref('')
  const modelOptions = ref<string[]>([])
  const providerModelOptions = ref<Record<string, string[]>>({})
  const providerDefaultModels = ref<Record<string, string>>({})
  const modelDraft = ref('')
  const currentVaultPath = ref('')
  const vaultPathDraft = ref('')
  const vaultConfigured = ref(false)
  const isVaultLoading = ref(false)
  const isVaultSaving = ref(false)
  const promptFiles = ref<PromptFile[]>([])
  const selectedPromptPath = ref('')
  const promptContent = ref('')
  const promptDraft = ref('')
  const improveInstruction = ref('')
  const improvedDraft = ref('')

  const selectedPrompt = computed(() =>
    promptFiles.value.find((file) => file.path === selectedPromptPath.value) || null,
  )

  const providerOptions = computed(() =>
    Object.entries(PROVIDER_PRESETS).map(([value, meta]) => ({ value, label: meta.label })),
  )
  const currentProviderMeta = computed(() => PROVIDER_PRESETS[currentProvider.value])
  const selectedProviderMeta = computed(() => PROVIDER_PRESETS[providerDraft.value])
  const displayModelOptions = computed(() => {
    return providerModelOptions.value[providerDraft.value] || modelOptions.value
  })
  const hasPromptChanges = computed(() => promptDraft.value !== promptContent.value)
  const hasModelChanges = computed(() =>
    modelDraft.value.trim() !== currentModel.value || providerDraft.value !== currentProvider.value,
  )
  const hasVaultChanges = computed(() => vaultPathDraft.value.trim() !== currentVaultPath.value)

  async function loadApiKey() {
    await loadApiKeyForProvider(providerDraft.value)
  }

  async function loadApiKeyForProvider(provider: ProviderKey) {
    error.value = ''
    isLoading.value = true
    try {
      const res = await api.getApiKeyMasked(provider)
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
      await api.saveApiKey({ provider: providerDraft.value, apiKey: key })
      successMessage.value = `${selectedProviderMeta.value.label} API Key 保存成功`
      await loadApiKeyForProvider(providerDraft.value)
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
      currentProvider.value = (res.provider in PROVIDER_PRESETS ? res.provider : 'zhipu') as ProviderKey
      providerDraft.value = currentProvider.value
      currentModel.value = res.model
      defaultModel.value = res.defaultModel
      modelOptions.value = res.options
      providerModelOptions.value = {
        ...providerModelOptions.value,
        [currentProvider.value]: res.options,
      }
      providerDefaultModels.value = {
        ...providerDefaultModels.value,
        [currentProvider.value]: res.defaultModel,
      }
      modelDraft.value = res.model
      await loadApiKeyForProvider(currentProvider.value)
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
      const res = await api.saveModel({ provider: providerDraft.value, model })
      currentProvider.value = (res.provider in PROVIDER_PRESETS ? res.provider : 'zhipu') as ProviderKey
      providerDraft.value = currentProvider.value
      currentModel.value = res.model
      defaultModel.value = res.defaultModel
      modelOptions.value = res.options
      modelDraft.value = res.model
      successMessage.value = 'AI 渠道与模型已保存，后续 AI 请求会使用新配置'
      await loadApiKeyForProvider(currentProvider.value)
    } catch (e: any) {
      error.value = e.message || '保存模型失败'
    } finally {
      isModelSaving.value = false
    }
  }

  function selectProvider(provider: string) {
    if (!(provider in PROVIDER_PRESETS)) return
    const next = provider as ProviderKey
    const prevOptions = providerModelOptions.value[providerDraft.value] || modelOptions.value
    const nextDefaultModel = providerDefaultModels.value[next] || PROVIDER_PRESETS[next].defaultModel
    const normalizedModel = modelDraft.value.trim()
    providerDraft.value = next

    const shouldReplaceModel =
      !normalizedModel ||
      normalizedModel === currentModel.value ||
      normalizedModel === (providerDefaultModels.value[currentProvider.value] || defaultModel.value) ||
      prevOptions.some((option) => option === normalizedModel)

    if (shouldReplaceModel) {
      modelDraft.value = nextDefaultModel
    }

    void loadModelOptionsForProvider(next, shouldReplaceModel)
    void loadApiKeyForProvider(next)
  }

  async function loadModelOptionsForProvider(provider: ProviderKey, applyDefault: boolean) {
    try {
      const res = await api.getModel(provider)
      providerModelOptions.value = {
        ...providerModelOptions.value,
        [provider]: res.options,
      }
      providerDefaultModels.value = {
        ...providerDefaultModels.value,
        [provider]: res.defaultModel,
      }
      if (applyDefault && providerDraft.value === provider) {
        modelDraft.value = res.defaultModel
      }
    } catch (e: any) {
      error.value = e.message || '加载模型选项失败'
    }
  }

  async function loadVaultConfig() {
    error.value = ''
    isVaultLoading.value = true
    try {
      const res = await api.getVaultConfig()
      vaultConfigured.value = res.configured
      currentVaultPath.value = res.path || ''
      vaultPathDraft.value = res.path || ''
    } catch (e: any) {
      error.value = e.message || '加载 Vault 配置失败'
    } finally {
      isVaultLoading.value = false
    }
  }

  async function saveVaultConfig() {
    const path = vaultPathDraft.value.trim()
    if (!path) return
    error.value = ''
    successMessage.value = ''
    isVaultSaving.value = true
    try {
      const res = await api.saveVaultConfig(path)
      vaultConfigured.value = res.configured
      currentVaultPath.value = res.path || ''
      vaultPathDraft.value = res.path || ''
      successMessage.value = 'Obsidian Vault 路径已保存'
    } catch (e: any) {
      error.value = e.message || '保存 Vault 配置失败'
    } finally {
      isVaultSaving.value = false
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
    isVaultLoading, isVaultSaving,
    isPromptLoading, isPromptSaving, isPromptImproving,
    error, successMessage,
    currentProvider, providerDraft, providerOptions, currentProviderMeta, selectedProviderMeta,
    displayModelOptions,
    currentModel, defaultModel, modelOptions, modelDraft, hasModelChanges,
    currentVaultPath, vaultPathDraft, vaultConfigured, hasVaultChanges,
    promptFiles, selectedPromptPath, selectedPrompt, promptContent, promptDraft,
    improveInstruction, improvedDraft, hasPromptChanges,
    loadApiKey, saveApiKey, loadModel, saveModel, loadVaultConfig, saveVaultConfig,
    selectProvider,
    loadPrompts, selectPrompt, savePrompt,
    improvePrompt, applyImprovedDraft,
  }
})
