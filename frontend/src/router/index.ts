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
          path: 'resume',
          name: 'resume',
          component: () => import('../views/ResumeOptimizerView.vue'),
        },
      ],
    },
  ],
})

export default router
