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
  const saveError = ref('')
  const saveResults = ref<Map<number, ImportSaveResult>>(new Map())
  const savedCount = computed(() => [...saveResults.value.values()].filter(r => r.success).length)

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
    saveResults.value = new Map()
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
    const s = new Set<number>()
    for (const id of selectedIds.value) {
      if (id === index) continue
      if (id > index) s.add(id - 1)
      else s.add(id)
    }
    selectedIds.value = s
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
    const selectedIndices = [...selectedIds.value].sort((a, b) => a - b)
    const items = selectedIndices.map(i => parsedQuestions.value[i])
    if (items.length === 0) return

    isSaving.value = true
    saveError.value = ''
    saveResults.value = new Map()
    try {
      const results = await saveImported({
        items,
        direction: direction.value,
        level: level.value,
        sourceUrl: inputMode.value === 'url' ? url.value : '',
      })
      const map = new Map<number, ImportSaveResult>()
      results.forEach((r, i) => map.set(selectedIndices[i], r))
      saveResults.value = map
    } catch (e: any) {
      saveError.value = e.message || '导入失败'
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
    saveResults.value = new Map()
    saveError.value = ''
  }

  return {
    inputMode, url, pastedContent, direction, level,
    capturedTitle, capturedContent, isCapturing, captureError,
    parsedQuestions, isParsing, parseError,
    selectedIds, editingIndex, editDraft,
    isSaving, saveError, saveResults, savedCount, contentToParse,
    doCapture, doParse, toggleSelect, selectAll, deselectAll,
    removeQuestion, startEdit, cancelEdit, saveEdit, doSave, reset,
  }
})
