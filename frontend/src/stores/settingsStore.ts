import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as api from '../api/settingsApi'

export const useSettingsStore = defineStore('settings', () => {
  const maskedKey = ref('')
  const configured = ref(false)
  const isLoading = ref(false)
  const isSaving = ref(false)
  const error = ref('')
  const successMessage = ref('')

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

  return {
    maskedKey, configured, isLoading, isSaving, error, successMessage,
    loadApiKey, saveApiKey,
  }
})
