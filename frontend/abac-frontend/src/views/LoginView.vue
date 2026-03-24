<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { changePassword, login } from '@/api/auth'
import InsightChart from '@/components/InsightChart.vue'
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

const chartOption = computed(() => ({
  backgroundColor: 'transparent',
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'shadow',
    },
  },
  grid: {
    top: 16,
    right: 12,
    bottom: 24,
    left: 28,
  },
  xAxis: {
    type: 'category',
    data: ['主体身份', '资源密级', '项目阶段', '环境约束', '审计留痕'],
    axisTick: {
      show: false,
    },
    axisLine: {
      lineStyle: {
        color: '#dcdfe6',
      },
    },
    axisLabel: {
      color: '#606266',
      interval: 0,
    },
  },
  yAxis: {
    type: 'value',
    max: 100,
    axisLabel: {
      color: '#909399',
    },
    splitLine: {
      lineStyle: {
        color: '#ebeef5',
      },
    },
  },
  series: [
    {
      type: 'bar',
      barWidth: 26,
      data: [92, 88, 90, 84, 96],
      itemStyle: {
        color: '#409eff',
        borderRadius: [4, 4, 0, 0],
      },
    },
  ],
}))

const modeTitle = computed(() =>
  mode.value === 'login' ? '员工登录' : '首次登录修改密码',
)

const modeDescription = computed(() =>
  mode.value === 'login'
    ? '请输入员工工号和密码。'
    : '首次登录需完成密码修改后才可进入系统。',
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
  <div class="login-page">
    <div class="login-page__inner">
      <el-card shadow="never" class="login-page__overview">
        <div class="login-page__heading">
          <p class="login-page__eyebrow">ABAC Platform</p>
          <h1>权限治理与安全审计系统</h1>
          <p>
            系统围绕员工身份、项目阶段、资源密级和审计记录组织权限控制，前端界面采用传统后台布局展示关键数据。
          </p>
        </div>

        <div class="login-page__tags">
          <el-tag>Element Plus</el-tag>
          <el-tag type="success">ECharts</el-tag>
          <el-tag type="warning">ABAC</el-tag>
        </div>

        <el-card shadow="never" class="login-page__chart-card">
          <template #header>
            <div class="login-page__chart-header">
              <span>控制维度概览</span>
              <span class="login-page__chart-note">示意数据</span>
            </div>
          </template>
          <InsightChart :option="chartOption" height="260px" />
        </el-card>

        <ul class="login-page__list">
          <li>项目页展示阶段分布、列表查询和项目创建。</li>
          <li>资产页展示可见资产、类型分布和按条件过滤。</li>
          <li>审计页展示授权决策与业务交接记录。</li>
        </ul>
      </el-card>

      <el-card shadow="never" class="login-card">
        <div class="login-card__heading">
          <p class="login-page__eyebrow">Account</p>
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
              placeholder="例如 1001"
              @keyup.enter="submitLogin"
            />
          </el-form-item>
          <el-form-item label="登录密码">
            <el-input
              v-model="loginForm.password"
              type="password"
              show-password
              placeholder="请输入登录密码"
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
              @keyup.enter="submitPasswordChange"
            />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              show-password
              placeholder="请设置新的登录密码"
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
  min-height: 100vh;
  padding: 32px 16px;
  background: #f5f7fa;
}

.login-page__inner {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 24px;
  max-width: 1180px;
  margin: 0 auto;
}

.login-page__overview,
.login-card,
.login-page__chart-card {
  border: 1px solid #e4e7ed;
  box-shadow: none;
}

.login-page__overview {
  display: grid;
  gap: 20px;
}

.login-page__heading h1 {
  margin: 0 0 12px;
  font-size: 30px;
  color: #303133;
}

.login-page__heading p {
  margin: 0;
  line-height: 1.8;
  color: #606266;
}

.login-page__eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-color-primary);
  letter-spacing: 0.04em;
}

.login-page__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.login-page__chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.login-page__chart-note {
  font-size: 12px;
  color: #909399;
}

.login-page__list {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  line-height: 1.9;
}

.login-card {
  align-self: start;
}

.login-card__heading {
  margin-bottom: 20px;
}

.login-card__heading h2 {
  margin: 0 0 8px;
  font-size: 24px;
  color: #303133;
}

.login-card__heading p {
  margin: 0;
  color: #909399;
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
  .login-page__inner {
    grid-template-columns: 1fr;
  }
}
</style>
