<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { queryAuditLogs } from '@/api/audit'
import InsightChart from '@/components/InsightChart.vue'
import { getOptionLabel, securityLevelOptions } from '@/constants/options'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const actionOptions = [
  { value: 'READ', label: '读取' },
  { value: 'WRITE', label: '写入' },
  { value: 'DELETE', label: '删除' },
  { value: 'ADVANCE_PHASE', label: '推进阶段' },
  { value: 'OFFBOARD_EMPLOYEE', label: '员工离职' },
  { value: 'ASSIGN_DEPARTMENT_MANAGER', label: '改派部门负责人' },
  { value: 'EXPORT', label: '导出引用' },
]

const decisionOptions = [
  { value: 'ALLOW', label: '允许' },
  { value: 'DENY', label: '拒绝' },
]

const resourceTypeOptions = [
  { value: 'PROJECT', label: '项目' },
  { value: 'ASSET', label: '资产' },
  { value: 'EMPLOYEE', label: '员工' },
  { value: 'DEPARTMENT', label: '部门' },
]

const filters = reactive({
  employeeId: '',
  projectId: '',
  resourceType: '',
  action: '',
  decision: '',
  securityLevel: '',
  pageNum: 1,
  pageSize: 10,
})

const timeRange = ref([])
const logs = ref([])
const total = ref(0)
const loading = ref(false)
const detailDialogVisible = ref(false)
const selectedLog = ref(null)
const canViewAudit = computed(() => authStore.hasCapability('audit.view'))

const denyCount = computed(() => logs.value.filter((item) => item.decision === 'DENY').length)
const allowCount = computed(() => logs.value.filter((item) => item.decision === 'ALLOW').length)
const handoverEventCount = computed(
  () =>
    logs.value.filter((item) =>
      ['OFFBOARD_EMPLOYEE', 'ASSIGN_DEPARTMENT_MANAGER'].includes(item.action),
    ).length,
)

const actionChartOption = computed(() => {
  const counts = actionOptions
    .map((option) => ({
      label: option.label,
      value: logs.value.filter((item) => item.action === option.value).length,
    }))
    .filter((item) => item.value > 0)

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
      },
    },
    grid: {
      top: 16,
      right: 18,
      bottom: 28,
      left: 36,
    },
    xAxis: {
      type: 'category',
      data: counts.map((item) => item.label),
      axisLabel: {
        color: '#64748b',
        rotate: 18,
      },
      axisTick: {
        show: false,
      },
      axisLine: {
        lineStyle: {
          color: 'rgba(100, 116, 139, 0.22)',
        },
      },
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#64748b',
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(148, 163, 184, 0.18)',
        },
      },
    },
    series: [
      {
        type: 'bar',
        barWidth: 24,
        data: counts.map((item) => item.value),
        itemStyle: {
          borderRadius: [10, 10, 0, 0],
          color(params) {
            const palette = ['#0f766e', '#2563eb', '#f59e0b', '#ef4444', '#7c3aed']
            return palette[params.dataIndex % palette.length]
          },
        },
      },
    ],
  }
})

function getTextLabel(options, rawValue) {
  return options.find((option) => option.value === rawValue)?.label || rawValue || '-'
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ') : '-'
}

function formatDetail(detailJson) {
  if (!detailJson) {
    return '无结构化明细'
  }

  try {
    return JSON.stringify(JSON.parse(detailJson), null, 2)
  } catch {
    return detailJson
  }
}

function buildQuery() {
  return {
    employeeId: filters.employeeId ? Number(filters.employeeId) : '',
    projectId: filters.projectId ? Number(filters.projectId) : '',
    resourceType: filters.resourceType,
    action: filters.action,
    decision: filters.decision,
    securityLevel: filters.securityLevel ? Number(filters.securityLevel) : '',
    startTime: timeRange.value?.[0] || '',
    endTime: timeRange.value?.[1] || '',
    pageNum: filters.pageNum,
    pageSize: filters.pageSize,
  }
}

async function loadAuditLogs() {
  if (!canViewAudit.value) {
    logs.value = []
    total.value = 0
    return
  }

  loading.value = true

  try {
    const data = await queryAuditLogs(buildQuery())
    logs.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    logs.value = []
    total.value = 0
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.employeeId = ''
  filters.projectId = ''
  filters.resourceType = ''
  filters.action = ''
  filters.decision = ''
  filters.securityLevel = ''
  filters.pageNum = 1
  timeRange.value = []
  loadAuditLogs()
}

function handlePageChange(pageNum) {
  filters.pageNum = pageNum
  loadAuditLogs()
}

function openDetail(log) {
  selectedLog.value = log
  detailDialogVisible.value = true
}

onMounted(() => {
  if (canViewAudit.value) {
    loadAuditLogs()
  }
})
</script>

<template>
  <div class="audit-page">
    <section class="audit-hero">
      <div>
        <p class="audit-hero__eyebrow">审计日志</p>
        <h2 class="audit-hero__title">授权决策与业务交接审计</h2>
        <p class="audit-hero__description">
          这里既能查看权限放行和拒绝，也能查看离职、改派等业务交接事件，是整套系统“可追溯性”的证据层。
        </p>
      </div>
      <el-button plain :loading="loading" @click="loadAuditLogs">刷新日志</el-button>
    </section>

    <section class="audit-summary">
      <el-card shadow="never" class="summary-card">
        <el-statistic title="当前页日志数" :value="logs.length" />
        <p>本页实际展示的审计记录条数。</p>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <el-statistic title="允许决策" :value="allowCount" />
        <p>当前页内被允许执行的动作数量。</p>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <el-statistic title="拒绝决策" :value="denyCount" />
        <p>当前页内被拒绝的授权请求数量。</p>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <el-statistic title="交接事件" :value="handoverEventCount" />
        <p>离职与负责人改派事件在当前页中的统计数量。</p>
      </el-card>
    </section>

    <section class="audit-grid">
      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-card__header">
            <div>
              <h3>查询条件</h3>
              <p>按照操作人、项目、动作、判定结果和时间窗口收缩审计范围。</p>
            </div>
          </div>
        </template>

        <el-form label-position="top" class="query-form">
          <el-form-item label="员工 ID">
            <el-input v-model="filters.employeeId" clearable placeholder="请输入员工 ID" />
          </el-form-item>
          <el-form-item label="项目 ID">
            <el-input v-model="filters.projectId" clearable placeholder="请输入项目 ID" />
          </el-form-item>
          <el-form-item label="资源类型">
            <el-select v-model="filters.resourceType" clearable placeholder="全部资源类型">
              <el-option
                v-for="item in resourceTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="动作">
            <el-select v-model="filters.action" clearable placeholder="全部动作">
              <el-option
                v-for="item in actionOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="判定结果">
            <el-select v-model="filters.decision" clearable placeholder="全部结果">
              <el-option
                v-for="item in decisionOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="密级">
            <el-select v-model="filters.securityLevel" clearable placeholder="全部密级">
              <el-option
                v-for="item in securityLevelOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="时间范围" class="query-form__range">
            <el-date-picker
              v-model="timeRange"
              type="datetimerange"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </el-form-item>
        </el-form>

        <div class="query-actions">
          <el-button type="primary" @click="filters.pageNum = 1; loadAuditLogs()">查询</el-button>
          <el-button plain @click="resetFilters">重置</el-button>
        </div>
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-card__header">
            <div>
              <h3>当前页动作分布</h3>
              <p>适合展示当前查询窗口内的访问热点与交接动作分布。</p>
            </div>
            <el-tag type="warning" effect="light" round>柱状图</el-tag>
          </div>
        </template>
        <InsightChart :option="actionChartOption" height="320px" />
      </el-card>
    </section>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="panel-card__header">
          <div>
            <h3>审计结果</h3>
            <p>既记录授权决策，也承载业务交接过程中的结构化明细。</p>
          </div>
          <el-tag type="info" effect="light">共 {{ total }} 条</el-tag>
        </div>
      </template>

      <el-table v-loading="loading" :data="logs" stripe>
        <el-table-column prop="requestTime" label="时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.requestTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="employeeId" label="员工" min-width="90" />
        <el-table-column label="动作" min-width="140">
          <template #default="{ row }">
            <el-tag effect="light">{{ getTextLabel(actionOptions, row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="判定" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.decision === 'DENY' ? 'danger' : 'success'" effect="light">
              {{ getTextLabel(decisionOptions, row.decision) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="资源" min-width="140">
          <template #default="{ row }">
            {{ getTextLabel(resourceTypeOptions, row.resourceType) }} #{{ row.resourceId || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="projectId" label="项目" min-width="90" />
        <el-table-column label="密级" min-width="100">
          <template #default="{ row }">
            {{ getOptionLabel(securityLevelOptions, row.securityLevel) }}
          </template>
        </el-table-column>
        <el-table-column prop="triggerPolicy" label="触发策略" min-width="160" />
        <el-table-column prop="denyReason" label="拒绝原因" min-width="180" />
        <el-table-column label="详情" width="110" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && logs.length === 0" description="当前筛选条件下没有命中审计日志" />

      <div class="table-card__footer">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :current-page="filters.pageNum"
          :page-size="filters.pageSize"
          :total="total"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailDialogVisible" width="760px" destroy-on-close title="审计详情">
      <template v-if="selectedLog">
        <div class="detail-meta">
          <el-tag effect="light">{{ getTextLabel(actionOptions, selectedLog.action) }}</el-tag>
          <el-tag :type="selectedLog.decision === 'DENY' ? 'danger' : 'success'" effect="light">
            {{ getTextLabel(decisionOptions, selectedLog.decision) }}
          </el-tag>
          <span>{{ formatDateTime(selectedLog.requestTime) }}</span>
        </div>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="员工 ID">{{ selectedLog.employeeId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="项目 ID">{{ selectedLog.projectId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="资源类型">
            {{ getTextLabel(resourceTypeOptions, selectedLog.resourceType) }}
          </el-descriptions-item>
          <el-descriptions-item label="资源 ID">{{ selectedLog.resourceId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="请求地址">{{ selectedLog.requestUri || '-' }}</el-descriptions-item>
          <el-descriptions-item label="请求 IP">{{ selectedLog.requestIp || '-' }}</el-descriptions-item>
          <el-descriptions-item label="触发策略">{{ selectedLog.triggerPolicy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="拒绝原因">{{ selectedLog.denyReason || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-block">
          <h4>结构化明细</h4>
          <pre>{{ formatDetail(selectedLog.detailJson) }}</pre>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.audit-page {
  display: grid;
  gap: 18px;
}

.audit-hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  padding: 20px 24px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #ffffff;
}

.audit-hero__eyebrow {
  margin: 0 0 10px;
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 600;
}

.audit-hero__title {
  margin: 0 0 10px;
  color: #303133;
  font-size: 24px;
  line-height: 1.4;
}

.audit-hero__description {
  margin: 0;
  max-width: 760px;
  color: #606266;
  line-height: 1.7;
}

.audit-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.audit-grid {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 16px;
}

.summary-card,
.panel-card,
.table-card {
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

.panel-card__header {
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

.query-form {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.query-form__range {
  grid-column: 1 / -1;
}

.query-actions,
.table-card__footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.table-card__footer {
  margin-top: 18px;
}

.detail-meta {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 18px;
  color: #475569;
}

.detail-block {
  margin-top: 18px;
}

.detail-block h4 {
  margin: 0 0 10px;
  color: #111827;
}

.detail-block pre {
  margin: 0;
  padding: 16px;
  border-radius: 18px;
  background: #0f172a;
  color: #e2e8f0;
  white-space: pre-wrap;
  word-break: break-word;
  overflow: auto;
}

@media (max-width: 1180px) {
  .audit-summary,
  .audit-grid,
  .query-form {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .audit-hero {
    flex-direction: column;
    padding: 22px;
  }
}
</style>
