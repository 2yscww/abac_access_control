<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { changePassword, login } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const loginForm = reactive({
  employeeCode: '',
  password: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
})

const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const mode = ref(authStore.tempToken ? 'change-password' : 'login')

async function submitLogin() {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const data = await login(loginForm)
    if (data.mustChangePassword) {
      authStore.setTempSession(data)
      mode.value = 'change-password'
      successMessage.value = '首次登录需要先修改密码。'
      passwordForm.oldPassword = loginForm.password
      return
    }

    authStore.setSession(data)
    router.push({ name: 'projects' })
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

async function submitPasswordChange() {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const data = await changePassword(authStore.tempToken, passwordForm)
    authStore.setSession(data)
    successMessage.value = '密码修改成功，正在进入系统。'
    router.push({ name: 'projects' })
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page login-page">
    <section class="hero-panel">
      <div class="stack">
        <p class="eyebrow">Enterprise Asset Control</p>
        <h1 class="hero-title">让密级、阶段和身份属性一起决定访问边界。</h1>
        <p class="hero-copy">
          这是一个面向企业在线数字资产管理场景的 ABAC 演示系统，强调动态授权、阶段隔离与安全审计。
        </p>
      </div>

      <div class="hero-grid">
        <article class="metric-card">
          <span class="muted">核心模型</span>
          <strong>ABAC 四元组</strong>
          <p class="muted">Subject / Resource / Action / Environment 联合决策。</p>
        </article>
        <article class="metric-card">
          <span class="muted">策略优先级</span>
          <strong>安全 > 项目</strong>
          <p class="muted">先挡住密级风险，再判断项目阶段和部门协同边界。</p>
        </article>
      </div>
    </section>

    <section class="surface">
      <div class="stack">
        <div>
          <p class="eyebrow">Secure Sign In</p>
          <h2 class="section-title">
            {{ mode === 'login' ? '登录系统' : '首次登录修改密码' }}
          </h2>
          <p class="section-subtitle">
            {{ mode === 'login' ? '输入员工工号和密码进入系统。' : '修改默认密码后才会下发正式登录凭证。' }}
          </p>
        </div>

        <div v-if="errorMessage" class="message message--error">{{ errorMessage }}</div>
        <div v-if="successMessage" class="message message--success">{{ successMessage }}</div>

        <form v-if="mode === 'login'" class="form-grid" @submit.prevent="submitLogin">
          <label class="label">
            员工工号
            <input v-model="loginForm.employeeCode" class="input" placeholder="例如 1001" />
          </label>

          <label class="label">
            密码
            <input
              v-model="loginForm.password"
              class="input"
              type="password"
              placeholder="请输入登录密码"
            />
          </label>

          <button class="primary-button" :disabled="loading">
            {{ loading ? '登录中...' : '进入系统' }}
          </button>
        </form>

        <form v-else class="form-grid" @submit.prevent="submitPasswordChange">
          <label class="label">
            原密码
            <input
              v-model="passwordForm.oldPassword"
              class="input"
              type="password"
              placeholder="请输入原密码"
            />
          </label>

          <label class="label">
            新密码
            <input
              v-model="passwordForm.newPassword"
              class="input"
              type="password"
              placeholder="设置新的登录密码"
            />
          </label>

          <div class="inline-actions">
            <button class="primary-button" :disabled="loading">
              {{ loading ? '提交中...' : '确认修改' }}
            </button>
            <button class="ghost-button" type="button" @click="mode = 'login'">返回登录</button>
          </div>
        </form>
      </div>
    </section>
  </div>
</template>
