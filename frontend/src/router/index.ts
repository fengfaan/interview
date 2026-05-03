import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '../layouts/AppLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: AppLayout,
      children: [
        {
          path: '',
          redirect: '/interview',
        },
        {
          path: 'interview',
          name: 'interview',
          component: () => import('../views/MockInterviewRoomView.vue'),
        },
        {
          path: 'rapid',
          name: 'rapid',
          component: () => import('../views/RapidQuestionView.vue'),
        },
        {
          path: 'import',
          name: 'import',
          component: () => import('../views/ImportView.vue'),
        },
        {
          path: 'resume',
          name: 'resume',
          component: () => import('../views/ResumeOptimizerView.vue'),
        },
        {
          path: 'knowledge',
          name: 'knowledge',
          component: () => import('../views/KnowledgeBaseView.vue'),
        },
        {
          path: 'settings',
          name: 'settings',
          component: () => import('../views/SettingsView.vue'),
        },
      ],
    },
  ],
})

export default router
