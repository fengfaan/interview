<template>
  <div class="min-h-screen flex flex-col">
    <!-- Top Config Bar -->
    <header class="sticky top-0 z-10 bg-surface-container-low/80 backdrop-blur-xl border-b border-outline-variant/20 px-8 py-4">
      <div class="flex items-center gap-6 flex-wrap">
        <h2 class="text-xl font-extrabold text-on-surface font-headline">面试演练室</h2>

        <div class="flex items-center gap-4 bg-surface-container-lowest px-4 py-2 rounded-xl">
          <div class="flex items-center gap-2 text-sm font-label text-on-surface-variant">
            <span class="material-symbols-outlined text-base">explore</span>
            <span>方向:</span>
            <select
              v-model="store.direction"
              :disabled="store.isStarted"
              class="bg-transparent border-none text-on-surface font-medium text-sm focus:ring-0 p-0 outline-none cursor-pointer"
            >
              <option v-for="d in DIRECTIONS" :key="d.value" :value="d.value">{{ d.label }}</option>
            </select>
          </div>

          <div class="w-px h-4 bg-outline-variant/30"></div>

          <div class="flex items-center gap-2 text-sm font-label text-on-surface-variant">
            <span class="material-symbols-outlined text-base">layers</span>
            <span>题型:</span>
          </div>
          <div class="flex gap-1">
            <button
              v-for="l in LEVELS" :key="l.value"
              @click="store.level = l.value"
              :disabled="store.isStarted"
              class="px-3 py-1 rounded-lg text-xs font-medium transition-colors"
              :class="store.level === l.value
                ? 'bg-primary text-on-primary'
                : 'bg-surface-container-high text-on-surface-variant hover:bg-surface-container-highest'"
            >{{ l.label }}</button>
          </div>
        </div>

        <button
          v-if="!store.isStarted"
          @click="store.startSession()"
          :disabled="store.isLoading"
          class="bg-gradient-to-r from-primary to-primary-container text-on-primary font-label font-medium rounded-xl px-6 py-2.5 shadow-md hover:opacity-90 transition-opacity flex items-center gap-2"
        >
          <span class="material-symbols-outlined text-base">play_arrow</span>
          {{ store.isLoading ? '生成中...' : '开始模拟' }}
        </button>

        <button
          v-else
          @click="store.newSession()"
          class="bg-surface-container-high text-on-surface font-label font-medium rounded-xl px-4 py-2 hover:bg-surface-container-highest transition-colors"
        >
          结束模拟
        </button>
      </div>
    </header>

    <!-- Error Banner -->
    <div v-if="store.error" class="mx-8 mt-4 bg-error-container text-on-error-container px-4 py-3 rounded-xl flex items-center gap-2">
      <span class="material-symbols-outlined text-base">error</span>
      <span class="text-sm">{{ store.error }}</span>
      <button @click="store.error = ''" class="ml-auto text-xs underline">关闭</button>
    </div>

    <!-- Middle: Q&A Area -->
    <div v-if="store.isStarted && store.currentQuestion" class="flex-1 p-8">
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <!-- Question Card -->
        <div class="bg-surface-container-lowest p-8 rounded-xl relative shadow-sm">
          <div class="absolute top-0 left-0 w-2 h-full bg-primary rounded-l-xl"></div>
          <div class="flex items-center gap-3 mb-4 text-primary font-label text-sm font-medium">
            <span class="material-symbols-outlined bg-primary-fixed p-1.5 rounded-md text-primary text-base">record_voice_over</span>
            第 {{ store.history.length + 1 }} 题
          </div>
          <h3 class="text-2xl font-headline font-bold text-on-surface leading-tight mb-3">
            {{ store.currentQuestion.question }}
          </h3>
          <div v-if="store.currentQuestion.expectedKeywords.length" class="flex flex-wrap gap-2 mt-4">
            <span class="text-xs font-label text-on-surface-variant">考察要点:</span>
            <span
              v-for="kw in store.currentQuestion.expectedKeywords" :key="kw"
              class="bg-primary-fixed/50 text-on-primary-fixed px-2 py-0.5 rounded text-xs"
            >{{ kw }}</span>
          </div>
          <div class="mt-6 flex flex-wrap items-center gap-3">
            <button
              @click="store.generateRecommendedAnswer()"
              :disabled="store.isAnswerStreaming || store.isLoading"
              class="bg-surface-container-high hover:bg-surface-container-highest text-on-surface font-label font-medium rounded-lg px-4 py-2 transition-colors flex items-center gap-2 disabled:opacity-50"
            >
              <span v-if="store.isAnswerStreaming" class="material-symbols-outlined animate-spin text-base">progress_activity</span>
              <span v-else class="material-symbols-outlined text-base">school</span>
              {{ store.isAnswerStreaming ? '生成背题答案...' : '背题答案' }}
            </button>
            <button
              v-if="store.recommendedAnswer"
              @click="store.answerExpanded = !store.answerExpanded"
              class="text-sm text-on-surface-variant hover:text-on-surface px-3 py-2 rounded-lg hover:bg-surface-container-low transition-colors flex items-center gap-1"
            >
              <span class="material-symbols-outlined text-base">{{ store.answerExpanded ? 'expand_less' : 'expand_more' }}</span>
              {{ store.answerExpanded ? '收起答案' : '查看答案' }}
            </button>
          </div>
        </div>

        <!-- Answer Editor -->
        <div class="bg-surface-container-low rounded-xl p-2 relative flex flex-col">
          <textarea
            v-model="store.draftAnswer"
            :disabled="store.isStreaming"
            class="w-full flex-1 min-h-[200px] bg-surface-container-highest border-none rounded-lg p-6 font-body text-on-surface placeholder-on-surface-variant/50 focus:ring-0 resize-none text-base"
            placeholder="在这里输入你的回答..."
            @keydown="handleKeydown"
          ></textarea>
          <div class="flex items-center justify-between px-4 py-3">
            <span class="text-xs text-on-surface-variant">{{ store.draftAnswer.length }} / 5000</span>
            <div class="flex items-center gap-3">
              <button
                @click="store.skipQuestion()"
                :disabled="store.isLoading || store.isStreaming || store.isAnswerStreaming"
                class="text-sm text-on-surface-variant hover:text-on-surface px-3 py-2 rounded-lg hover:bg-surface-container-highest transition-colors"
              >
                跳过此题
              </button>
              <button
                @click="store.submitAnswer()"
                :disabled="store.isLoading || store.isStreaming || store.isAnswerStreaming || !store.draftAnswer.trim()"
                class="bg-gradient-to-r from-primary to-primary-container text-on-primary font-label font-medium rounded-lg px-6 py-2.5 shadow-md hover:opacity-90 transition-opacity flex items-center gap-2 disabled:opacity-50"
              >
                提交回答
                <span class="material-symbols-outlined text-sm">send</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Recommended Answer Panel -->
      <div
        v-if="store.answerExpanded"
        class="mt-8 bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden"
      >
        <div class="bg-surface-container-low px-6 py-4 flex items-center justify-between gap-4">
          <div class="flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">menu_book</span>
            <h3 class="font-headline font-bold text-on-surface">AI 推荐背题答案</h3>
          </div>
          <div class="flex items-center gap-2">
            <div v-if="store.isAnswerStreaming" class="flex items-center gap-2 text-xs font-label text-primary">
              <span class="material-symbols-outlined animate-spin text-sm">progress_activity</span>
              生成中...
            </div>
            <button
              v-if="store.recommendedAnswer"
              @click="copyRecommendedAnswer"
              class="text-sm font-medium text-on-surface-variant hover:bg-surface-container-high px-3 py-2 rounded-lg transition-colors flex items-center gap-1"
            >
              <span class="material-symbols-outlined text-base">{{ copiedAnswer ? 'check' : 'content_copy' }}</span>
              {{ copiedAnswer ? '已复制' : '复制 MD' }}
            </button>
            <button
              v-if="store.recommendedAnswer"
              @click="saveAnswer"
              :disabled="saveAnswerState !== 'idle'"
              class="text-sm font-medium text-on-surface-variant hover:bg-surface-container-high px-3 py-2 rounded-lg transition-colors flex items-center gap-1 disabled:opacity-50"
            >
              <span v-if="saveAnswerState === 'saving'" class="material-symbols-outlined text-base animate-spin">progress_activity</span>
              <span v-else-if="saveAnswerState === 'saved'" class="material-symbols-outlined text-base text-secondary">check_circle</span>
              <span v-else class="material-symbols-outlined text-base">bookmark_add</span>
              {{ saveAnswerState === 'saving' ? '保存中...' : saveAnswerState === 'saved' ? '已保存' : '保存到知识库' }}
            </button>
          </div>
        </div>
        <div class="p-6">
          <div
            v-if="store.recommendedAnswer"
            class="markdown-body text-on-surface"
            v-html="renderedRecommendedAnswer"
          ></div>
          <div v-else class="min-h-[160px] flex items-center justify-center text-on-surface-variant">
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined animate-spin text-primary">progress_activity</span>
              正在生成适合背诵的 Markdown 答案...
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty state -->
    <div v-else-if="!store.isStarted" class="flex-1 flex items-center justify-center">
      <div class="text-center text-on-surface-variant">
        <span class="material-symbols-outlined text-6xl mb-4 block">psychology</span>
        <p class="text-lg font-headline font-bold">选择方向和题型，开始模拟面试</p>
        <p class="text-sm mt-2">AI 将扮演资深面试官，为你生成真实面试题目</p>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="store.isLoading && !store.currentQuestion" class="flex-1 flex items-center justify-center">
      <div class="flex items-center gap-3 text-primary">
        <span class="material-symbols-outlined animate-spin">progress_activity</span>
        <span class="font-label">正在生成题目...</span>
      </div>
    </div>

    <!-- Bottom Feedback Drawer -->
    <div
      v-if="store.feedbackExpanded"
      class="border-t border-outline-variant/20 bg-surface-container-low"
    >
      <div class="px-8 py-6">
        <div class="flex items-center justify-between mb-6">
          <h3 class="font-headline font-bold text-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">auto_awesome</span>
            实时反馈
          </h3>
          <div v-if="store.isStreaming" class="flex items-center gap-2 text-xs font-label text-primary">
            <span class="material-symbols-outlined animate-spin text-sm">progress_activity</span>
            生成中...
          </div>
          <button @click="store.feedbackExpanded = false" class="text-on-surface-variant hover:text-on-surface">
            <span class="material-symbols-outlined text-sm">expand_more</span>
          </button>
        </div>

        <!-- Keyword Radar -->
        <div v-if="store.keywordHits" class="mb-6">
          <h4 class="text-sm font-label font-medium text-on-surface-variant mb-3 uppercase tracking-wider">关键词命中</h4>
          <div class="flex flex-wrap gap-2">
            <span
              v-for="kw in store.keywordHits.hit" :key="'hit-'+kw"
              class="bg-secondary-container/60 text-on-secondary-container px-3 py-1.5 rounded-md text-sm flex items-center gap-1.5"
            >
              <span class="material-symbols-outlined text-sm">check_circle</span>
              {{ kw }}
            </span>
            <span
              v-for="kw in store.keywordHits.miss" :key="'miss-'+kw"
              class="bg-error-container text-on-error-container px-3 py-1.5 rounded-md text-sm flex items-center gap-1.5"
            >
              <span class="material-symbols-outlined text-sm">error</span>
              {{ kw }}
            </span>
          </div>
          <div v-if="store.score !== null" class="mt-3 text-sm text-on-surface-variant">
            得分: <span class="font-bold text-primary">{{ store.score }}</span>/100
          </div>
        </div>

        <!-- Streaming Commentary -->
        <div v-if="store.commentary" class="mb-6">
          <div class="prose prose-sm max-w-none text-on-surface bg-surface-container-lowest p-6 rounded-xl" v-html="renderedCommentary"></div>
        </div>

        <!-- Follow-up Question -->
        <div v-if="!store.isStreaming && store.commentary" class="bg-surface-container-lowest p-6 rounded-xl border border-outline-variant/15">
          <div class="flex items-center justify-between">
            <div>
              <h4 class="font-headline font-bold text-on-surface mb-1">继续挑战？</h4>
              <p class="text-sm text-on-surface-variant">点击进入下一道进阶问题</p>
            </div>
            <div class="flex items-center gap-3">
              <button
                @click="saveFeedback"
                :disabled="saveFeedbackState !== 'idle'"
                class="bg-surface-container-high hover:bg-surface-container-highest text-on-surface font-label font-medium py-2.5 px-5 rounded-lg transition-colors flex items-center gap-2 disabled:opacity-50"
              >
                <span v-if="saveFeedbackState === 'saving'" class="material-symbols-outlined animate-spin text-base">progress_activity</span>
                <span v-else-if="saveFeedbackState === 'saved'" class="material-symbols-outlined text-base text-secondary">check_circle</span>
                <span v-else class="material-symbols-outlined text-base">bookmark_add</span>
                {{ saveFeedbackState === 'saving' ? '保存中...' : saveFeedbackState === 'saved' ? '已保存' : '保存到知识库' }}
              </button>
              <button
                @click="store.continueChallenge()"
                class="bg-surface-container-high hover:bg-surface-container-highest text-on-surface font-label font-medium py-2.5 px-5 rounded-lg transition-colors flex items-center gap-2"
              >
                <span class="material-symbols-outlined text-base">psychology</span>
                继续挑战
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useInterviewStore } from '../stores/interviewStore'
import { useKnowledgeStore } from '../stores/knowledgeStore'
import { DIRECTIONS, LEVELS } from '../types/interview'
import { renderMarkdown } from '../utils/markdown'

const store = useInterviewStore()
const knowledgeStore = useKnowledgeStore()
const copiedAnswer = ref(false)
const saveFeedbackState = ref<'idle' | 'saving' | 'saved'>('idle')
const saveAnswerState = ref<'idle' | 'saving' | 'saved'>('idle')

const DIRECTION_LABELS: Record<string, string> = {
  GO_BACKEND: 'Go 后端',
  REACT_FRONTEND: 'React 前端',
  SYSTEM_DESIGN: '系统设计',
  DATABASE_RELATED: '数据库相关',
  AI_CODING: 'AI Agent 开发方向',
}

const renderedCommentary = computed(() => {
  if (!store.commentary) return ''
  return renderMarkdown(store.commentary)
})

const renderedRecommendedAnswer = computed(() => {
  if (!store.recommendedAnswer) return ''
  return renderMarkdown(store.recommendedAnswer)
})

async function copyRecommendedAnswer() {
  if (!store.recommendedAnswer) return
  try {
    await navigator.clipboard.writeText(store.recommendedAnswer)
    copiedAnswer.value = true
    window.setTimeout(() => {
      copiedAnswer.value = false
    }, 1600)
  } catch {
    store.error = '复制失败，请手动选择答案内容复制'
  }
}

async function saveFeedback() {
  if (!store.currentQuestion || saveFeedbackState.value !== 'idle') return
  saveFeedbackState.value = 'saving'
  const q = store.currentQuestion
  const content = buildFeedbackContent()
  const ok = await knowledgeStore.saveNote({
    title: q.question,
    direction: DIRECTION_LABELS[store.direction] || store.direction,
    content,
    tags: q.expectedKeywords.slice(0, 5),
    questionId: q.questionId,
    source: 'interview-feedback',
  })
  saveFeedbackState.value = ok ? 'saved' : 'idle'
  if (ok) {
    window.setTimeout(() => { saveFeedbackState.value = 'idle' }, 2000)
  }
}

async function saveAnswer() {
  if (!store.currentQuestion || !store.recommendedAnswer || saveAnswerState.value !== 'idle') return
  saveAnswerState.value = 'saving'
  const q = store.currentQuestion
  const ok = await knowledgeStore.saveNote({
    title: q.question,
    direction: DIRECTION_LABELS[store.direction] || store.direction,
    content: store.recommendedAnswer,
    tags: q.expectedKeywords.slice(0, 5),
    questionId: q.questionId,
    source: 'recommended-answer',
  })
  saveAnswerState.value = ok ? 'saved' : 'idle'
  if (ok) {
    window.setTimeout(() => { saveAnswerState.value = 'idle' }, 2000)
  }
}

function buildFeedbackContent() {
  const q = store.currentQuestion!
  const lines: string[] = [`## 题目\n\n${q.question}`]
  if (store.keywordHits) {
    lines.push(`\n## 关键词命中\n\n**命中:** ${store.keywordHits.hit.join(', ') || '无'}\n\n**未命中:** ${store.keywordHits.miss.join(', ') || '无'}`)
  }
  if (store.score !== null) {
    lines.push(`\n**得分:** ${store.score}/100`)
  }
  if (store.commentary) {
    lines.push(`\n## AI 评语\n\n${store.commentary}`)
  }
  return lines.join('\n')
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
    e.preventDefault()
    store.submitAnswer()
  }
}
</script>
