<template>
  <div class="min-h-screen flex flex-col">
    <header class="sticky top-0 z-10 bg-surface-container-low/80 backdrop-blur-xl border-b border-outline-variant/20 px-8 py-4">
      <div class="flex items-center justify-between">
        <h2 class="text-xl font-extrabold text-on-surface font-headline flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">cloud_download</span>
          网页抓题
        </h2>
        <button
          v-if="store.parsedQuestions.length"
          @click="store.reset()"
          class="bg-surface-container-high hover:bg-surface-container-highest text-on-surface font-medium rounded-lg px-4 py-2 transition-colors flex items-center gap-2 text-sm"
        >
          <span class="material-symbols-outlined text-base">refresh</span>
          重新开始
        </button>
      </div>
    </header>

    <!-- Input Area -->
    <div class="px-8 py-6">
      <div class="max-w-4xl mx-auto">
        <!-- Tab switch -->
        <div class="flex gap-2 mb-5">
          <button
            @click="store.inputMode = 'url'"
            class="px-4 py-2 rounded-lg text-sm font-medium transition-colors"
            :class="store.inputMode === 'url' ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant'"
          >URL 抓取</button>
          <button
            @click="store.inputMode = 'paste'"
            class="px-4 py-2 rounded-lg text-sm font-medium transition-colors"
            :class="store.inputMode === 'paste' ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant'"
          >粘贴文本</button>
        </div>

        <!-- URL mode -->
        <div v-if="store.inputMode === 'url'" class="space-y-3">
          <div class="flex gap-3">
            <input
              v-model="store.url"
              type="url"
              placeholder="输入面试题网页 URL，如 https://juejin.cn/post/..."
              class="flex-1 bg-surface-container-lowest text-on-surface border border-outline-variant/30 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-primary outline-none"
              @keyup.enter="store.doCapture()"
            />
            <button
              @click="store.doCapture()"
              :disabled="store.isCapturing || !store.url.trim()"
              class="bg-primary text-on-primary font-medium rounded-xl px-6 py-3 shadow-sm hover:opacity-90 transition-opacity disabled:opacity-50 flex items-center gap-2 text-sm"
            >
              <span v-if="store.isCapturing" class="material-symbols-outlined animate-spin text-base">progress_activity</span>
              <span v-else class="material-symbols-outlined text-base">search</span>
              抓取
            </button>
          </div>
          <div v-if="store.captureError" class="bg-error-container text-on-error-container px-4 py-3 rounded-xl text-sm">
            {{ store.captureError }}
          </div>
          <div v-if="store.capturedContent && !store.isCapturing" class="bg-surface-container-lowest rounded-xl p-4">
            <div class="flex items-center justify-between mb-2">
              <span class="text-sm font-semibold text-on-surface">{{ store.capturedTitle }}</span>
              <button @click="showRaw = !showRaw" class="text-xs text-primary underline">
                {{ showRaw ? '收起' : '查看原文' }}
              </button>
            </div>
            <div v-if="showRaw" class="text-xs text-on-surface-variant max-h-40 overflow-y-auto whitespace-pre-wrap">{{ store.capturedContent }}</div>
          </div>
        </div>

        <!-- Paste mode -->
        <div v-if="store.inputMode === 'paste'">
          <textarea
            v-model="store.pastedContent"
            class="w-full min-h-[200px] bg-surface-container-lowest text-on-surface border border-outline-variant/30 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-primary outline-none resize-y"
            placeholder="粘贴面试题网页内容..."
          ></textarea>
        </div>

        <!-- Parse button -->
        <div class="mt-5">
          <button
            @click="store.doParse()"
            :disabled="store.isParsing || !store.contentToParse.trim()"
            class="bg-gradient-to-r from-primary to-primary-container text-on-primary font-label font-medium rounded-xl py-3 px-8 shadow-md hover:opacity-90 transition-opacity disabled:opacity-50 flex items-center gap-2"
          >
            <span v-if="store.isParsing" class="material-symbols-outlined animate-spin text-base">progress_activity</span>
            <span v-else class="material-symbols-outlined text-base">psychology</span>
            {{ store.isParsing ? (store.parseProgress || '解析中...') : '解析面试题' }}
          </button>
        </div>
        <div v-if="store.parseError" class="bg-error-container text-on-error-container px-4 py-3 rounded-xl text-sm mt-3">
          {{ store.parseError }}
        </div>
        <div v-if="store.saveError" class="bg-error-container text-on-error-container px-4 py-3 rounded-xl text-sm mt-3">
          {{ store.saveError }}
        </div>
      </div>
    </div>

    <!-- Parsed Results (hidden when consolidated result is showing) -->
    <div v-if="store.parsedQuestions.length && !store.consolidatedResult" class="flex-1 px-8 pb-8">
      <div class="max-w-4xl mx-auto">
        <div class="flex items-center justify-between mb-4">
          <span class="text-sm text-on-surface-variant">共解析 {{ store.parsedQuestions.length }} 题，已选 {{ store.selectedIds.size }} 题</span>
          <div class="flex gap-2">
            <button @click="store.selectAll()" class="text-xs text-primary hover:underline">全选</button>
            <button @click="store.deselectAll()" class="text-xs text-on-surface-variant hover:underline">取消全选</button>
          </div>
        </div>

        <div class="space-y-3">
          <div
            v-for="(q, index) in store.parsedQuestions" :key="index"
            class="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden"
          >
            <!-- Editing mode -->
            <div v-if="store.editingIndex === index" class="px-6 py-4 space-y-3">
              <input v-model="store.editDraft!.question" class="w-full bg-surface-container-low text-on-surface border border-outline-variant/30 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary" />
              <textarea v-model="store.editDraft!.answer" class="w-full min-h-[100px] bg-surface-container-low text-on-surface border border-outline-variant/30 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary resize-y"></textarea>
              <div class="flex gap-2 justify-end">
                <button @click="store.cancelEdit()" class="text-sm text-on-surface-variant px-3 py-1.5 rounded-lg hover:bg-surface-container-high">取消</button>
                <button @click="store.saveEdit()" class="text-sm bg-primary text-on-primary px-4 py-1.5 rounded-lg">保存</button>
              </div>
            </div>
            <!-- Display mode -->
            <div v-else class="px-6 py-4 flex items-start gap-3">
              <input
                type="checkbox"
                :checked="store.selectedIds.has(index)"
                @change="store.toggleSelect(index)"
                class="mt-1 w-4 h-4 accent-primary"
              />
              <div class="flex-1 min-w-0">
                <div class="font-medium text-on-surface text-sm">{{ q.question }}</div>
                <div v-if="q.keywords.length" class="flex flex-wrap gap-1.5 mt-2">
                  <span v-for="kw in q.keywords" :key="kw" class="bg-primary-fixed/40 text-on-primary-fixed px-2 py-0.5 rounded text-xs">{{ kw }}</span>
                </div>
                <div v-if="q.answer" class="text-xs text-on-surface-variant mt-2 line-clamp-2">{{ q.answer }}</div>
              </div>
              <div class="flex items-center gap-1">
                <button @click="store.startEdit(index)" class="text-on-surface-variant hover:text-on-surface p-1.5 rounded transition-colors" title="编辑">
                  <span class="material-symbols-outlined text-base">edit</span>
                </button>
                <button @click="store.removeQuestion(index)" class="text-on-surface-variant hover:text-error p-1.5 rounded transition-colors" title="删除">
                  <span class="material-symbols-outlined text-base">delete</span>
                </button>
              </div>
            </div>
            <!-- Save result indicator -->
            <div v-if="getSaveResult(index)" class="px-6 py-2 text-xs"
                 :class="getSaveResult(index)!.success ? 'bg-primary-container text-on-primary-container' : 'bg-error-container text-on-error-container'">
              {{ getSaveResult(index)!.success ? '已保存' : '保存失败: ' + getSaveResult(index)!.error }}
            </div>
          </div>
        </div>

        <!-- Import button -->
        <div class="mt-6 flex items-center gap-4">
          <button
            @click="store.doSave()"
            :disabled="store.isSaving || store.selectedIds.size === 0"
            class="bg-gradient-to-r from-primary to-primary-container text-on-primary font-label font-medium rounded-xl py-3 px-8 shadow-md hover:opacity-90 transition-opacity disabled:opacity-50 flex items-center gap-2"
          >
            <span v-if="store.isSaving" class="material-symbols-outlined animate-spin text-base">progress_activity</span>
            <span v-else class="material-symbols-outlined text-base">bookmark_add</span>
            {{ store.isSaving ? '导入中...' : `导入 ${store.selectedIds.size} 题到知识库` }}
          </button>

          <!-- AI 清洗并合并保存 -->
          <button
            v-if="store.parsedQuestions.length > 0 && !store.isConsolidating && !store.consolidatedResult"
            @click="store.doConsolidate()"
            :disabled="store.selectedIds.size === 0 || store.isSaving"
            class="bg-purple-600 text-white font-label font-medium rounded-xl py-3 px-8 shadow-md hover:bg-purple-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
          >
            <span class="material-symbols-outlined text-base">auto_fix_high</span>
            AI 清洗并合并保存 ({{ store.selectedIds.size }} 题)
          </button>

          <span v-if="store.saveResults.size" class="text-sm text-on-surface-variant">
            {{ store.savedCount }} / {{ store.saveResults.size }} 题保存成功
          </span>
        </div>

        <!-- 清洗进度 -->
        <div v-if="store.isConsolidating" class="mt-4 flex items-center gap-2 text-purple-600">
          <svg class="animate-spin h-4 w-4" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
          <span>{{ store.consolidateProgress || '正在 AI 清洗整理...' }}</span>
        </div>
        <div v-if="store.consolidateError" class="mt-4 text-red-600 text-sm">
          {{ store.consolidateError }}
        </div>
      </div>
    </div>

    <!-- 清洗结果预览 -->
    <div v-if="store.consolidatedResult" class="flex-1 px-8 pb-8">
      <div class="max-w-4xl mx-auto mt-4 space-y-4">
        <div class="bg-purple-50 border border-purple-200 rounded-lg p-3">
          <div class="flex items-center justify-between">
            <span class="text-sm text-purple-700">
              清洗完成：原始 {{ store.selectedIds.size }} 题 → 整理后 {{ store.consolidatedResult.totalCount }} 题
              <span v-if="store.consolidatedResult.dedupCount > 0">
                （去重 {{ store.consolidatedResult.dedupCount }} 题）
              </span>
              ，分为 {{ store.consolidatedResult.categories.length }} 个分类
            </span>
            <div class="flex gap-2">
              <button
                @click="store.consolidatedResult = null"
                class="text-sm text-gray-500 hover:text-gray-700"
              >
                取消
              </button>
              <button
                @click="store.doConsolidatedSave()"
                :disabled="store.isSaving"
                class="px-3 py-1 bg-green-600 text-white text-sm rounded hover:bg-green-700 disabled:opacity-50"
              >
                确认保存
              </button>
            </div>
          </div>
        </div>

        <!-- 按分类展示 -->
        <div v-for="(category, ci) in store.consolidatedResult.categories" :key="ci" class="space-y-2">
          <h3 class="text-md font-semibold text-gray-700 border-b pb-1">{{ category.name }}</h3>
          <div v-for="(item, ii) in category.items" :key="ii"
            class="bg-white border rounded-lg p-3 text-sm">
            <div class="font-medium">{{ item.question }}</div>
            <div v-if="item.answer" class="mt-2 text-gray-600 whitespace-pre-line">{{ item.answer }}</div>
          </div>
        </div>

        <!-- 保存结果 -->
        <div v-if="store.consolidatedSaved && store.consolidatedSaveResult" class="bg-green-50 border border-green-200 rounded-lg p-3">
          <span class="text-green-700 text-sm">
            已保存 {{ store.consolidatedSaveResult.questionCount }} 道题到知识库
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useImportStore } from '../stores/importStore'
import type { ImportSaveResult } from '../types/import'

const store = useImportStore()
const showRaw = ref(false)

function getSaveResult(index: number): ImportSaveResult | undefined {
  return store.saveResults.get(index)
}
</script>
