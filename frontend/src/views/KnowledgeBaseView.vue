<template>
  <div class="min-h-screen flex flex-col">
    <header class="sticky top-0 z-10 bg-surface-container-low/80 backdrop-blur-xl border-b border-outline-variant/20 px-8 py-4">
      <div class="flex items-center justify-between">
        <h2 class="text-xl font-extrabold text-on-surface font-headline flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">auto_stories</span>
          知识库
        </h2>
        <div class="flex items-center gap-3">
          <div class="relative">
            <input
              v-model="store.searchKeyword"
              @keydown.enter="handleSearch"
              placeholder="搜索笔记..."
              class="bg-surface-container-highest text-on-surface rounded-lg pl-9 pr-4 py-2 text-sm focus:ring-2 focus:ring-primary/30 outline-none w-64"
            />
            <span class="material-symbols-outlined absolute left-2.5 top-1/2 -translate-y-1/2 text-on-surface-variant text-base">search</span>
            <button
              v-if="store.searchKeyword"
              @click="clearAndRefresh"
              class="absolute right-2 top-1/2 -translate-y-1/2 text-on-surface-variant hover:text-on-surface"
            >
              <span class="material-symbols-outlined text-sm">close</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Direction Filters -->
      <div class="flex gap-2 mt-3">
        <button
          v-for="d in store.DIRECTIONS" :key="d.value"
          @click="store.filterByDirection(d.value)"
          class="px-3 py-1.5 rounded-lg text-xs font-medium transition-colors"
          :class="store.selectedDirection === d.value
            ? 'bg-primary text-on-primary'
            : 'bg-surface-container-high text-on-surface-variant hover:bg-surface-container-highest'"
        >{{ d.label }}</button>
      </div>
    </header>

    <!-- Vault not configured -->
    <div v-if="!vaultConfigured" class="flex-1 flex items-center justify-center">
      <div class="text-center text-on-surface-variant max-w-md">
        <span class="material-symbols-outlined text-6xl mb-4 block">folder_off</span>
        <p class="text-lg font-headline font-bold mb-2">Obsidian Vault 未配置</p>
        <p class="text-sm mb-4">请先在设置页配置 Obsidian Vault 路径，才能使用知识库功能</p>
        <RouterLink
          to="/settings"
          class="inline-flex items-center gap-2 bg-primary text-on-primary px-4 py-2 rounded-lg font-medium hover:opacity-90 transition-opacity"
        >
          <span class="material-symbols-outlined text-base">settings</span>
          前往设置
        </RouterLink>
      </div>
    </div>

    <!-- Empty state -->
    <div v-else-if="!store.isLoading && displayNotes.length === 0 && !store.searchKeyword" class="flex-1 flex items-center justify-center">
      <div class="text-center text-on-surface-variant">
        <span class="material-symbols-outlined text-6xl mb-4 block">menu_book</span>
        <p class="text-lg font-headline font-bold mb-2">暂无笔记</p>
        <p class="text-sm">在面试练习中保存知识点吧</p>
      </div>
    </div>

    <!-- Search empty -->
    <div v-else-if="!store.isLoading && store.searchKeyword && displayNotes.length === 0" class="flex-1 flex items-center justify-center">
      <div class="text-center text-on-surface-variant">
        <span class="material-symbols-outlined text-6xl mb-4 block">search_off</span>
        <p class="text-lg font-headline font-bold mb-2">未找到匹配的笔记</p>
        <p class="text-sm">试试其他关键词</p>
      </div>
    </div>

    <!-- Note List + Detail -->
    <div v-else class="flex-1 flex">
      <!-- Note List -->
      <div class="w-80 border-r border-outline-variant/20 overflow-y-auto">
        <div v-if="store.isLoading" class="flex items-center justify-center py-12 text-primary">
          <span class="material-symbols-outlined animate-spin mr-2">progress_activity</span>
          加载中...
        </div>
        <div
          v-for="note in displayNotes" :key="note.id"
          @click="store.fetchNote(note.id)"
          class="px-5 py-4 border-b border-outline-variant/10 cursor-pointer transition-colors hover:bg-surface-container-low"
          :class="store.currentNote?.id === note.id ? 'bg-surface-container-low border-l-2 border-l-primary' : ''"
        >
          <h4 class="font-medium text-on-surface text-sm line-clamp-2 mb-1">{{ note.title }}</h4>
          <div class="flex items-center gap-2 text-xs text-on-surface-variant">
            <span v-if="note.direction" class="bg-primary-fixed/40 text-on-primary-fixed px-1.5 py-0.5 rounded">{{ note.direction }}</span>
            <span v-if="note.created">{{ formatDate(note.created) }}</span>
          </div>
          <div v-if="note.tags.length" class="flex flex-wrap gap-1 mt-2">
            <span v-for="tag in note.tags.slice(0, 3)" :key="tag" class="text-xs text-on-surface-variant bg-surface-container-highest px-1.5 py-0.5 rounded">{{ tag }}</span>
          </div>
        </div>
      </div>

      <!-- Note Detail -->
      <div class="flex-1 overflow-y-auto p-8">
        <div v-if="store.isLoading && store.currentNote === null" class="flex items-center justify-center h-full text-primary">
          <span class="material-symbols-outlined animate-spin mr-2">progress_activity</span>
        </div>
        <div v-else-if="store.currentNote" class="max-w-3xl">
          <h2 class="text-2xl font-headline font-bold text-on-surface mb-4">{{ store.currentNote.title }}</h2>
          <div class="flex items-center gap-3 mb-6 text-sm text-on-surface-variant">
            <span v-if="store.currentNote.direction" class="bg-primary-fixed/40 text-on-primary-fixed px-2 py-1 rounded">{{ store.currentNote.direction }}</span>
            <span v-if="store.currentNote.created">{{ formatDate(store.currentNote.created) }}</span>
          </div>
          <div v-if="store.currentNote.tags.length" class="flex flex-wrap gap-1 mb-6">
            <span v-for="tag in store.currentNote.tags" :key="tag" class="text-xs text-on-surface-variant bg-surface-container-highest px-2 py-1 rounded">{{ tag }}</span>
          </div>
          <div class="markdown-body text-on-surface prose prose-sm max-w-none" v-html="renderedContent"></div>
        </div>
        <div v-else class="flex items-center justify-center h-full text-on-surface-variant">
          <div class="text-center">
            <span class="material-symbols-outlined text-4xl block mb-2">note_stack</span>
            <p class="text-sm">选择左侧笔记查看详情</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Error -->
    <div v-if="store.error" class="mx-8 my-4 bg-error-container text-on-error-container px-4 py-3 rounded-xl flex items-center gap-2">
      <span class="material-symbols-outlined text-base">error</span>
      <span class="text-sm">{{ store.error }}</span>
      <button @click="store.error = ''" class="ml-auto text-xs underline">关闭</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useKnowledgeStore } from '../stores/knowledgeStore'
import { renderMarkdown } from '../utils/markdown'
import { getVaultConfig } from '../api/settingsApi'

const store = useKnowledgeStore()
const vaultConfigured = ref(true)

onMounted(async () => {
  try {
    const config = await getVaultConfig()
    vaultConfigured.value = config.configured
    if (config.configured) {
      await store.fetchNotes()
    }
  } catch {
    vaultConfigured.value = false
  }
})

const displayNotes = computed(() => {
  if (store.searchKeyword && store.searchResults.length > 0) return store.searchResults
  if (store.searchKeyword) return store.searchResults
  return store.notes
})

const renderedContent = computed(() => {
  if (!store.currentNote?.content) return ''
  return renderMarkdown(store.currentNote.content)
})

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  try {
    const d = new Date(dateStr)
    return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
  } catch {
    return dateStr
  }
}

async function handleSearch() {
  if (store.searchKeyword.trim()) {
    await store.search(store.searchKeyword)
  } else {
    store.clearSearch()
  }
}

function clearAndRefresh() {
  store.clearSearch()
  store.fetchNotes()
}
</script>
