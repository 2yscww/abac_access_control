import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import { pinia } from '@/stores'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: () => import('@/components/MainShell.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          redirect: '/projects',
        },
        {
          path: 'projects',
          name: 'projects',
          component: () => import('@/views/ProjectsView.vue'),
          meta: {
            requiresAuth: true,
            requiredMenu: 'projects',
            title: '项目总览',
            description: '查看项目列表、阶段分布和基础统计。',
          },
        },
        {
          path: 'projects/:id',
          name: 'project-detail',
          component: () => import('@/views/ProjectDetailView.vue'),
          meta: {
            requiresAuth: true,
            requiredMenu: 'projects',
            title: '项目详情',
            description: '查看项目元数据、资产列表和阶段推进信息。',
          },
        },
        {
          path: 'assets',
          name: 'assets',
          component: () => import('@/views/AssetsView.vue'),
          meta: {
            requiresAuth: true,
            requiredMenu: 'assets',
            title: '资产中心',
            description: '按项目、类型和密级查询可见资产。',
          },
        },
        {
          path: 'files',
          name: 'files',
          component: () => import('@/views/FileCenterView.vue'),
          meta: {
            requiresAuth: true,
            requiredMenu: 'files',
            title: '文件中心',
            description: '原型页：展示上传、下载和文件流转入口。',
          },
        },
        {
          path: 'handover',
          name: 'handover',
          component: () => import('@/views/HandoverWorkbenchView.vue'),
          meta: {
            requiresAuth: true,
            requiredMenu: 'handover',
            title: '负责人交接',
            description: '处理离职、负责人改派和待办项目。',
          },
        },
        {
          path: 'audit',
          name: 'audit',
          component: () => import('@/views/AuditLogsView.vue'),
          meta: {
            requiresAuth: true,
            requiredMenu: 'audit',
            title: '审计日志',
            description: '查看授权决策和业务交接记录。',
          },
        },
      ],
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore(pinia)

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'login' }
  }

  if (to.meta.requiresAuth && authStore.isAuthenticated) {
    try {
      await authStore.fetchCurrentUser()
    } catch {
      authStore.clearSession()
      return { name: 'login' }
    }

    if (to.meta.requiredMenu && !authStore.hasMenu(String(to.meta.requiredMenu))) {
      return { name: 'projects' }
    }
  }

  if (to.name === 'login' && authStore.isAuthenticated) {
    return { name: 'projects' }
  }

  return true
})

export default router
