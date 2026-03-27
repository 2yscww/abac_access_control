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
  return value ? String(value).replace('T', ' ') : 'Code default'
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
    ElMessage.success(`${data.displayName} updated`)
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
        <p class="policy-hero__eyebrow">Policy Admin</p>
        <h2 class="policy-hero__title">Parameterized ABAC controls</h2>
        <p class="policy-hero__description">
          Policy logic remains fixed in the backend, while selected thresholds and switches are
          editable through this page for demo and operations scenarios.
        </p>
      </div>
      <el-button plain :loading="loading" @click="loadPolicyConfigs">Refresh</el-button>
    </section>

    <section class="policy-summary">
      <el-card shadow="never" class="summary-card">
        <el-statistic title="Editable Policies" :value="3" />
        <p>The current build exposes three runtime-tunable security controls.</p>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <el-statistic title="Enabled Policies" :value="enabledPolicyCount" />
        <p>Each policy can be switched on or off without changing the hardcoded logic.</p>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <el-statistic title="Admin Capability" :value="canManagePolicy ? 1 : 0" />
        <p>The policy page is visible only when the current account owns `policy.manage`.</p>
      </el-card>
    </section>

    <el-alert
      v-if="!canManagePolicy"
      title="Current account cannot manage policy parameters."
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
                <h3>Security Thresholds</h3>
                <p>Controls the minimum employee rank required by each security level.</p>
              </div>
              <el-switch v-model="forms.SecurityLevelPolicy.enabled" />
            </div>
          </template>

          <el-form label-position="top" class="policy-form">
            <el-form-item label="PUBLIC minimum rank">
              <el-input
                v-model="forms.SecurityLevelPolicy.conditions.publicMinRank"
                type="number"
              />
            </el-form-item>
            <el-form-item label="INTERNAL minimum rank">
              <el-input
                v-model="forms.SecurityLevelPolicy.conditions.internalMinRank"
                type="number"
              />
            </el-form-item>
            <el-form-item label="CONFIDENTIAL minimum rank">
              <el-input
                v-model="forms.SecurityLevelPolicy.conditions.confidentialMinRank"
                type="number"
              />
            </el-form-item>
            <el-form-item label="TOP_SECRET minimum rank">
              <el-input
                v-model="forms.SecurityLevelPolicy.conditions.topSecretMinRank"
                type="number"
              />
            </el-form-item>
          </el-form>

          <div class="panel-card__footer">
            <span>
              Last updated:
              {{ formatDateTime(policyConfigs.find((item) => item.policyName === 'SecurityLevelPolicy')?.updatedAt) }}
            </span>
            <el-button
              type="primary"
              :loading="Boolean(saving.SecurityLevelPolicy)"
              @click="savePolicy('SecurityLevelPolicy')"
            >
              Save
            </el-button>
          </div>
        </el-card>

        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-card__header">
              <div>
                <h3>Working Hours</h3>
                <p>Controls the time window for sensitive access checked by the environment policy.</p>
              </div>
              <el-switch v-model="forms.EnvironmentAccessPolicy.enabled" />
            </div>
          </template>

          <el-form label-position="top" class="policy-form">
            <el-form-item label="workStart (HH:mm)">
              <el-input v-model="forms.EnvironmentAccessPolicy.conditions.workStart" />
            </el-form-item>
            <el-form-item label="workEnd (HH:mm)">
              <el-input v-model="forms.EnvironmentAccessPolicy.conditions.workEnd" />
            </el-form-item>
          </el-form>

          <div class="panel-card__footer">
            <span>
              Last updated:
              {{ formatDateTime(policyConfigs.find((item) => item.policyName === 'EnvironmentAccessPolicy')?.updatedAt) }}
            </span>
            <el-button
              type="primary"
              :loading="Boolean(saving.EnvironmentAccessPolicy)"
              @click="savePolicy('EnvironmentAccessPolicy')"
            >
              Save
            </el-button>
          </div>
        </el-card>

        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-card__header">
              <div>
                <h3>Export Guard</h3>
                <p>Controls the export threshold and rolling time window used by the history policy.</p>
              </div>
              <el-switch v-model="forms.HistoricalExportPolicy.enabled" />
            </div>
          </template>

          <el-form label-position="top" class="policy-form">
            <el-form-item label="exportThreshold">
              <el-input
                v-model="forms.HistoricalExportPolicy.conditions.exportThreshold"
                type="number"
              />
            </el-form-item>
            <el-form-item label="exportWindowMinutes">
              <el-input
                v-model="forms.HistoricalExportPolicy.conditions.exportWindowMinutes"
                type="number"
              />
            </el-form-item>
          </el-form>

          <div class="panel-card__footer">
            <span>
              Last updated:
              {{ formatDateTime(policyConfigs.find((item) => item.policyName === 'HistoricalExportPolicy')?.updatedAt) }}
            </span>
            <el-button
              type="primary"
              :loading="Boolean(saving.HistoricalExportPolicy)"
              @click="savePolicy('HistoricalExportPolicy')"
            >
              Save
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
