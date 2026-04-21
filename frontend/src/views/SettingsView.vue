<template>
  <div class="min-h-screen p-8">
    <h2 class="text-xl font-extrabold text-on-surface font-headline mb-6">设置</h2>

    <!-- Loading -->
    <div v-if="store.isLoading" class="flex items-center gap-3 text-primary">
      <span class="material-symbols-outlined animate-spin">progress_activity</span>
      <span class="font-label">加载中...</span>
    </div>

    <template v-else>
      <!-- Error -->
      <div v-if="store.error" class="mb-6 bg-error-container text-on-error-container px-4 py-3 rounded-xl flex items-center gap-2">
        <span class="material-symbols-outlined text-base">error</span>
        <span class="text-sm">{{ store.error }}</span>
        <button @click="store.error = ''" class="ml-auto text-xs underline">关闭</button>
      </div>

      <!-- Success -->
      <div v-if="store.successMessage" class="mb-6 bg-secondary-container/60 text-on-secondary-container px-4 py-3 rounded-xl flex items-center gap-2">
        <span class="material-symbols-outlined text-base">check_circle</span>
        <span class="text-sm">{{ store.successMessage }}</span>
        <button @click="store.successMessage = ''" class="ml-auto text-xs underline">关闭</button>
      </div>

      <!-- API Key Card -->
      <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm max-w-xl">
        <div class="flex items-center gap-3 mb-1">
          <span class="material-symbols-outlined text-primary">key</span>
          <h3 class="font-headline font-bold text-on-surface">智谱 AI API Key</h3>
        </div>
        <p class="text-sm text-on-surface-variant mb-5 ml-9">
          配置你的智谱 AI API Key，保存后立即生效，无需重启服务。
          <a href="https://open.bigmodel.cn/" target="_blank" class="text-primary underline">获取 API Key</a>
        </p>

        <div v-if="!store.configured" class="mb-4 bg-tertiary-container/40 text-on-tertiary-container px-4 py-2.5 rounded-lg text-sm flex items-center gap-2">
          <span class="material-symbols-outlined text-base">warning</span>
          当前未配置 API Key，AI 功能不可用，请先填写。
        </div>

        <div class="relative">
          <input
            v-model="inputKey"
            :type="showKey ? 'text' : 'password'"
            :placeholder="store.configured ? store.maskedKey : '请输入智谱 AI API Key'"
            class="w-full bg-surface-container-high text-on-surface border-none rounded-lg px-4 py-3 pr-12 text-sm focus:ring-2 focus:ring-primary outline-none"
          />
          <button
            @click="showKey = !showKey"
            class="absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant hover:text-on-surface"
          >
            <span class="material-symbols-outlined text-base">{{ showKey ? 'visibility_off' : 'visibility' }}</span>
          </button>
        </div>

        <div class="flex items-center justify-between mt-4">
          <span v-if="store.configured" class="text-xs text-on-surface-variant">
            当前: {{ store.maskedKey }}
          </span>
          <span v-else class="text-xs text-on-surface-variant"></span>

          <button
            @click="handleSave"
            :disabled="store.isSaving || !inputKey.trim()"
            class="bg-gradient-to-r from-primary to-primary-container text-on-primary font-label font-medium rounded-lg px-6 py-2.5 shadow-md hover:opacity-90 transition-opacity flex items-center gap-2 disabled:opacity-50"
          >
            <span v-if="store.isSaving" class="material-symbols-outlined animate-spin text-sm">progress_activity</span>
            <span v-else class="material-symbols-outlined text-sm">save</span>
            {{ store.isSaving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useSettingsStore } from '../stores/settingsStore'

const store = useSettingsStore()
const inputKey = ref('')
const showKey = ref(false)

onMounted(() => {
  store.loadApiKey()
})

function handleSave() {
  if (!inputKey.value.trim()) return
  store.saveApiKey(inputKey.value.trim())
  inputKey.value = ''
  showKey.value = false
}
</script>
