<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const navItems = [
  { name: 'projects', label: '项目总览' },
  { name: 'assets', label: '资产总览' },
]

const employeeLabel = computed(() =>
  authStore.employeeId ? `EMP-${authStore.employeeId}` : '已登录用户',
)

function logout() {
  authStore.clearSession()
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="shell">
    <aside class="shell__aside">
      <div>
        <p class="eyebrow">ABAC Graduation Project</p>
        <h1 class="shell__title">企业在线数字资产管理系统</h1>
        <p class="shell__subtitle">
          以项目阶段、密级和员工属性为核心，统一管理企业数字资产的访问控制。
        </p>
      </div>

      <nav class="shell__nav">
        <button
          v-for="item in navItems"
          :key="item.name"
          class="nav-button"
          :class="{ 'nav-button--active': route.name === item.name }"
          @click="router.push({ name: item.name })"
        >
          {{ item.label }}
        </button>
      </nav>

      <div class="shell__meta">
        <span class="badge">{{ employeeLabel }}</span>
        <button class="secondary-button" @click="logout">退出登录</button>
      </div>
    </aside>

    <main class="shell__main">
      <slot />
    </main>
  </div>
</template>
