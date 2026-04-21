<template>
  <div class="flex min-h-screen">
    <AppSidebar />
    <main class="flex-1 ml-64 overflow-y-auto">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppSidebar from '../components/AppSidebar.vue'
</script>
