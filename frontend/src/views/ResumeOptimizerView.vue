<template>
  <div class="min-h-screen flex flex-col">
    <!-- Header -->
    <header class="sticky top-0 z-10 bg-surface-container-low/80 backdrop-blur-xl border-b border-outline-variant/20 px-8 py-4">
      <div class="flex items-center justify-between">
        <h2 class="text-xl font-extrabold text-on-surface font-headline">简历调优台</h2>
        <button
          @click="store.analyze()"
          :disabled="store.isLoading || !store.jobDescription.trim() || !store.resume.trim()"
          class="bg-gradient-to-r from-primary to-primary-container text-on-primary font-label font-medium rounded-xl px-6 py-2.5 shadow-md hover:opacity-90 transition-opacity flex items-center gap-2 disabled:opacity-50"
        >
          <span class="material-symbols-outlined text-base">auto_fix_high</span>
          {{ store.isLoading ? '分析中...' : '生成报告' }}
        </button>
      </div>
    </header>

    <!-- Error Banner -->
    <div v-if="store.error" class="mx-8 mt-4 bg-error-container text-on-error-container px-4 py-3 rounded-xl flex items-center gap-2">
      <span class="material-symbols-outlined text-base">error</span>
      <span class="text-sm">{{ store.error }}</span>
      <button @click="store.error = ''" class="ml-auto text-xs underline">关闭</button>
    </div>

    <!-- Main Split View -->
    <div class="flex-1 p-8">
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <!-- Left Column: Inputs -->
        <div class="flex flex-col gap-6">
          <!-- JD Input -->
          <div class="bg-surface-container-lowest rounded-xl overflow-hidden flex flex-col">
            <div class="bg-surface-container-low px-6 py-3 flex items-center justify-between">
              <h3 class="font-headline font-bold text-on-surface">目标 JD</h3>
              <span class="text-xs text-on-surface-variant">{{ store.jobDescription.length }} / 20000</span>
            </div>
            <textarea
              v-model="store.jobDescription"
              class="w-full min-h-[200px] bg-surface-container-lowest p-6 font-body text-sm text-on-surface placeholder-on-surface-variant/50 focus:ring-0 resize-y border-none outline-none"
              placeholder="粘贴目标职位 JD 内容..."
              @keydown="handleKeydown"
            ></textarea>
          </div>

          <!-- Resume Editor -->
          <div class="bg-surface-container-lowest rounded-xl overflow-hidden flex flex-col flex-1">
            <div class="bg-surface-container-low px-6 py-3 flex items-center justify-between">
              <h3 class="font-headline font-bold text-on-surface">简历内容</h3>
              <span class="text-xs text-on-surface-variant">{{ store.resume.length }} / 20000</span>
            </div>
            <textarea
              v-model="store.resume"
              class="w-full min-h-[300px] flex-1 bg-surface-container-lowest p-6 font-body text-sm text-on-surface placeholder-on-surface-variant/50 focus:ring-0 resize-y border-none outline-none"
              placeholder="粘贴或输入简历 Markdown 内容..."
              @input="store.persist()"
            ></textarea>
          </div>
        </div>

        <!-- Right Column: Analysis Results -->
        <div class="flex flex-col gap-6">
          <template v-if="store.analysisResult">
            <!-- Score Card -->
            <div class="bg-surface-container-lowest rounded-xl p-6 shadow-sm flex items-center gap-6">
              <div class="relative w-20 h-20 flex-shrink-0">
                <svg class="w-full h-full transform -rotate-90" viewBox="0 0 36 36">
                  <path class="text-surface-container-highest stroke-current" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" fill="none" stroke-width="3" />
                  <path class="stroke-current" :class="scoreColor" :stroke-dasharray="store.analysisResult.score + ', 100'" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" fill="none" stroke-width="3" />
                </svg>
                <div class="absolute inset-0 flex items-center justify-center">
                  <span class="text-xl font-bold font-headline text-on-surface">{{ store.analysisResult.score }}%</span>
                </div>
              </div>
              <div>
                <h3 class="font-headline font-bold text-on-surface text-lg">匹配度评分</h3>
                <p class="text-sm text-on-surface-variant mt-1">{{ scoreLabel }}</p>
              </div>
            </div>

            <!-- Dimensions -->
            <div class="bg-surface-container-lowest rounded-xl p-6 shadow-sm">
              <h4 class="font-headline font-bold text-on-surface mb-4">维度分析</h4>
              <div class="space-y-4">
                <div v-for="dim in store.analysisResult.dimensions" :key="dim.name">
                  <div class="flex justify-between items-center mb-1">
                    <span class="text-sm font-medium text-on-surface">{{ dim.name }}</span>
                    <span class="text-sm font-bold" :class="dimScoreClass(dim.score)">{{ dim.score }}%</span>
                  </div>
                  <div class="w-full h-2 bg-surface-container-highest rounded-full overflow-hidden">
                    <div
                      class="h-full rounded-full transition-all"
                      :class="dimBarClass(dim.score)"
                      :style="{ width: dim.score + '%' }"
                    ></div>
                  </div>
                  <p class="text-xs text-on-surface-variant mt-1">{{ dim.reason }}</p>
                </div>
              </div>
            </div>

            <!-- Suggestions -->
            <div class="bg-surface-container-lowest rounded-xl p-6 shadow-sm">
              <div class="flex items-center gap-2 mb-4">
                <span class="material-symbols-outlined text-primary">psychology</span>
                <h4 class="font-headline font-bold text-on-surface">优化建议</h4>
              </div>
              <div class="space-y-3">
                <div
                  v-for="s in store.visibleSuggestions" :key="s.id"
                  @click="store.selectSuggestion(s)"
                  class="bg-surface-container p-4 rounded-xl cursor-pointer hover:bg-surface-container-high transition-colors border border-outline-variant/10"
                  :class="{ 'ring-2 ring-primary': store.selectedSuggestion?.id === s.id }"
                >
                  <div class="flex items-start justify-between gap-3">
                    <div>
                      <span
                        class="text-xs font-bold px-2 py-0.5 rounded-full"
                        :class="priorityClass(s.priority)"
                      >{{ s.priority }}</span>
                      <h5 class="font-semibold text-on-surface mt-2">{{ s.title }}</h5>
                      <p class="text-xs text-on-surface-variant mt-1">{{ s.reason }}</p>
                    </div>
                    <span class="material-symbols-outlined text-on-surface-variant text-sm mt-1">chevron_right</span>
                  </div>
                </div>
              </div>
              <p v-if="!store.visibleSuggestions.length" class="text-sm text-on-surface-variant text-center py-4">
                所有建议已处理
              </p>
            </div>
          </template>

          <!-- Empty State -->
          <div v-else class="flex-1 flex items-center justify-center bg-surface-container-lowest rounded-xl min-h-[400px]">
            <div class="text-center text-on-surface-variant">
              <span class="material-symbols-outlined text-5xl mb-3 block">description</span>
              <p class="font-headline font-bold">输入 JD 和简历，开始分析</p>
              <p class="text-sm mt-2">AI 将评估匹配度并提供优化建议</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Bottom STAR Workspace -->
    <div
      v-if="store.starWorkspaceExpanded"
      class="border-t-2 border-primary/20 bg-surface-container-low"
    >
      <div class="px-8 py-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="font-headline font-bold text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">edit_note</span>
            STAR 改写
          </h3>
          <div v-if="store.isStreaming" class="flex items-center gap-2 text-xs font-label text-primary">
            <span class="material-symbols-outlined animate-spin text-sm">progress_activity</span>
            生成中...
          </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <!-- Original -->
          <div class="bg-surface-container-lowest p-4 rounded-xl">
            <h4 class="text-xs font-label text-on-surface-variant uppercase tracking-wider mb-2">原始描述</h4>
            <p v-if="store.selectedSuggestion" class="text-sm text-on-surface line-through decoration-error/50">
              {{ store.selectedSuggestion.sourceText }}
            </p>
          </div>

          <!-- Rewrite -->
          <div class="bg-surface-container-lowest p-4 rounded-xl">
            <h4 class="text-xs font-label text-on-surface-variant uppercase tracking-wider mb-2">STAR 改写</h4>
            <div v-if="store.starRewrite" class="prose prose-sm max-w-none text-on-surface" v-html="renderedRewrite"></div>
            <div v-else class="text-sm text-on-surface-variant/50">等待生成...</div>
          </div>
        </div>

        <div class="flex justify-end gap-3 mt-4">
          <button
            @click="store.dismissSuggestion()"
            class="text-sm font-medium text-on-surface-variant hover:bg-surface-container-high px-4 py-2 rounded-lg transition-colors"
          >
            忽略
          </button>
          <button
            @click="store.applyStarRewrite()"
            :disabled="store.isStreaming || !store.starRewrite"
            class="text-sm font-semibold bg-primary text-on-primary px-4 py-2 rounded-lg shadow-sm hover:opacity-90 transition-opacity disabled:opacity-50"
          >
            应用到简历
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import { useResumeStore } from '../stores/resumeStore'

const store = useResumeStore()

const renderedRewrite = computed(() => {
  if (!store.starRewrite) return ''
  return marked(store.starRewrite) as string
})

const scoreColor = computed(() => {
  const s = store.analysisResult?.score ?? 0
  if (s >= 70) return 'text-secondary'
  if (s >= 40) return 'text-yellow-500'
  return 'text-error'
})

const scoreLabel = computed(() => {
  const s = store.analysisResult?.score ?? 0
  if (s >= 80) return '高度匹配目标职位'
  if (s >= 60) return '中等匹配，有提升空间'
  if (s >= 40) return '匹配度一般，需要优化'
  return '匹配度较低，建议大幅调整'
})

function dimScoreClass(score: number) {
  if (score >= 70) return 'text-secondary'
  if (score >= 40) return 'text-yellow-600'
  return 'text-error'
}

function dimBarClass(score: number) {
  if (score >= 70) return 'bg-secondary'
  if (score >= 40) return 'bg-yellow-500'
  return 'bg-error'
}

function priorityClass(priority: string) {
  if (priority === 'HIGH') return 'bg-error-container text-on-error-container'
  if (priority === 'MEDIUM') return 'bg-yellow-100 text-yellow-800'
  return 'bg-surface-container-high text-on-surface-variant'
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
    e.preventDefault()
    store.analyze()
  }
}
</script>
