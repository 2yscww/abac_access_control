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
            description: '查看当前账号可见项目、阶段分布，以及受 ABAC 约束的新增与删除入口。',
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
            description: '在一个流程中查看项目属性、成员隔离、资产管理与阶段交接。',
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
            description: '按项目、阶段、类型和密级查询当前账号可见的资产。',
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
            description: '演示 MinIO 文件上传、受控下载与资产建档入口。',
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
            title: '交接工作台',
            description: '处理员工离职、部门负责人改派，以及严格模式下的待交接事项。',
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
            title: '审计日志',
            description: '查看授权决策记录与业务交接事件，展示系统的可追溯性。',
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
            title: '策略配置',
            description: '在不修改 ABAC 核心逻辑的前提下，调整运行期阈值与开关。',
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
