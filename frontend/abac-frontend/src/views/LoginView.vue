<script setup>
import { computed, reactive, ref } from 'vue'
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

const systemTitle = '企业敏感文档访问控制与安全审计系统'
const systemSummary =
  '面向多项目协作场景，统一管理文档访问、下载审计和阶段流转约束。'
const featureList = [
  '基于员工属性、项目阶段、资产类型和密级进行动态授权。',
  '所有关键访问行为都会进入审计链路，便于复核与追踪。',
  '项目阶段与部门职责联动，避免文档在不合规时机被扩散。',
]

const modeTitle = computed(() => (mode.value === 'login' ? '员工登录' : '首次登录修改密码'))

const modeDescription = computed(() =>
  mode.value === 'login'
    ? '请输入员工工号和登录密码。'
    : '首次登录必须先完成密码修改，修改成功后才可进入系统。',
)

async function submitLogin() {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const data = await login(loginForm)
    if (data.mustChangePassword) {
      authStore.setTempSession(data)
      mode.value = 'change-password'
      successMessage.value = '检测到首次登录，请先完成密码修改。'
      passwordForm.oldPassword = loginForm.password
      passwordForm.newPassword = ''
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
  <div class="login-page">
    <div class="login-page__mask"></div>
    <div class="login-page__inner">
      <section class="login-page__intro">
        <p class="login-page__eyebrow">ABAC Access Control</p>
        <h1>{{ systemTitle }}</h1>
        <p class="login-page__summary">
          {{ systemSummary }}
        </p>

        <div class="login-page__panel">
          <p class="login-page__panel-title">系统说明</p>
          <ul class="login-page__feature-list">
            <li v-for="feature in featureList" :key="feature">
              {{ feature }}
            </li>
          </ul>
        </div>
      </section>

      <el-card shadow="never" class="login-card">
        <div class="login-card__heading">
          <p class="login-page__eyebrow">账号入口</p>
          <h2>{{ modeTitle }}</h2>
          <p>{{ modeDescription }}</p>
        </div>

        <el-alert
          v-if="errorMessage"
          :title="errorMessage"
          type="error"
          show-icon
          :closable="false"
          class="login-card__alert"
        />
        <el-alert
          v-if="successMessage"
          :title="successMessage"
          type="success"
          show-icon
          :closable="false"
          class="login-card__alert"
        />

        <el-form v-if="mode === 'login'" label-position="top" @submit.prevent="submitLogin">
          <el-form-item label="员工工号">
            <el-input
              v-model="loginForm.employeeCode"
              placeholder="请输入员工工号"
              autocomplete="username"
              @keyup.enter="submitLogin"
            />
          </el-form-item>
          <el-form-item label="登录密码">
            <el-input
              v-model="loginForm.password"
              type="password"
              show-password
              placeholder="请输入登录密码"
              autocomplete="current-password"
              @keyup.enter="submitLogin"
            />
          </el-form-item>
          <el-button type="primary" class="login-card__submit" :loading="loading" @click="submitLogin">
            登录
          </el-button>
        </el-form>

        <el-form v-else label-position="top" @submit.prevent="submitPasswordChange">
          <el-form-item label="原密码">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              show-password
              placeholder="请输入原密码"
              autocomplete="current-password"
              @keyup.enter="submitPasswordChange"
            />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              show-password
              placeholder="请设置新的登录密码"
              autocomplete="new-password"
              @keyup.enter="submitPasswordChange"
            />
          </el-form-item>
          <div class="login-card__actions">
            <el-button
              type="primary"
              class="login-card__submit"
              :loading="loading"
              @click="submitPasswordChange"
            >
              确认修改
            </el-button>
            <el-button @click="mode = 'login'">返回登录</el-button>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  --login-bg: linear-gradient(135deg, #f3f6fb 0%, #eef2f8 45%, #f9fbfd 100%);
  --login-panel-bg: rgba(255, 255, 255, 0.82);
  --login-panel-border: rgba(148, 163, 184, 0.28);
  --login-title: #0f172a;
  --login-text: #475569;
  --login-muted: #64748b;
  --login-accent: #1d4ed8;

  position: relative;
  min-height: 100vh;
  overflow: hidden;
  padding: 32px 16px;
  background: var(--login-bg);
}

.login-page__mask {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at top left, rgba(29, 78, 216, 0.14), transparent 32%),
    radial-gradient(circle at bottom right, rgba(15, 118, 110, 0.12), transparent 28%);
  pointer-events: none;
}

.login-page__inner {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 28px;
  align-items: center;
  max-width: 1160px;
  min-height: calc(100vh - 64px);
  margin: 0 auto;
}

.login-page__intro {
  max-width: 640px;
  color: var(--login-title);
}

.login-page__eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--login-accent);
  text-transform: uppercase;
}

.login-page__intro h1 {
  margin: 0;
  font-size: clamp(32px, 4vw, 46px);
  line-height: 1.15;
}

.login-page__summary {
  max-width: 560px;
  margin: 18px 0 0;
  font-size: 16px;
  line-height: 1.8;
  color: var(--login-text);
}

.login-page__panel,
.login-card {
  border: 1px solid var(--login-panel-border);
  background: var(--login-panel-bg);
  backdrop-filter: blur(12px);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.08);
}

.login-page__panel {
  margin-top: 28px;
  padding: 24px 28px;
  border-radius: 24px;
}

.login-page__panel-title {
  margin: 0 0 16px;
  font-size: 14px;
  font-weight: 700;
  color: var(--login-title);
}

.login-page__feature-list {
  display: grid;
  gap: 14px;
  margin: 0;
  padding-left: 20px;
  color: var(--login-text);
  line-height: 1.8;
}

.login-card {
  border-radius: 24px;
}

.login-card__heading {
  margin-bottom: 20px;
}

.login-card__heading h2 {
  margin: 0 0 8px;
  font-size: 26px;
  color: var(--login-title);
}

.login-card__heading p {
  margin: 0;
  line-height: 1.7;
  color: var(--login-muted);
}

.login-card__alert {
  margin-bottom: 16px;
}

.login-card__submit {
  width: 100%;
}

.login-card__actions {
  display: grid;
  gap: 12px;
}

@media (max-width: 960px) {
  .login-page {
    padding: 20px 12px;
  }

  .login-page__inner {
    grid-template-columns: 1fr;
    align-items: stretch;
    min-height: auto;
  }

  .login-page__intro {
    max-width: none;
  }

  .login-page__panel {
    margin-top: 20px;
    padding: 20px;
  }
}
</style>
