<template>
  <Transition name="drawer">
    <div
      v-if="store.isOpen"
      class="fixed inset-0 z-50 flex justify-end"
    >
      <div class="absolute inset-0 bg-black/30" @click="store.closeDeepDive()"></div>
      <div class="relative w-full max-w-lg bg-surface-container-lowest flex flex-col shadow-2xl">
        <!-- Header -->
        <div class="flex items-center justify-between px-5 py-4 border-b border-outline-variant/20 bg-surface-container-low">
          <div class="flex items-center gap-2 min-w-0">
            <span class="material-symbols-outlined text-primary text-xl">forum</span>
            <h3 class="font-headline font-bold text-on-surface text-sm truncate">深度追问</h3>
          </div>
          <button
            @click="store.closeDeepDive()"
            class="text-on-surface-variant hover:text-on-surface p-1 rounded-lg hover:bg-surface-container-highest transition-colors"
          >
            <span class="material-symbols-outlined text-xl">close</span>
          </button>
        </div>

        <!-- Context Summary -->
        <div class="px-5 py-3 bg-surface-container-low border-b border-outline-variant/10">
          <p class="text-xs text-on-surface-variant font-label mb-1">
            当前题目 · {{ store.sourceType === 'RECOMMENDED_ANSWER' ? '基于推荐答案' : '基于反馈点评' }}
          </p>
          <p class="text-sm text-on-surface line-clamp-2">{{ questionText }}</p>
        </div>

        <!-- Chat Messages -->
        <div ref="chatContainer" class="flex-1 overflow-y-auto px-5 py-4 space-y-4">
          <div
            v-for="(msg, i) in store.messages"
            :key="i"
            class="flex"
            :class="msg.role === 'USER' ? 'justify-end' : 'justify-start'"
          >
            <div
              class="max-w-[85%] rounded-xl px-4 py-3 text-sm overflow-hidden min-w-0"
              :class="msg.role === 'USER'
                ? 'bg-primary text-on-primary rounded-br-sm'
                : 'bg-surface-container-high text-on-surface rounded-bl-sm'"
            >
              <div v-if="msg.role === 'ASSISTANT'" class="markdown-body" v-html="renderMarkdown(msg.content)"></div>
              <div v-else>{{ msg.content }}</div>
            </div>
          </div>

          <!-- Agent step info -->
          <div v-if="store.agentStepInfo"
               class="mx-4 mb-2 px-3 py-2 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg text-sm text-blue-700 dark:text-blue-300">
            已检索知识库「{{ store.agentStepInfo.keyword }}」：
            {{ store.agentStepInfo.notes.join('、') }}
          </div>

          <!-- Streaming message -->
          <div v-if="store.isStreaming" class="flex justify-start">
            <div class="max-w-[85%] bg-surface-container-high text-on-surface rounded-xl rounded-bl-sm px-4 py-3 text-sm overflow-hidden min-w-0">
              <div v-if="store.streamingContent" class="markdown-body" v-html="renderMarkdown(store.streamingContent)"></div>
              <div v-else class="flex items-center gap-2 text-on-surface-variant">
                <span class="material-symbols-outlined animate-spin text-sm">progress_activity</span>
                思考中...
              </div>
            </div>
          </div>
        </div>

        <!-- Input Area -->
        <div class="px-5 py-4 border-t border-outline-variant/20 bg-surface-container-low">
          <div class="flex items-end gap-3">
            <textarea
              v-model="store.inputText"
              :disabled="store.isStreaming"
              rows="2"
              class="flex-1 bg-surface-container-highest text-on-surface rounded-xl px-4 py-3 text-sm resize-none focus:ring-0 outline-none placeholder-on-surface-variant/50"
              placeholder="输入追问..."
              @keydown="handleKeydown"
            ></textarea>
            <button
              @click="handleSend"
              :disabled="store.isStreaming || !store.inputText.trim()"
              class="bg-primary text-on-primary rounded-xl p-3 hover:opacity-90 transition-opacity disabled:opacity-40 flex-shrink-0"
            >
              <span class="material-symbols-outlined text-xl">send</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue'
import { useDeepDiveStore } from '../stores/deepDiveStore'
import { useInterviewStore } from '../stores/interviewStore'
import { renderMarkdown } from '../utils/markdown'

const store = useDeepDiveStore()
const interviewStore = useInterviewStore()
const chatContainer = ref<HTMLElement>()

const questionText = computed(() => interviewStore.currentQuestion?.question ?? '')
const expectedKeywords = computed(() => interviewStore.currentQuestion?.expectedKeywords ?? [])

function handleSend() {
  if (!store.inputText.trim() || store.isStreaming) return
  store.sendMessage(store.inputText, questionText.value, expectedKeywords.value)
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
    e.preventDefault()
    handleSend()
  }
}

watch(
  () => [store.messages.length, store.isStreaming, store.streamingContent],
  () => nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  }),
)
</script>

<style scoped>
.drawer-enter-active,
.drawer-leave-active {
  transition: transform 0.3s ease;
}
.drawer-enter-from,
.drawer-leave-to {
  transform: translateX(100%);
}
</style>
