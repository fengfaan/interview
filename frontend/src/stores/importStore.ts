import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { capturePage, parseQuestions, saveImported } from '../api/importApi'
import type { ParsedQuestion, ImportSaveResult } from '../types/import'

export const useImportStore = defineStore('import', () => {
  const inputMode = ref<'url' | 'paste'>('url')
  const url = ref('')
  const pastedContent = ref('')
  const direction = ref('GO_BACKEND')
  const level = ref('BASIC')

  const capturedTitle = ref('')
  const capturedContent = ref('')
  const isCapturing = ref(false)
  const captureError = ref('')

  const parsedQuestions = ref<ParsedQuestion[]>([])
  const isParsing = ref(false)
  const parseError = ref('')

  const selectedIds = ref<Set<number>>(new Set())
  const editingIndex = ref<number | null>(null)
  const editDraft = ref<ParsedQuestion | null>(null)

  const isSaving = ref(false)
  const saveResults = ref<ImportSaveResult[]>([])
  const savedCount = computed(() => saveResults.value.filter(r => r.success).length)

  const contentToParse = computed(() =>
    inputMode.value === 'url' ? capturedContent.value : pastedContent.value
  )

  async function doCapture() {
    captureError.value = ''
    isCapturing.value = true
    capturedTitle.value = ''
    capturedContent.value = ''
    try {
      const result = await capturePage({ url: url.value })
      capturedTitle.value = result.title
      capturedContent.value = result.content
    } catch (e: any) {
      captureError.value = e.message || '网页抓取失败'
    } finally {
      isCapturing.value = false
    }
  }

  async function doParse() {
    if (!contentToParse.value.trim()) return
    parseError.value = ''
    isParsing.value = true
    parsedQuestions.value = []
    selectedIds.value = new Set()
    saveResults.value = []
    try {
      const result = await parseQuestions({
        content: contentToParse.value,
        direction: direction.value,
        level: level.value,
      })
      parsedQuestions.value = result.items
    } catch (e: any) {
      parseError.value = e.message || '解析失败'
    } finally {
      isParsing.value = false
    }
  }

  function toggleSelect(index: number) {
    const s = new Set(selectedIds.value)
    if (s.has(index)) {
      s.delete(index)
    } else {
      s.add(index)
    }
    selectedIds.value = s
  }

  function selectAll() {
    selectedIds.value = new Set(parsedQuestions.value.map((_, i) => i))
  }

  function deselectAll() {
    selectedIds.value = new Set()
  }

  function removeQuestion(index: number) {
    const updated = [...parsedQuestions.value]
    updated.splice(index, 1)
    parsedQuestions.value = updated
    const s = new Set(selectedIds.value)
    s.delete(index)
    selectedIds.value = new Set([...s].filter(i => i < updated.length))
  }

  function startEdit(index: number) {
    editingIndex.value = index
    editDraft.value = { ...parsedQuestions.value[index] }
  }

  function cancelEdit() {
    editingIndex.value = null
    editDraft.value = null
  }

  function saveEdit() {
    if (editingIndex.value !== null && editDraft.value) {
      parsedQuestions.value[editingIndex.value] = { ...editDraft.value }
      editingIndex.value = null
      editDraft.value = null
    }
  }

  async function doSave() {
    const items = [...selectedIds.value]
      .sort((a, b) => a - b)
      .map(i => parsedQuestions.value[i])
    if (items.length === 0) return

    isSaving.value = true
    saveResults.value = []
    try {
      const results = await saveImported({
        items,
        direction: direction.value,
        level: level.value,
        sourceUrl: inputMode.value === 'url' ? url.value : '',
      })
      saveResults.value = results
    } catch (e: any) {
      parseError.value = e.message || '导入失败'
    } finally {
      isSaving.value = false
    }
  }

  function reset() {
    url.value = ''
    pastedContent.value = ''
    capturedTitle.value = ''
    capturedContent.value = ''
    captureError.value = ''
    parsedQuestions.value = []
    parseError.value = ''
    selectedIds.value = new Set()
    editingIndex.value = null
    editDraft.value = null
    isSaving.value = false
    saveResults.value = []
  }

  return {
    inputMode, url, pastedContent, direction, level,
    capturedTitle, capturedContent, isCapturing, captureError,
    parsedQuestions, isParsing, parseError,
    selectedIds, editingIndex, editDraft,
    isSaving, saveResults, savedCount, contentToParse,
    doCapture, doParse, toggleSelect, selectAll, deselectAll,
    removeQuestion, startEdit, cancelEdit, saveEdit, doSave, reset,
  }
})
