<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const navItems = [
  { name: 'projects', label: '项目总览', menuKey: 'projects' },
  { name: 'assets', label: '资产中心', menuKey: 'assets' },
  { name: 'files', label: '文件中心', menuKey: 'files' },
  { name: 'handover', label: '负责人交接', menuKey: 'handover' },
  { name: 'audit', label: '审计日志', menuKey: 'audit' },
]

const visibleNavItems = computed(() =>
  navItems.filter((item) => authStore.hasMenu(item.menuKey)),
)

const activeMenu = computed(() => {
  if (route.name === 'project-detail') {
    return 'projects'
  }

  return route.name ? String(route.name) : 'projects'
})

const employeeLabel = computed(() => authStore.employeeLabel)

function handleSelect(name) {
  if (route.name === name) {
    return
  }

  router.push({ name })
}

function logout() {
  authStore.clearSession()
  router.push({ name: 'login' })
}
</script>

<template>
  <aside class="app-sidebar">
    <div class="app-sidebar__brand">
      <p class="app-sidebar__eyebrow">ABAC Platform</p>
      <h1 class="app-sidebar__title">权限治理与安全审计</h1>
      <p class="app-sidebar__subtitle">
        {{ authStore.profile?.deptTypeDesc || '业务控制台' }}
      </p>
    </div>

    <el-scrollbar class="app-sidebar__nav">
      <el-menu :default-active="activeMenu" class="app-sidebar__menu" @select="handleSelect">
        <el-menu-item v-for="item in visibleNavItems" :key="item.name" :index="item.name">
          {{ item.label }}
        </el-menu-item>
      </el-menu>
    </el-scrollbar>

    <div class="app-sidebar__footer">
      <el-tag effect="plain">{{ employeeLabel }}</el-tag>
      <el-button plain @click="logout">退出登录</el-button>
    </div>
  </aside>
</template>

<style scoped>
.app-sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: 100%;
  padding: 20px 16px;
  background: #ffffff;
}

.app-sidebar__brand {
  display: grid;
  gap: 8px;
}

.app-sidebar__eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: #909399;
  text-transform: uppercase;
}

.app-sidebar__title {
  margin: 0;
  font-size: 22px;
  line-height: 1.35;
  color: #303133;
}

.app-sidebar__subtitle {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: #909399;
}

.app-sidebar__nav {
  flex: 1;
  min-height: 0;
}

.app-sidebar__menu {
  border-right: none;
}

.app-sidebar__menu :deep(.el-menu) {
  border-right: none;
}

.app-sidebar__menu :deep(.el-menu-item) {
  height: 44px;
  margin-bottom: 6px;
  border-radius: 8px;
}

.app-sidebar__menu :deep(.el-menu-item.is-active) {
  background: #ecf5ff;
  color: var(--el-color-primary);
}

.app-sidebar__footer {
  display: grid;
  gap: 12px;
}
</style>
