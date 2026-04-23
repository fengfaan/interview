import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as api from '../api/knowledgeApi'
import type { NoteItem, NoteDetail, CreateNoteRequest } from '../types/knowledge'

export const useKnowledgeStore = defineStore('knowledge', () => {
  const notes = ref<NoteItem[]>([])
  const currentNote = ref<NoteDetail | null>(null)
  const searchKeyword = ref('')
  const searchResults = ref<NoteItem[]>([])
  const selectedDirection = ref('all')
  const isLoading = ref(false)
  const isSaving = ref(false)
  const isSearching = ref(false)
  const error = ref('')
  const successMessage = ref('')

  const DIRECTIONS = [
    { value: 'all', label: '全部' },
    { value: 'Go 后端', label: 'Go 后端' },
    { value: 'React 前端', label: 'React 前端' },
    { value: '系统设计', label: '系统设计' },
    { value: '数据库相关', label: '数据库相关' },
    { value: 'AI Agent 开发方向', label: 'AI Agent 开发方向' },
  ]

  async function fetchNotes() {
    error.value = ''
    isLoading.value = true
    try {
      const dir = selectedDirection.value === 'all' ? undefined : selectedDirection.value
      notes.value = await api.listNotes(dir)
    } catch (e: any) {
      error.value = e.message || '加载笔记失败'
    } finally {
      isLoading.value = false
    }
  }

  async function fetchNote(id: string) {
    error.value = ''
    isLoading.value = true
    try {
      currentNote.value = await api.getNote(id)
    } catch (e: any) {
      error.value = e.message || '加载笔记详情失败'
    } finally {
      isLoading.value = false
    }
  }

  async function saveNote(request: CreateNoteRequest): Promise<boolean> {
    error.value = ''
    successMessage.value = ''
    isSaving.value = true
    try {
      await api.createNote(request)
      successMessage.value = '已保存到知识库'
      return true
    } catch (e: any) {
      error.value = e.message || '保存笔记失败'
      return false
    } finally {
      isSaving.value = false
    }
  }

  async function search(keyword: string) {
    if (!keyword.trim()) {
      searchResults.value = []
      return
    }
    error.value = ''
    isSearching.value = true
    try {
      searchResults.value = await api.searchNotes(keyword)
    } catch (e: any) {
      error.value = e.message || '搜索失败'
    } finally {
      isSearching.value = false
    }
  }

  async function filterByDirection(direction: string) {
    selectedDirection.value = direction
    await fetchNotes()
  }

  function clearSearch() {
    searchKeyword.value = ''
    searchResults.value = []
  }

  return {
    notes, currentNote, searchKeyword, searchResults,
    selectedDirection, isLoading, isSaving, isSearching,
    error, successMessage, DIRECTIONS,
    fetchNotes, fetchNote, saveNote, search, filterByDirection, clearSearch,
  }
})
