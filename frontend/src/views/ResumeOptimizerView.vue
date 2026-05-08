<template>
  <div class="min-h-screen flex flex-col">
    <!-- Header -->
    <header class="sticky top-0 z-10 bg-surface-container-low/80 backdrop-blur-xl border-b border-outline-variant/20 px-8 py-4">
      <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
        <h2 class="text-xl font-extrabold text-on-surface font-headline shrink-0">简历调优台</h2>

        <div class="flex gap-2 overflow-x-auto">
          <button
            v-for="tab in TABS" :key="tab.value"
            @click="store.setActiveTab(tab.value)"
            class="px-3 py-1.5 rounded-lg text-sm font-medium transition-colors whitespace-nowrap"
            :class="store.activeTab === tab.value ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant hover:bg-surface-container-highest'"
          >{{ tab.label }}</button>
        </div>

        <div class="shrink-0">
          <button
            v-if="store.activeTab === 'match'"
            @click="store.analyze()"
            :disabled="!store.canAnalyze"
            class="bg-gradient-to-r from-primary to-primary-container text-on-primary font-label font-medium rounded-xl px-6 py-2.5 shadow-md hover:opacity-90 transition-opacity flex items-center gap-2 disabled:opacity-50"
          >
            <span class="material-symbols-outlined text-base" :class="{'animate-spin': store.isLoading}">{{ store.isLoading ? 'progress_activity' : 'auto_fix_high' }}</span>
            {{ store.isLoading ? '分析中...' : '生成报告' }}
          </button>
          <button
            v-else-if="store.activeTab === 'structure'"
            @click="store.analyzeStructure()"
            :disabled="!store.canAnalyzeStructure"
            class="bg-gradient-to-r from-secondary to-secondary-container text-on-secondary font-label font-medium rounded-xl px-6 py-2.5 shadow-md hover:opacity-90 transition-opacity flex items-center gap-2 disabled:opacity-50"
          >
            <span class="material-symbols-outlined text-base" :class="{'animate-spin': store.isStructureAnalyzing}">{{ store.isStructureAnalyzing ? 'progress_activity' : 'health_and_safety' }}</span>
            {{ store.isStructureAnalyzing ? '诊断中...' : '开始体检' }}
          </button>
          <button
            v-else-if="store.activeTab === 'polish'"
            @click="doPolish"
            :disabled="store.isPolishing || !store.polishInput.trim()"
            class="bg-gradient-to-r from-primary to-primary-container text-on-primary font-label font-medium rounded-xl px-6 py-2.5 shadow-md hover:opacity-90 transition-opacity flex items-center gap-2 disabled:opacity-50"
          >
            <span class="material-symbols-outlined text-base" :class="{'animate-spin': store.isPolishing}">{{ store.isPolishing ? 'progress_activity' : 'magic_button' }}</span>
            {{ store.isPolishing ? '润色中...' : '深度润色' }}
          </button>
        </div>
      </div>
      <p v-if="store.inputValidationMessage" class="text-xs text-on-surface-variant mt-2 text-right">
        {{ store.inputValidationMessage }}
      </p>
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
          <!-- JD Input (Hidden in Structure Check) -->
          <div v-show="store.activeTab !== 'structure'" class="bg-surface-container-lowest rounded-xl overflow-hidden flex flex-col">
            <div class="bg-surface-container-low px-6 py-3 flex items-center justify-between">
              <h3 class="font-headline font-bold text-on-surface">目标 JD <span v-if="store.activeTab === 'polish'" class="text-xs text-on-surface-variant font-normal ml-2">(可选)有助于融入关键词</span></h3>
              <span class="text-xs text-on-surface-variant">{{ store.jobDescription.length }} / 20000</span>
            </div>
            <textarea
              v-model="store.jobDescription"
              class="w-full min-h-[200px] bg-surface-container-lowest p-6 font-body text-sm text-on-surface placeholder-on-surface-variant/50 focus:ring-0 resize-y border-none outline-none"
              placeholder="粘贴目标职位 JD 内容..."
              @input="store.persist()"
              @keydown="handleKeydown"
            ></textarea>
          </div>

          <!-- Resume Editor -->
          <div class="bg-surface-container-lowest rounded-xl overflow-hidden flex flex-col flex-1">
            <div class="bg-surface-container-low px-6 py-3 flex items-center justify-between">
              <h3 class="font-headline font-bold text-on-surface">简历内容</h3>
              <span class="text-xs text-on-surface-variant">{{ store.resume.length }} / 20000</span>
            </div>
            <!-- File upload area -->
            <div
              class="px-6 py-3 border-b border-outline-variant/10 flex items-center gap-3"
              @dragover.prevent="isDragging = true"
              @dragleave.prevent="isDragging = false"
              @drop.prevent="handleFileDrop"
              :class="{ 'bg-primary/5': isDragging }"
            >
              <label class="cursor-pointer inline-flex items-center gap-2 text-sm text-primary font-medium hover:opacity-80 transition-opacity">
                <span class="material-symbols-outlined text-base">{{ store.isImporting ? 'progress_activity' : 'upload_file' }}</span>
                <span :class="{ 'animate-pulse': store.isImporting }">{{ store.isImporting ? '导入中...' : '导入文件' }}</span>
                <input type="file" accept=".pdf,.docx" class="hidden" @change="handleFileSelect" :disabled="store.isImporting" />
              </label>
              <span class="text-xs text-on-surface-variant">支持 PDF / DOCX</span>
              <span v-if="store.importError" class="text-xs text-error ml-auto">{{ store.importError }}</span>
            </div>
            <textarea
              v-model="store.resume"
              class="w-full flex-1 bg-surface-container-lowest p-6 font-body text-sm text-on-surface placeholder-on-surface-variant/50 focus:ring-0 resize-y border-none outline-none"
              :class="store.activeTab === 'structure' ? 'min-h-[560px]' : 'min-h-[300px]'"
              placeholder="粘贴或输入简历 Markdown 内容..."
              @input="store.persist()"
              @keydown="handleKeydown"
            ></textarea>
          </div>
        </div>

        <!-- Right Column: Results mapping to tabs -->
        <div class="flex flex-col gap-6">

          <!-- 1. Match Analysis view -->
          <template v-if="store.activeTab === 'match'">
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
            <div v-else class="flex-1 flex items-center justify-center bg-surface-container-lowest rounded-xl min-h-[400px]">
              <div class="text-center text-on-surface-variant">
                <span class="material-symbols-outlined text-5xl mb-3 block">description</span>
                <p class="font-headline font-bold">输入 JD 和简历，开始分析</p>
                <p class="text-sm mt-2">AI 将评估匹配度并提供优化建议</p>
              </div>
            </div>
          </template>

          <!-- 2. Structure Analysis view -->
          <template v-else-if="store.activeTab === 'structure'">
            <div class="bg-surface-container-lowest rounded-xl p-6 shadow-sm">
              <h3 class="text-lg font-bold font-headline mb-6 flex items-center gap-2">
                <span class="material-symbols-outlined text-secondary">monitor_heart</span>
                体检报告
                <span v-if="store.structureResult" class="text-xs text-on-surface-variant font-normal ml-auto border border-outline-variant/50 px-2 py-0.5 rounded-full">{{ store.structureResult.actualModel }}</span>
              </h3>

              <div v-if="store.structureResult">
                  <div class="mb-6">
                     <span class="text-4xl font-extrabold text-primary">{{ store.structureResult.value.structureScore }}</span><span class="text-on-surface-variant font-bold ml-1">分</span>
                     <p class="text-sm text-error font-semibold mt-2">{{ store.structureResult.value.summary }}</p>
                  </div>

                  <div class="mb-6">
                     <h4 class="font-headline font-bold text-on-surface mb-3 flex items-center gap-2"><span class="material-symbols-outlined text-sm">fact_check</span> 模块扫描</h4>
                     <div class="space-y-2">
                         <div v-for="check in store.structureResult.value.moduleChecks" :key="check.name" class="flex items-center text-sm p-3 rounded-lg bg-surface-container">
                            <span :class="{'text-secondary': check.status==='pass', 'text-yellow-600': check.status==='warn', 'text-error': check.status==='fail'}" class="mr-3 font-bold block min-w-[80px]">
                              {{ check.status === 'pass' ? '✓' : (check.status === 'warn' ? '!' : '✗') }} {{ check.name }}
                            </span>
                            <span class="text-on-surface leading-tight">{{ check.detail }}</span>
                         </div>
                     </div>
                  </div>

                  <div>
                     <h4 class="font-headline font-bold text-on-surface mb-3 flex items-center gap-2"><span class="material-symbols-outlined text-sm">rule</span> 逐段诊断</h4>
                     <div class="space-y-4">
                         <div v-for="(issue, idx) in store.structureResult.value.issues" :key="idx" class="border-l-4 rounded-r-lg bg-surface-container-low shadow-sm overflow-hidden" :class="{'border-error': issue.severity==='critical', 'border-yellow-500': issue.severity==='warning', 'border-secondary': issue.severity==='info'}">
                             <!-- Header: location + action badge -->
                             <div class="flex items-center gap-2 px-4 py-2 bg-surface-container">
                                 <span class="text-xs font-bold px-2 py-0.5 rounded" :class="{'bg-error-container text-on-error-container': issue.severity==='critical', 'bg-yellow-100 text-yellow-800': issue.severity==='warning', 'bg-surface-container-high text-on-surface-variant': issue.severity==='info'}">{{ issue.severity === 'critical' ? '严重' : (issue.severity === 'warning' ? '警告' : '建议') }}</span>
                                 <span class="text-xs text-on-surface-variant">{{ issue.location }}</span>
                                 <span v-if="issue.action" class="ml-auto text-xs font-bold px-2 py-0.5 rounded bg-primary-container text-on-primary-container">{{ issue.action }}</span>
                             </div>
                             <!-- Quote from resume -->
                             <div class="px-4 py-2 text-xs text-on-surface-variant bg-surface-container/50 border-y border-outline-variant/10">
                                 <span class="text-on-surface-variant/60">原文：</span>
                                 <span class="line-through decoration-error/40">{{ issue.quote }}</span>
                             </div>
                             <!-- Problem + suggestion -->
                             <div class="px-4 py-3">
                                 <p class="text-sm font-medium text-on-surface">{{ issue.problem }}</p>
                                 <p class="text-sm text-on-surface-variant mt-1">{{ issue.suggestion }}</p>
                             </div>
                             <!-- Rewrite result -->
                             <div v-if="issue.rewrite" class="px-4 py-3 border-t border-outline-variant/10">
                                 <div class="flex items-center gap-2 mb-1">
                                     <span class="text-xs text-secondary font-bold">改写参考</span>
                                     <button @click="store.applyStructureRewrite(issue.quote, issue.rewrite)" class="text-xs text-primary hover:underline ml-auto">应用到简历</button>
                                 </div>
                                 <div class="text-sm text-secondary bg-secondary/5 p-3 rounded-lg whitespace-pre-line leading-relaxed">{{ issue.rewrite }}</div>
                             </div>
                         </div>
                     </div>
                  </div>
              </div>
              <div v-else class="flex-1 flex items-center justify-center min-h-[300px]">
                <div class="text-center text-on-surface-variant">
                  <span class="material-symbols-outlined text-5xl mb-3 opacity-50 block">monitor_heart</span>
                  <p class="font-headline font-bold">在左侧填写简历，点击顶部「开始体检」</p>
                  <p class="text-sm mt-2">AI 将逐段诊断简历问题，给出具体修改方案</p>
                </div>
              </div>
            </div>
          </template>

          <!-- 3. Deep Polish view -->
          <template v-else-if="store.activeTab === 'polish'">
            <div class="bg-surface-container-lowest rounded-xl p-6 shadow-sm">
                <div class="mb-4">
                    <label class="block font-headline font-bold text-on-surface mb-2 flex items-center gap-2"><span class="material-symbols-outlined text-sm">edit_square</span>粘贴简历中某段原经历进行重写</label>
                    <textarea v-model="store.polishInput" class="w-full p-4 border border-outline-variant/30 rounded-xl focus:ring-2 focus:ring-primary focus:border-transparent outline-none bg-surface-container-low text-sm leading-relaxed" rows="4" placeholder="例如：在公司中负责了XX管理系统的开发，修复了许多 bug，最后提升了效率。使用了 Java, SpringBoot..." @input="store.persist()"></textarea>
                </div>

                <div v-if="store.polishResult">
                  <div class="flex items-center justify-between mb-3">
                    <h4 class="font-headline font-bold text-on-surface flex items-center gap-2"><span class="material-symbols-outlined text-primary text-sm">stars</span>润色结果（多版本参考）</h4>
                    <button @click="store.clearPolishResult()" class="text-xs text-on-surface-variant hover:text-on-surface transition-colors flex items-center gap-1">
                      <span class="material-symbols-outlined text-sm">refresh</span>清除结果
                    </button>
                  </div>
                  <div class="prose prose-sm max-w-none bg-surface-container-low p-6 rounded-xl border border-outline-variant/10 text-on-surface">
                     <div v-html="renderMarkdown(store.polishResult)"></div>
                  </div>
                </div>
                <div v-else-if="!store.isPolishing" class="flex-1 flex items-center justify-center min-h-[200px]">
                  <div class="text-center text-on-surface-variant">
                    <span class="material-symbols-outlined text-5xl mb-3 opacity-50 block">magic_button</span>
                    <p class="font-headline font-bold">粘贴一段项目经历，点击顶部「深度润色」</p>
                    <p class="text-sm mt-2">AI 将按 STAR 法则改写，并提供多版本参考</p>
                  </div>
                </div>
                <div v-else class="flex items-center justify-center min-h-[200px]">
                  <div class="flex items-center gap-2 text-primary">
                    <span class="material-symbols-outlined animate-spin">progress_activity</span>
                    <span class="text-sm font-medium">正在深度润色...</span>
                  </div>
                </div>
            </div>
          </template>

        </div>
      </div>
    </div>

    <!-- Bottom STAR Workspace (Only visible for match tab) -->
    <div
      v-show="store.activeTab === 'match' && store.starWorkspaceExpanded"
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
import { computed, ref } from 'vue'
import { useResumeStore } from '../stores/resumeStore'
import { renderMarkdown } from '../utils/markdown'

const store = useResumeStore()
const isDragging = ref(false)

const TABS = [
  { value: 'match' as const, label: 'JD 匹配分析' },
  { value: 'structure' as const, label: '结构体检' },
  { value: 'polish' as const, label: '深度润色' },
]

function doPolish() {
  if (store.polishInput.trim()) {
    store.startPolish()
  }
}

const renderedRewrite = computed(() => {
  if (!store.starRewrite) return ''
  return renderMarkdown(store.starRewrite)
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

function handleFileSelect(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) store.importFile(file)
  input.value = ''
}

function handleFileDrop(e: DragEvent) {
  isDragging.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file) store.importFile(file)
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
    e.preventDefault()
    if (store.activeTab === 'match') store.analyze()
    else if (store.activeTab === 'structure') store.analyzeStructure()
    else if (store.activeTab === 'polish') doPolish()
  }
}
</script>
