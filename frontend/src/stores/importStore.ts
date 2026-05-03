import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { capturePage, streamParseQuestions, saveImported, consolidateStream, saveConsolidated } from '../api/importApi'
import type { ParsedQuestion, ImportSaveResult, ConsolidateResult, ConsolidatedSaveResult } from '../types/import'

export const useImportStore = defineStore('import', () => {
  const inputMode = ref<'url' | 'paste'>('url')
  const url = ref('')
  const pastedContent = ref('')

  const capturedTitle = ref('')
  const capturedContent = ref('')
  const isCapturing = ref(false)
  const captureError = ref('')

  const parsedQuestions = ref<ParsedQuestion[]>([])
  const isParsing = ref(false)
  const parseError = ref('')
  const parseProgress = ref('')

  const selectedIds = ref<Set<number>>(new Set())
  const editingIndex = ref<number | null>(null)
  const editDraft = ref<ParsedQuestion | null>(null)

  const isSaving = ref(false)
  const saveError = ref('')
  const saveResults = ref<Map<number, ImportSaveResult>>(new Map())
  const savedCount = computed(() => [...saveResults.value.values()].filter(r => r.success).length)

  const isConsolidating = ref(false)
  const consolidateProgress = ref('')
  const consolidatedResult = ref<ConsolidateResult | null>(null)
  const consolidateError = ref('')
  const consolidatedSaved = ref(false)
  const consolidatedSaveResult = ref<ConsolidatedSaveResult | null>(null)

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
    parseProgress.value = ''
    isParsing.value = true
    parsedQuestions.value = []
    selectedIds.value = new Set()
    saveResults.value = new Map()
    try {
      await streamParseQuestions(
        contentToParse.value,
        (items) => {
          parsedQuestions.value = [...parsedQuestions.value, ...items]
        },
        (text) => {
          parseProgress.value = text
        },
        (error) => {
          parseError.value = error
        },
      )
    } catch (e: any) {
      parseError.value = e.message || '解析失败'
    } finally {
      isParsing.value = false
      parseProgress.value = ''
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

  async function doConsolidate() {
    const items = Array.from(selectedIds.value)
      .sort((a, b) => a - b)
      .map(i => parsedQuestions.value[i])
      .filter(Boolean)

    if (items.length === 0) return

    isConsolidating.value = true
    consolidateError.value = ''
    consolidateProgress.value = ''
    consolidatedResult.value = null
    consolidatedSaved.value = false
    consolidatedSaveResult.value = null

    try {
      await consolidateStream(
        {
          items,
          sourceUrl: url.value,
          title: capturedTitle.value || '面试题集'
        },
        (result) => {
          consolidatedResult.value = result
        },
        (msg) => {
          consolidateProgress.value = msg
        },
        (msg) => {
          consolidateError.value = msg
        }
      )
    } catch (e: any) {
      consolidateError.value = e.message || '清洗失败'
    } finally {
      isConsolidating.value = false
    }
  }

  async function doConsolidatedSave() {
    if (!consolidatedResult.value) return

    isSaving.value = true
    saveError.value = ''

    try {
      const result = await saveConsolidated({
        categories: consolidatedResult.value.categories,
        sourceUrl: url.value,
        title: capturedTitle.value || '面试题集'
      })
      consolidatedSaveResult.value = result
      consolidatedSaved.value = true
    } catch (e: any) {
      saveError.value = e.message || '合并保存失败'
    } finally {
      isSaving.value = false
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
    parseProgress.value = ''
    selectedIds.value = new Set()
    editingIndex.value = null
    editDraft.value = null
    isSaving.value = false
    saveResults.value = new Map()
    saveError.value = ''
    isConsolidating.value = false
    consolidateProgress.value = ''
    consolidatedResult.value = null
    consolidateError.value = ''
    consolidatedSaved.value = false
    consolidatedSaveResult.value = null
  }

  return {
    inputMode, url, pastedContent,
    capturedTitle, capturedContent, isCapturing, captureError,
    parsedQuestions, isParsing, parseError, parseProgress,
    selectedIds, editingIndex, editDraft,
    isSaving, saveError, saveResults, savedCount, contentToParse,
    isConsolidating, consolidateProgress, consolidatedResult, consolidateError,
    consolidatedSaved, consolidatedSaveResult,
    doCapture, doParse, toggleSelect, selectAll, deselectAll,
    removeQuestion, startEdit, cancelEdit, saveEdit, doSave,
    doConsolidate, doConsolidatedSave,
    reset,
  }
})
