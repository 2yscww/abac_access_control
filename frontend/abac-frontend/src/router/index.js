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
            title: 'Project Overview',
            description:
              'Review visible projects, phase distribution, and ABAC-filtered create/delete entry points.',
          },
        },
        {
          path: 'projects/:id',
          name: 'project-detail',
          component: () => import('@/views/ProjectDetailView.vue'),
          meta: {
            requiresAuth: true,
            requiredMenu: 'projects',
            title: 'Project Detail',
            description:
              'Inspect project attributes, members, assets, and phase handover in one workflow.',
          },
        },
        {
          path: 'assets',
          name: 'assets',
          component: () => import('@/views/AssetsView.vue'),
          meta: {
            requiresAuth: true,
            requiredMenu: 'assets',
            title: 'Asset Center',
            description: 'Query assets by project, stage, type, and security level.',
          },
        },
        {
          path: 'files',
          name: 'files',
          component: () => import('@/views/FileCenterView.vue'),
          meta: {
            requiresAuth: true,
            requiredMenu: 'files',
            title: 'File Center',
            description: 'Prototype page for file upload, download, and circulation entry points.',
          },
        },
        {
          path: 'handover',
          name: 'handover',
          component: () => import('@/views/HandoverWorkbenchView.vue'),
          meta: {
            requiresAuth: true,
            requiredMenu: 'handover',
            requiredCapability: 'handover.view',
            title: 'Handover Workbench',
            description: 'Handle offboarding, manager reassignment, and pending handover items.',
          },
        },
        {
          path: 'audit',
          name: 'audit',
          component: () => import('@/views/AuditLogsView.vue'),
          meta: {
            requiresAuth: true,
            requiredMenu: 'audit',
            requiredCapability: 'audit.view',
            title: 'Audit Logs',
            description: 'Review authorization decisions and business handover events.',
          },
        },
        {
          path: 'policy',
          name: 'policy',
          component: () => import('@/views/PolicyConfigView.vue'),
          meta: {
            requiresAuth: true,
            requiredMenu: 'policy',
            requiredCapability: 'policy.manage',
            title: 'Policy Config',
            description:
              'Adjust runtime thresholds while keeping ABAC policy logic fixed in code.',
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
      await authStore.fetchCurrentUser(true)
    } catch {
      authStore.clearSession()
      return { name: 'login' }
    }

    if (to.meta.requiredMenu && !authStore.hasMenu(String(to.meta.requiredMenu))) {
      return { name: 'projects' }
    }

    if (
      to.meta.requiredCapability &&
      !authStore.hasCapability(String(to.meta.requiredCapability))
    ) {
      return { name: 'projects' }
    }
  }

  if (to.name === 'login' && authStore.isAuthenticated) {
    return { name: 'projects' }
  }

  return true
})

export default router
