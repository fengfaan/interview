<template>
  <div class="min-h-screen flex flex-col">
    <header class="sticky top-0 z-10 bg-surface-container-low/80 backdrop-blur-xl border-b border-outline-variant/20 px-8 py-4">
      <div class="flex items-center justify-between">
        <h2 class="text-xl font-extrabold text-on-surface font-headline flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">bolt</span>
          快速刷题
        </h2>
        <div v-if="store.questions.length" class="flex items-center gap-3">
          <span class="text-sm text-on-surface-variant">共 {{ store.questions.length }} 题</span>
          <button
            @click="store.saveAll()"
            :disabled="store.isLoading"
            class="bg-surface-container-high hover:bg-surface-container-highest text-on-surface font-medium rounded-lg px-4 py-2 transition-colors flex items-center gap-2 text-sm"
          >
            <span class="material-symbols-outlined text-base">bookmark_add</span>
            全部保存到知识库
          </button>
          <button
            @click="store.reset()"
            class="bg-surface-container-high hover:bg-surface-container-highest text-on-surface font-medium rounded-lg px-4 py-2 transition-colors flex items-center gap-2 text-sm"
          >
            <span class="material-symbols-outlined text-base">refresh</span>
            重新出题
          </button>
        </div>
      </div>
    </header>

    <!-- Config Panel (shown when no questions) -->
    <div v-if="!store.questions.length && !store.isLoading" class="flex-1 flex items-center justify-center">
      <div class="bg-surface-container-lowest rounded-2xl shadow-sm p-10 max-w-lg w-full">
        <div class="text-center mb-8">
          <span class="material-symbols-outlined text-5xl text-primary mb-3 block">quiz</span>
          <h3 class="text-lg font-headline font-bold text-on-surface">选择参数，快速出题</h3>
          <p class="text-sm text-on-surface-variant mt-1">快速生成题目，点击展开即可查看答案</p>
        </div>

        <div class="space-y-5">
          <div>
            <label class="text-sm font-label text-on-surface-variant mb-2 block">方向</label>
            <div class="flex flex-wrap gap-2">
              <button
                v-for="d in DIRECTIONS" :key="d.value"
                @click="store.direction = d.value"
                class="px-3 py-2 rounded-lg text-sm font-medium transition-colors"
                :class="store.direction === d.value ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant hover:bg-surface-container-highest'"
              >{{ d.label }}</button>
            </div>
          </div>

          <div>
            <label class="text-sm font-label text-on-surface-variant mb-2 block">题型</label>
            <div class="flex gap-2">
              <button
                v-for="l in LEVELS" :key="l.value"
                @click="store.level = l.value"
                class="px-3 py-2 rounded-lg text-sm font-medium transition-colors"
                :class="store.level === l.value ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant hover:bg-surface-container-highest'"
              >{{ l.label }}</button>
            </div>
          </div>

          <div>
            <label class="text-sm font-label text-on-surface-variant mb-2 block">数量</label>
            <div class="flex gap-2">
              <button
                v-for="n in store.COUNT_OPTIONS" :key="n"
                @click="store.count = n"
                class="px-4 py-2 rounded-lg text-sm font-medium transition-colors"
                :class="store.count === n ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant hover:bg-surface-container-highest'"
              >{{ n }} 题</button>
            </div>
          </div>

          <button
            @click="store.generate()"
            class="w-full bg-gradient-to-r from-primary to-primary-container text-on-primary font-label font-medium rounded-xl py-3 shadow-md hover:opacity-90 transition-opacity flex items-center justify-center gap-2 mt-4"
          >
            <span class="material-symbols-outlined text-base">play_arrow</span>
            开始出题
          </button>
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="store.isLoading && !store.questions.length" class="flex-1 flex items-center justify-center">
      <div class="text-center">
        <span class="material-symbols-outlined animate-spin text-4xl text-primary block mb-3">progress_activity</span>
        <p class="text-sm text-on-surface-variant">{{ store.progressText || '正在生成题目，请稍候...' }}</p>
        <p class="text-xs text-on-surface-variant mt-1">已生成 {{ store.generatedCount }} / {{ store.count }} 题</p>
      </div>
    </div>

    <div v-if="store.isLoading && store.questions.length" class="mx-8 mt-4 bg-primary-container text-on-primary-container px-4 py-3 rounded-xl flex items-center gap-2">
      <span class="material-symbols-outlined animate-spin text-base">progress_activity</span>
      <span class="text-sm">{{ store.progressText || '正在生成题目，请稍候...' }}</span>
      <span class="ml-auto text-xs">已生成 {{ store.generatedCount }} / {{ store.count }}</span>
    </div>

    <div v-if="!store.isLoading && store.failedBatches.length && store.questions.length" class="mx-8 mt-4 bg-secondary-container text-on-secondary-container px-4 py-3 rounded-xl flex items-center gap-2">
      <span class="material-symbols-outlined text-base">info</span>
      <span class="text-sm">已保留成功生成的题目，失败批次：第 {{ store.failedBatches.join('、') }} 批</span>
    </div>

    <!-- Error -->
    <div v-if="store.error" class="mx-8 mt-4 bg-error-container text-on-error-container px-4 py-3 rounded-xl flex items-center gap-2">
      <span class="material-symbols-outlined text-base">error</span>
      <span class="text-sm">{{ store.error }}</span>
      <button @click="store.error = ''" class="ml-auto text-xs underline">关闭</button>
    </div>

    <!-- Question List -->
    <div v-if="store.questions.length" class="flex-1 px-8 py-6">
      <div class="max-w-4xl mx-auto space-y-3">
        <div
          v-for="(q, index) in store.questions" :key="q.questionId"
          class="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden transition-all"
        >
          <!-- Question Header (clickable) -->
          <div
            @click="store.toggleExpand(q.questionId)"
            class="px-6 py-4 cursor-pointer hover:bg-surface-container-low transition-colors flex items-center gap-4"
          >
            <span class="text-sm font-bold text-primary w-8 text-right">{{ index + 1 }}</span>
            <span class="flex-1 font-medium text-on-surface">{{ q.question }}</span>
            <div class="flex items-center gap-2">
              <button
                @click.stop="handleSave(q)"
                :disabled="store.savedIds.has(q.questionId)"
                class="text-on-surface-variant hover:text-on-surface px-2 py-1 rounded transition-colors disabled:opacity-50"
                :title="store.savedIds.has(q.questionId) ? '已保存' : '保存到知识库'"
              >
                <span v-if="store.savedIds.has(q.questionId)" class="material-symbols-outlined text-base text-secondary">check_circle</span>
                <span v-else class="material-symbols-outlined text-base">bookmark_add</span>
              </button>
              <span class="material-symbols-outlined text-on-surface-variant text-base transition-transform" :class="store.isExpanded(q.questionId) ? 'rotate-180' : ''">expand_more</span>
            </div>
          </div>

          <!-- Answer (expandable) -->
          <transition name="expand">
            <div v-if="store.isExpanded(q.questionId)" class="border-t border-outline-variant/15">
              <div class="px-6 py-5">
                <div v-if="q.keywords.length" class="flex flex-wrap gap-1.5 mb-3">
                  <span
                    v-for="kw in q.keywords" :key="kw"
                    class="bg-primary-fixed/40 text-on-primary-fixed px-2 py-0.5 rounded text-xs"
                  >{{ kw }}</span>
                </div>
                <div v-if="store.isLoadingAnswer(q.questionId)" class="flex items-center gap-2 text-sm text-on-surface-variant py-2">
                  <span class="material-symbols-outlined animate-spin text-base">progress_activity</span>
                  正在生成答案...
                </div>
                <div v-else-if="store.getAnswerError(q.questionId)" class="text-sm text-error py-2">
                  {{ store.getAnswerError(q.questionId) }}
                  <button @click="store.loadAnswer(q.questionId)" class="ml-2 underline text-xs">重试</button>
                </div>
                <div v-else-if="q.answer" class="markdown-body text-on-surface text-sm" v-html="renderAnswer(q.answer)"></div>
              </div>
            </div>
          </transition>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRapidQuestionStore } from '../stores/rapidQuestionStore'
import { DIRECTIONS, LEVELS } from '../types/interview'
import type { BatchQuestionItem } from '../types/interview'
import { renderMarkdown } from '../utils/markdown'

const store = useRapidQuestionStore()

function renderAnswer(answer: string) {
  return renderMarkdown(answer)
}

async function handleSave(q: BatchQuestionItem) {
  await store.saveOne(q)
}
</script>

<style scoped>
.expand-enter-active,
.expand-leave-active {
  transition: all 0.2s ease;
  max-height: 2000px;
  overflow: hidden;
}
.expand-enter-from,
.expand-leave-to {
  max-height: 0;
  opacity: 0;
}
</style>
