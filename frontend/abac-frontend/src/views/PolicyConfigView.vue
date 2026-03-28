<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { listPolicyConfigs, updatePolicyConfig } from '@/api/policy'
import { useAuthStore } from '@/stores/auth'
import { applyPolicyConfigSnapshot } from '@/utils/accessControl'

const authStore = useAuthStore()

const loading = ref(false)
const policyConfigs = ref([])
const saving = reactive({})

const forms = reactive({
  SecurityLevelPolicy: {
    enabled: true,
    conditions: {
      publicMinRank: 1,
      internalMinRank: 3,
      confidentialMinRank: 5,
      topSecretMinRank: 9,
    },
  },
  EnvironmentAccessPolicy: {
    enabled: true,
    conditions: {
      workStart: '08:00',
      workEnd: '20:00',
    },
  },
  HistoricalExportPolicy: {
    enabled: true,
    conditions: {
      exportThreshold: 50,
      exportWindowMinutes: 30,
    },
  },
})

const canManagePolicy = computed(() => authStore.hasCapability('policy.manage'))
const enabledPolicyCount = computed(
  () => policyConfigs.value.filter((item) => item.enabled !== false).length,
)

function resetForms() {
  forms.SecurityLevelPolicy.enabled = true
  forms.SecurityLevelPolicy.conditions.publicMinRank = 1
  forms.SecurityLevelPolicy.conditions.internalMinRank = 3
  forms.SecurityLevelPolicy.conditions.confidentialMinRank = 5
  forms.SecurityLevelPolicy.conditions.topSecretMinRank = 9

  forms.EnvironmentAccessPolicy.enabled = true
  forms.EnvironmentAccessPolicy.conditions.workStart = '08:00'
  forms.EnvironmentAccessPolicy.conditions.workEnd = '20:00'

  forms.HistoricalExportPolicy.enabled = true
  forms.HistoricalExportPolicy.conditions.exportThreshold = 50
  forms.HistoricalExportPolicy.conditions.exportWindowMinutes = 30
}

function applyConfigs(configs) {
  resetForms()
  policyConfigs.value = configs || []

  policyConfigs.value.forEach((item) => {
    if (!item?.policyName || !forms[item.policyName]) {
      return
    }

    forms[item.policyName].enabled = item.enabled !== false
    Object.assign(forms[item.policyName].conditions, item.conditions || {})
  })
}

function upsertPolicyConfig(updatedConfig) {
  const currentIndex = policyConfigs.value.findIndex(
    (item) => item.policyName === updatedConfig.policyName,
  )

  if (currentIndex >= 0) {
    policyConfigs.value[currentIndex] = updatedConfig
  } else {
    policyConfigs.value = [...policyConfigs.value, updatedConfig]
  }

  applyPolicyConfigSnapshot(policyConfigs.value)
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ') : '代码默认值'
}

function buildPayload(policyName) {
  if (policyName === 'SecurityLevelPolicy') {
    return {
      enabled: forms.SecurityLevelPolicy.enabled,
      conditions: {
        publicMinRank: Number(forms.SecurityLevelPolicy.conditions.publicMinRank),
        internalMinRank: Number(forms.SecurityLevelPolicy.conditions.internalMinRank),
        confidentialMinRank: Number(forms.SecurityLevelPolicy.conditions.confidentialMinRank),
        topSecretMinRank: Number(forms.SecurityLevelPolicy.conditions.topSecretMinRank),
      },
    }
  }

  if (policyName === 'EnvironmentAccessPolicy') {
    return {
      enabled: forms.EnvironmentAccessPolicy.enabled,
      conditions: {
        workStart: forms.EnvironmentAccessPolicy.conditions.workStart,
        workEnd: forms.EnvironmentAccessPolicy.conditions.workEnd,
      },
    }
  }

  return {
    enabled: forms.HistoricalExportPolicy.enabled,
    conditions: {
      exportThreshold: Number(forms.HistoricalExportPolicy.conditions.exportThreshold),
      exportWindowMinutes: Number(forms.HistoricalExportPolicy.conditions.exportWindowMinutes),
    },
  }
}

async function loadPolicyConfigs() {
  if (!canManagePolicy.value) {
    policyConfigs.value = []
    return
  }

  loading.value = true

  try {
    const data = await listPolicyConfigs()
    applyConfigs(data)
    applyPolicyConfigSnapshot(data || [])
  } catch (error) {
    policyConfigs.value = []
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function savePolicy(policyName) {
  saving[policyName] = true

  try {
    const data = await updatePolicyConfig(policyName, buildPayload(policyName))
    upsertPolicyConfig(data)
    ElMessage.success(`${data.displayName} 已更新`)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving[policyName] = false
  }
}

onMounted(() => {
  loadPolicyConfigs()
})
</script>

<template>
  <div class="policy-page">
    <section class="policy-hero">
      <div>
        <p class="policy-hero__eyebrow">策略配置</p>
        <h2 class="policy-hero__title">参数化 ABAC 控制面板</h2>
        <p class="policy-hero__description">
          ABAC 的核心策略逻辑固定在后端代码中，这里只开放部分阈值和开关，便于演示和运维调参。
        </p>
      </div>
      <el-button plain :loading="loading" @click="loadPolicyConfigs">刷新</el-button>
    </section>

    <section class="policy-summary">
      <el-card shadow="never" class="summary-card">
        <el-statistic title="可调策略数" :value="3" />
        <p>当前版本开放了 3 组可在运行期调整的安全控制参数。</p>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <el-statistic title="已启用策略" :value="enabledPolicyCount" />
        <p>每项策略都可以独立启停，而不需要改动后端硬编码逻辑。</p>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <el-statistic title="管理权限" :value="canManagePolicy ? 1 : 0" />
        <p>只有当前账号具备 `policy.manage` 能力时，才可以访问并修改本页内容。</p>
      </el-card>
    </section>

    <el-alert
      v-if="!canManagePolicy"
      title="当前账号没有策略参数管理权限。"
      type="warning"
      show-icon
      :closable="false"
    />

    <template v-else>
      <section class="policy-grid">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-card__header">
              <div>
                <h3>密级阈值</h3>
                <p>控制不同安全密级对应的员工最低职级要求。</p>
              </div>
              <el-switch v-model="forms.SecurityLevelPolicy.enabled" />
            </div>
          </template>

          <el-form label-position="top" class="policy-form">
            <el-form-item label="公开级最低职级">
              <el-input
                v-model="forms.SecurityLevelPolicy.conditions.publicMinRank"
                type="number"
              />
            </el-form-item>
            <el-form-item label="内部级最低职级">
              <el-input
                v-model="forms.SecurityLevelPolicy.conditions.internalMinRank"
                type="number"
              />
            </el-form-item>
            <el-form-item label="机密级最低职级">
              <el-input
                v-model="forms.SecurityLevelPolicy.conditions.confidentialMinRank"
                type="number"
              />
            </el-form-item>
            <el-form-item label="绝密级最低职级">
              <el-input
                v-model="forms.SecurityLevelPolicy.conditions.topSecretMinRank"
                type="number"
              />
            </el-form-item>
          </el-form>

          <div class="panel-card__footer">
            <span>
              最近更新：
              {{ formatDateTime(policyConfigs.find((item) => item.policyName === 'SecurityLevelPolicy')?.updatedAt) }}
            </span>
            <el-button
              type="primary"
              :loading="Boolean(saving.SecurityLevelPolicy)"
              @click="savePolicy('SecurityLevelPolicy')"
            >
              保存
            </el-button>
          </div>
        </el-card>

        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-card__header">
              <div>
                <h3>工作时间窗口</h3>
                <p>控制环境策略校验高敏访问时所采用的时间范围。</p>
              </div>
              <el-switch v-model="forms.EnvironmentAccessPolicy.enabled" />
            </div>
          </template>

          <el-form label-position="top" class="policy-form">
            <el-form-item label="开始时间（HH:mm）">
              <el-input v-model="forms.EnvironmentAccessPolicy.conditions.workStart" />
            </el-form-item>
            <el-form-item label="结束时间（HH:mm）">
              <el-input v-model="forms.EnvironmentAccessPolicy.conditions.workEnd" />
            </el-form-item>
          </el-form>

          <div class="panel-card__footer">
            <span>
              最近更新：
              {{ formatDateTime(policyConfigs.find((item) => item.policyName === 'EnvironmentAccessPolicy')?.updatedAt) }}
            </span>
            <el-button
              type="primary"
              :loading="Boolean(saving.EnvironmentAccessPolicy)"
              @click="savePolicy('EnvironmentAccessPolicy')"
            >
              保存
            </el-button>
          </div>
        </el-card>

        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-card__header">
              <div>
                <h3>导出保护</h3>
                <p>控制历史导出策略使用的导出阈值和滚动时间窗口。</p>
              </div>
              <el-switch v-model="forms.HistoricalExportPolicy.enabled" />
            </div>
          </template>

          <el-form label-position="top" class="policy-form">
            <el-form-item label="导出阈值">
              <el-input
                v-model="forms.HistoricalExportPolicy.conditions.exportThreshold"
                type="number"
              />
            </el-form-item>
            <el-form-item label="统计窗口（分钟）">
              <el-input
                v-model="forms.HistoricalExportPolicy.conditions.exportWindowMinutes"
                type="number"
              />
            </el-form-item>
          </el-form>

          <div class="panel-card__footer">
            <span>
              最近更新：
              {{ formatDateTime(policyConfigs.find((item) => item.policyName === 'HistoricalExportPolicy')?.updatedAt) }}
            </span>
            <el-button
              type="primary"
              :loading="Boolean(saving.HistoricalExportPolicy)"
              @click="savePolicy('HistoricalExportPolicy')"
            >
              保存
            </el-button>
          </div>
        </el-card>
      </section>
    </template>
  </div>
</template>

<style scoped>
.policy-page {
  display: grid;
  gap: 18px;
}

.policy-hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  padding: 20px 24px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #ffffff;
}

.policy-hero__eyebrow {
  margin: 0 0 10px;
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 600;
}

.policy-hero__title {
  margin: 0 0 10px;
  color: #303133;
  font-size: 24px;
  line-height: 1.4;
}

.policy-hero__description {
  margin: 0;
  max-width: 760px;
  color: #606266;
  line-height: 1.7;
}

.policy-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.policy-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.summary-card,
.panel-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: none;
}

.summary-card p {
  margin: 12px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.panel-card__header,
.panel-card__footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.panel-card__header h3 {
  margin: 0 0 6px;
  color: #111827;
  font-size: 20px;
}

.panel-card__header p {
  margin: 0;
  color: #64748b;
  line-height: 1.6;
}

.panel-card__footer {
  margin-top: 18px;
  color: #64748b;
  flex-wrap: wrap;
}

.policy-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.policy-form :deep(.el-form-item) {
  margin-bottom: 0;
}

@media (max-width: 1180px) {
  .policy-summary,
  .policy-grid,
  .policy-form {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .policy-hero {
    flex-direction: column;
    padding: 22px;
  }
}
</style>
