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

      <div class="grid grid-cols-1 xl:grid-cols-2 gap-6">
        <!-- API Key Card -->
        <section class="bg-surface-container-lowest p-6 rounded-xl shadow-sm">
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
        </section>

        <!-- Model Card -->
        <section class="bg-surface-container-lowest p-6 rounded-xl shadow-sm">
          <div class="flex items-center gap-3 mb-1">
            <span class="material-symbols-outlined text-primary">model_training</span>
            <h3 class="font-headline font-bold text-on-surface">模型选择</h3>
          </div>
          <p class="text-sm text-on-surface-variant mb-5 ml-9">
            只切换当前使用的模型，不会修改 API Key。可选择预设，也可以输入自定义模型 ID。
          </p>

          <label class="text-xs font-label text-on-surface-variant block mb-2">当前模型</label>
          <input
            v-model="store.modelDraft"
            list="model-options"
            placeholder="例如 glm-4-flash"
            class="w-full bg-surface-container-high text-on-surface border-none rounded-lg px-4 py-3 text-sm focus:ring-2 focus:ring-primary outline-none"
          />
          <datalist id="model-options">
            <option v-for="model in store.modelOptions" :key="model" :value="model" />
          </datalist>

          <div class="flex flex-wrap gap-2 mt-3">
            <button
              v-for="model in store.modelOptions"
              :key="model"
              @click="store.modelDraft = model"
              class="text-xs px-3 py-1.5 rounded-full transition-colors"
              :class="store.modelDraft === model ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant hover:text-on-surface'"
            >
              {{ model }}
            </button>
          </div>

          <div class="flex items-center justify-between mt-4">
            <span class="text-xs text-on-surface-variant">
              已生效: {{ store.currentModel || store.defaultModel || '-' }}
            </span>
            <button
              @click="store.saveModel()"
              :disabled="store.isModelSaving || !store.modelDraft.trim() || !store.hasModelChanges"
              class="bg-primary text-on-primary font-label font-medium rounded-lg px-6 py-2.5 shadow-md hover:opacity-90 transition-opacity flex items-center gap-2 disabled:opacity-50"
            >
              <span v-if="store.isModelSaving" class="material-symbols-outlined animate-spin text-sm">progress_activity</span>
              <span v-else class="material-symbols-outlined text-sm">save</span>
              {{ store.isModelSaving ? '保存中...' : '保存模型' }}
            </button>
          </div>
        </section>

        <!-- Obsidian Vault Card -->
        <section class="bg-surface-container-lowest p-6 rounded-xl shadow-sm xl:col-span-2">
          <div class="flex items-center gap-3 mb-1">
            <span class="material-symbols-outlined text-primary">folder_open</span>
            <h3 class="font-headline font-bold text-on-surface">Obsidian Vault</h3>
          </div>
          <p class="text-sm text-on-surface-variant mb-5 ml-9">
            配置本地 Obsidian Vault 目录，面试反馈和推荐答案会保存到其中的「面试知识库」文件夹。
          </p>

          <label class="text-xs font-label text-on-surface-variant block mb-2">Vault 绝对路径</label>
          <input
            v-model="store.vaultPathDraft"
            placeholder="/Users/yourname/Documents/ObsidianVault"
            class="w-full bg-surface-container-high text-on-surface border-none rounded-lg px-4 py-3 text-sm focus:ring-2 focus:ring-primary outline-none"
          />

          <div class="flex flex-wrap items-center justify-between gap-3 mt-4">
            <span class="text-xs text-on-surface-variant">
              {{ store.vaultConfigured ? `已配置: ${store.currentVaultPath}` : '尚未配置 Vault 路径' }}
            </span>
            <button
              @click="store.saveVaultConfig()"
              :disabled="store.isVaultSaving || !store.vaultPathDraft.trim() || !store.hasVaultChanges"
              class="bg-primary text-on-primary font-label font-medium rounded-lg px-6 py-2.5 shadow-md hover:opacity-90 transition-opacity flex items-center gap-2 disabled:opacity-50"
            >
              <span v-if="store.isVaultSaving" class="material-symbols-outlined animate-spin text-sm">progress_activity</span>
              <span v-else class="material-symbols-outlined text-sm">save</span>
              {{ store.isVaultSaving ? '保存中...' : '保存 Vault' }}
            </button>
          </div>
        </section>
      </div>

      <!-- Prompt Manager -->
      <section class="mt-8 bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden">
        <div class="px-6 py-5 bg-surface-container-low flex items-start justify-between gap-4">
          <div>
            <div class="flex items-center gap-3">
              <span class="material-symbols-outlined text-primary">tune</span>
              <h3 class="font-headline font-bold text-on-surface">提示词管理</h3>
            </div>
            <p class="text-sm text-on-surface-variant mt-1 ml-9">
              查看、编辑并用 AI 优化后端提示词。保存后下一次 AI 请求立即生效。
            </p>
          </div>
          <button
            @click="store.loadPrompts()"
            :disabled="store.isPromptLoading"
            class="text-sm font-medium text-on-surface-variant hover:text-on-surface px-3 py-2 rounded-lg hover:bg-surface-container-high transition-colors disabled:opacity-50 flex items-center gap-1"
          >
            <span class="material-symbols-outlined text-base" :class="{ 'animate-spin': store.isPromptLoading }">refresh</span>
            刷新
          </button>
        </div>

        <div class="grid grid-cols-1 xl:grid-cols-[280px_minmax(0,1fr)]">
          <aside class="bg-surface-container-low p-4 xl:min-h-[680px]">
            <div v-if="store.isPromptLoading && !store.promptFiles.length" class="text-sm text-primary flex items-center gap-2">
              <span class="material-symbols-outlined animate-spin text-base">progress_activity</span>
              加载提示词...
            </div>
            <div v-else class="space-y-2">
              <button
                v-for="file in store.promptFiles"
                :key="file.path"
                @click="store.selectPrompt(file.path)"
                class="w-full text-left px-3 py-3 rounded-lg transition-colors"
                :class="store.selectedPromptPath === file.path ? 'bg-primary text-on-primary shadow-sm' : 'hover:bg-surface-container-high text-on-surface'"
              >
                <div class="text-xs uppercase tracking-wide opacity-70">{{ file.group }}</div>
                <div class="font-label font-semibold text-sm mt-0.5">{{ file.name }}</div>
                <div class="text-xs opacity-70 mt-1">{{ formatSize(file.size) }}</div>
              </button>
            </div>
          </aside>

          <main class="p-6">
            <template v-if="store.selectedPrompt">
              <div class="flex flex-wrap items-center justify-between gap-3 mb-4">
                <div>
                  <h4 class="font-headline font-bold text-on-surface">{{ store.selectedPrompt.path }}</h4>
                  <p class="text-xs text-on-surface-variant mt-1">
                    最后修改：{{ formatDate(store.selectedPrompt.lastModified) }}
                  </p>
                </div>
                <div class="flex items-center gap-2">
                  <span v-if="store.hasPromptChanges" class="text-xs text-on-surface-variant">有未保存修改</span>
                  <button
                    @click="store.selectPrompt(store.selectedPromptPath)"
                    :disabled="store.isPromptLoading"
                    class="text-sm font-medium text-on-surface-variant hover:bg-surface-container-high px-4 py-2 rounded-lg transition-colors disabled:opacity-50"
                  >
                    还原
                  </button>
                  <button
                    @click="store.savePrompt()"
                    :disabled="store.isPromptSaving || !store.hasPromptChanges || !store.promptDraft.trim()"
                    class="bg-primary text-on-primary text-sm font-semibold px-4 py-2 rounded-lg shadow-sm hover:opacity-90 transition-opacity disabled:opacity-50 flex items-center gap-2"
                  >
                    <span v-if="store.isPromptSaving" class="material-symbols-outlined animate-spin text-sm">progress_activity</span>
                    <span v-else class="material-symbols-outlined text-sm">save</span>
                    保存提示词
                  </button>
                </div>
              </div>

              <textarea
                v-model="store.promptDraft"
                class="w-full min-h-[360px] bg-surface-container-low text-on-surface border-none rounded-xl px-4 py-4 text-sm font-mono leading-relaxed focus:ring-2 focus:ring-primary outline-none resize-y"
                spellcheck="false"
              ></textarea>

              <div class="mt-6 bg-surface-container-low rounded-xl p-5">
                <div class="flex items-center gap-2 mb-3">
                  <span class="material-symbols-outlined text-primary">auto_fix_high</span>
                  <h4 class="font-headline font-bold text-on-surface">AI 优化提示词</h4>
                </div>
                <textarea
                  v-model="store.improveInstruction"
                  class="w-full min-h-[80px] bg-surface-container-lowest text-on-surface border-none rounded-lg px-4 py-3 text-sm focus:ring-2 focus:ring-primary outline-none resize-y"
                  placeholder="可选：写下优化目标，例如：更严格评分、输出更稳定 JSON、减少幻觉、保留所有变量..."
                ></textarea>
                <div class="flex justify-end mt-3">
                  <button
                    @click="store.improvePrompt()"
                    :disabled="store.isPromptImproving || !store.promptDraft.trim()"
                    class="bg-gradient-to-r from-primary to-primary-container text-on-primary text-sm font-semibold px-5 py-2.5 rounded-lg shadow-sm hover:opacity-90 transition-opacity disabled:opacity-50 flex items-center gap-2"
                  >
                    <span v-if="store.isPromptImproving" class="material-symbols-outlined animate-spin text-sm">progress_activity</span>
                    <span v-else class="material-symbols-outlined text-sm">psychology</span>
                    {{ store.isPromptImproving ? '优化中...' : '生成优化草稿' }}
                  </button>
                </div>
              </div>

              <div v-if="store.improvedDraft" class="mt-6 bg-primary-fixed/70 rounded-xl p-5">
                <div class="flex items-center justify-between gap-3 mb-3">
                  <h4 class="font-headline font-bold text-on-primary-fixed">优化草稿</h4>
                  <button
                    @click="store.applyImprovedDraft()"
                    class="bg-primary text-on-primary text-sm font-semibold px-4 py-2 rounded-lg shadow-sm hover:opacity-90 transition-opacity"
                  >
                    应用到编辑区
                  </button>
                </div>
                <pre class="whitespace-pre-wrap text-sm font-mono text-on-primary-fixed leading-relaxed">{{ store.improvedDraft }}</pre>
              </div>
            </template>

            <div v-else class="min-h-[360px] flex items-center justify-center text-center text-on-surface-variant">
              <div>
                <span class="material-symbols-outlined text-5xl mb-3 block">text_snippet</span>
                <p class="font-headline font-bold">暂无可查看的提示词</p>
                <p class="text-sm mt-2">请确认后端提示词目录已配置。</p>
              </div>
            </div>
          </main>
        </div>
      </section>
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
  store.loadModel()
  store.loadVaultConfig()
  store.loadPrompts()
})

function handleSave() {
  if (!inputKey.value.trim()) return
  store.saveApiKey(inputKey.value.trim())
  inputKey.value = ''
  showKey.value = false
}

function formatSize(size: number) {
  if (size < 1024) return `${size} B`
  return `${(size / 1024).toFixed(1)} KB`
}

function formatDate(value: string) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}
</script>
