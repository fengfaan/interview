<template>
  <nav class="fixed left-0 top-0 h-screen w-64 z-40 bg-inverse-surface flex flex-col justify-between p-6">
    <div>
      <div class="mb-10 flex items-center gap-3">
        <div class="w-10 h-10 rounded-full bg-gradient-to-br from-primary to-primary-container flex items-center justify-center">
          <span class="material-symbols-outlined text-on-primary text-lg">insights</span>
        </div>
        <div>
          <h1 class="text-lg font-black text-white tracking-tighter uppercase font-headline">Precision Path</h1>
          <p class="text-neutral-400 text-xs font-label">AI Career Curator</p>
        </div>
      </div>

      <button
        @click="handleNewSimulation"
        class="w-full mb-8 bg-gradient-to-r from-primary to-primary-container text-on-primary font-label font-medium rounded-xl py-3 px-4 shadow-lg hover:opacity-90 transition-opacity flex items-center justify-center gap-2"
      >
        <span class="material-symbols-outlined text-sm">add</span>
        新建模拟
      </button>

      <ul class="space-y-2 font-headline text-sm tracking-wide">
        <li>
          <RouterLink
            to="/interview"
            class="flex items-center gap-3 px-4 py-3 rounded-lg transition-all duration-300"
            :class="isActive('/interview') ? 'bg-white/10 text-white font-bold' : 'text-neutral-400 hover:text-white hover:bg-white/5'"
          >
            <span class="material-symbols-outlined">settings_voice</span>
            面试演练室
          </RouterLink>
        </li>
        <li>
          <RouterLink
            to="/resume"
            class="flex items-center gap-3 px-4 py-3 rounded-lg transition-all duration-300"
            :class="isActive('/resume') ? 'bg-white/10 text-white font-bold' : 'text-neutral-400 hover:text-white hover:bg-white/5'"
          >
            <span class="material-symbols-outlined">description</span>
            简历调优台
          </RouterLink>
        </li>
      </ul>
    </div>

    <div class="border-t border-white/10 pt-4 space-y-2 font-headline text-sm">
      <RouterLink
        to="/settings"
        class="flex items-center gap-3 px-4 py-2 rounded-lg transition-all duration-300"
        :class="isActive('/settings') ? 'bg-white/10 text-white font-bold' : 'text-neutral-400 hover:text-white hover:bg-white/5'"
      >
        <span class="material-symbols-outlined text-lg">settings</span> 设置
      </RouterLink>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { useRoute, RouterLink } from 'vue-router'
import { useInterviewStore } from '../stores/interviewStore'

const route = useRoute()

function isActive(path: string) {
  return route.path.startsWith(path)
}

function handleNewSimulation() {
  const store = useInterviewStore()
  store.newSession()
}
</script>
