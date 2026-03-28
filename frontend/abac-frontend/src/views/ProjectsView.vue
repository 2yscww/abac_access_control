<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

import InsightChart from '@/components/InsightChart.vue'
import { createProject, deleteProject, queryProjects } from '@/api/project'
import {
  findOption,
  getOptionLabel,
  projectPhaseOptions,
  securityLevelOptions,
} from '@/constants/options'
import { useAuthStore } from '@/stores/auth'
import {
  canCreateProject,
  canMutateProject,
  getAllowedProjectPhaseOptions,
  getAllowedSecurityLevelOptions,
} from '@/utils/accessControl'

const router = useRouter()
const authStore = useAuthStore()

const filters = reactive({
  projectName: '',
  projectPhase: '',
  securityLevel: '',
  pageNum: 1,
  pageSize: 8,
})

const createForm = reactive({
  projectName: '',
  projectPhase: '',
  securityLevel: '',
  ownerId: '',
})

const loading = ref(false)
const createSubmitting = ref(false)
const projects = ref([])
const total = ref(0)
const createDialogVisible = ref(false)

const currentProfile = computed(() => authStore.profile || null)
const currentEmployeeId = computed(() =>
  Number(currentProfile.value?.employeeId || authStore.employeeId || 0),
)
const assumeProjectMembership = computed(
  () => currentProfile.value?.deptType && currentProfile.value.deptType !== 'MANAGEMENT',
)

const allowedCreatePhaseOptions = computed(() =>
  getAllowedProjectPhaseOptions(currentProfile.value),
)
const allowedCreateSecurityOptions = computed(() =>
  getAllowedSecurityLevelOptions(currentProfile.value),
)
const canOpenCreateDialog = computed(
  () =>
    allowedCreatePhaseOptions.value.length > 0 && allowedCreateSecurityOptions.value.length > 0,
)

const visibleCount = computed(() => projects.value.length)
const confidentialCount = computed(
  () =>
    projects.value.filter((project) =>
      ['CONFIDENTIAL', 'TOP_SECRET', 3, 4].includes(project.securityLevel),
    ).length,
)
const ownerMissingCount = computed(
  () => projects.value.filter((project) => !project.ownerId).length,
)

const phaseChartOption = computed(() => {
  const counts = projectPhaseOptions.map((option) => ({
    label: option.label,
    value: projects.value.filter((project) => {
      const matched = findOption(projectPhaseOptions, project.projectPhase)
      return matched?.key === option.key
    }).length,
  }))

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
      bottom: 36,
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
        barWidth: 22,
        data: counts.map((item) => item.value),
        itemStyle: {
          borderRadius: [10, 10, 0, 0],
          color(params) {
            const palette = ['#0f766e', '#14b8a6', '#f59e0b', '#f97316', '#2563eb', '#64748b']
            return palette[params.dataIndex % palette.length]
          },
        },
      },
    ],
  }
})

function getPhaseTagType(rawValue) {
  const key = findOption(projectPhaseOptions, rawValue)?.key
  switch (key) {
    case 'INIT':
      return ''
    case 'REQUIREMENT':
      return 'warning'
    case 'DEVELOPMENT':
      return 'success'
    case 'TEST':
      return 'warning'
    case 'RELEASE':
      return 'success'
    case 'ARCHIVED':
      return 'info'
    default:
      return ''
  }
}

function getSecurityTagType(rawValue) {
  const key = findOption(securityLevelOptions, rawValue)?.key
  switch (key) {
    case 'PUBLIC':
      return 'success'
    case 'INTERNAL':
      return ''
    case 'CONFIDENTIAL':
      return 'warning'
    case 'TOP_SECRET':
      return 'danger'
    default:
      return ''
  }
}

function syncCreateFormOptions() {
  if (!allowedCreatePhaseOptions.value.some((option) => option.value === createForm.projectPhase)) {
    createForm.projectPhase = allowedCreatePhaseOptions.value[0]?.value || ''
  }

  if (
    !allowedCreateSecurityOptions.value.some(
      (option) => option.value === createForm.securityLevel,
    )
  ) {
    createForm.securityLevel = allowedCreateSecurityOptions.value[0]?.value || ''
  }
}

function resetCreateForm() {
  createForm.projectName = ''
  createForm.ownerId = ''
  syncCreateFormOptions()
}

function openCreateDialog() {
  if (!canOpenCreateDialog.value) {
    ElMessage.warning('当前账号在现有阶段或密级规则下不能创建项目')
    return
  }

  resetCreateForm()
  createDialogVisible.value = true
}

function canDeleteProject(project) {
  return canMutateProject(currentProfile.value, project, {
    assumeActiveMember: assumeProjectMembership.value,
    currentEmployeeId: currentEmployeeId.value,
  })
}

async function loadProjects() {
  loading.value = true

  try {
    const data = await queryProjects(filters)
    projects.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    projects.value = []
    total.value = 0
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function handleCreateProject() {
  if (!canCreateProject(currentProfile.value, createForm.projectPhase, createForm.securityLevel)) {
    ElMessage.warning('当前账号不允许使用所选阶段或密级创建项目')
    return
  }

  createSubmitting.value = true

  try {
    await createProject({
      ...createForm,
      ownerId: createForm.ownerId ? Number(createForm.ownerId) : null,
    })
    ElMessage.success('项目创建成功')
    createDialogVisible.value = false
    resetCreateForm()
    filters.pageNum = 1
    await loadProjects()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    createSubmitting.value = false
  }
}

async function handleDeleteProject(projectId) {
  const project = projects.value.find((item) => item.projectId === projectId)
  if (!canDeleteProject(project)) {
    ElMessage.warning('当前账号不能删除该项目')
    return
  }

  try {
    await ElMessageBox.confirm(
      '确认删除该项目吗？相关项目资产会一并删除。',
      '删除项目',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }

  try {
    await deleteProject(projectId)
    ElMessage.success('项目删除成功')

    if (projects.value.length === 1 && filters.pageNum > 1) {
      filters.pageNum -= 1
    }

    await loadProjects()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function resetFilters() {
  filters.projectName = ''
  filters.projectPhase = ''
  filters.securityLevel = ''
  filters.pageNum = 1
  loadProjects()
}

function handlePageChange(pageNum) {
  filters.pageNum = pageNum
  loadProjects()
}

onMounted(() => {
  resetCreateForm()
  loadProjects()
})
</script>

<template>
  <div class="projects-page">
    <section class="projects-hero">
      <div>
        <p class="projects-hero__eyebrow">项目总览</p>
        <h2 class="projects-hero__title">ABAC 过滤后的项目工作区</h2>
        <p class="projects-hero__description">
          当前页面只展示本账号有权访问的项目，同时仅保留仍然满足后端阶段规则与密级规则的创建、删除入口。
        </p>
      </div>
      <div class="projects-hero__actions">
        <el-button plain @click="loadProjects" :loading="loading">刷新</el-button>
        <el-button v-if="canOpenCreateDialog" type="primary" @click="openCreateDialog">
          新建项目
        </el-button>
      </div>
    </section>

    <section class="projects-summary">
      <el-card shadow="never" class="summary-card">
        <el-statistic title="当前页可见项目" :value="visibleCount" />
        <p>展示经过 ABAC 读取决策过滤后，当前账号实际可见的项目数量。</p>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <el-statistic title="命中总数" :value="total" />
        <p>当前查询条件下后端返回的总记录数。</p>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <el-statistic title="高密项目" :value="confidentialCount" />
        <p>机密与绝密项目仍持续受到级别与时间环境规则约束。</p>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <el-statistic title="缺少负责人" :value="ownerMissingCount" />
        <p>适合在演示前检查阶段负责人数据是否完整。</p>
      </el-card>
    </section>

    <section class="projects-grid">
      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-card__header">
            <div>
              <h3>查询条件</h3>
              <p>按项目名称、阶段和密级筛选项目。</p>
            </div>
          </div>
        </template>

        <el-form label-position="top" class="query-form">
          <el-form-item label="项目名称">
            <el-input v-model="filters.projectName" clearable placeholder="支持模糊匹配" />
          </el-form-item>
          <el-form-item label="项目阶段">
            <el-select v-model="filters.projectPhase" clearable placeholder="全部阶段">
              <el-option
                v-for="item in projectPhaseOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="安全密级">
            <el-select v-model="filters.securityLevel" clearable placeholder="全部密级">
              <el-option
                v-for="item in securityLevelOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-form>

        <div class="query-actions">
          <el-button type="primary" @click="filters.pageNum = 1; loadProjects()">查询</el-button>
          <el-button plain @click="resetFilters">重置</el-button>
        </div>
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-card__header">
            <div>
              <h3>阶段分布</h3>
              <p>快速查看当前结果集中各生命周期阶段的分布情况。</p>
            </div>
            <el-tag type="warning" effect="light" round>柱状图</el-tag>
          </div>
        </template>
        <InsightChart :option="phaseChartOption" height="308px" />
      </el-card>
    </section>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="panel-card__header">
          <div>
            <h3>项目列表</h3>
            <p>
              只有当前账号仍满足后端变更条件时，删除入口才会显示出来。
            </p>
          </div>
          <el-tag type="info" effect="light">共 {{ total }} 条</el-tag>
        </div>
      </template>

      <el-table v-loading="loading" :data="projects" stripe>
        <el-table-column prop="projectName" label="项目名称" min-width="180" />
        <el-table-column label="阶段" min-width="130">
          <template #default="{ row }">
            <el-tag :type="getPhaseTagType(row.projectPhase)" effect="light">
              {{ getOptionLabel(projectPhaseOptions, row.projectPhase) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="密级" min-width="120">
          <template #default="{ row }">
            <el-tag :type="getSecurityTagType(row.securityLevel)" effect="light">
              {{ getOptionLabel(securityLevelOptions, row.securityLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdByEmployeeId" label="创建人" min-width="110" />
        <el-table-column prop="ownerId" label="当前负责人" min-width="120" />
        <el-table-column prop="createdAt" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button
                type="primary"
                link
                @click="router.push({ name: 'project-detail', params: { id: row.projectId } })"
              >
                查看详情
              </el-button>
              <el-button
                v-if="canDeleteProject(row)"
                type="danger"
                link
                @click="handleDeleteProject(row.projectId)"
              >
                删除
              </el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && projects.length === 0" description="当前筛选条件下没有可见项目" />

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

    <el-dialog
      v-model="createDialogVisible"
      width="560px"
      destroy-on-close
      title="新建项目"
      @closed="resetCreateForm"
    >
      <el-alert
        title="表单中仅保留当前账号仍被允许使用的阶段与密级。"
        type="warning"
        show-icon
        :closable="false"
        class="create-alert"
      />

      <el-form label-position="top">
        <el-form-item label="项目名称">
          <el-input v-model="createForm.projectName" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="初始阶段">
          <el-select v-model="createForm.projectPhase" placeholder="请选择阶段">
            <el-option
              v-for="item in allowedCreatePhaseOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="安全密级">
          <el-select v-model="createForm.securityLevel" placeholder="请选择密级">
            <el-option
              v-for="item in allowedCreateSecurityOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="当前负责人 ID">
          <el-input v-model="createForm.ownerId" placeholder="请输入当前阶段负责人的员工 ID" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="handleCreateProject">
          提交
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.projects-page {
  display: grid;
  gap: 18px;
}

.projects-hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  padding: 20px 24px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #ffffff;
}

.projects-hero__eyebrow {
  margin: 0 0 10px;
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 600;
}

.projects-hero__title {
  margin: 0 0 10px;
  color: #303133;
  font-size: 24px;
  line-height: 1.4;
}

.projects-hero__description {
  margin: 0;
  max-width: 760px;
  color: #606266;
  line-height: 1.7;
}

.projects-hero__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.projects-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
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

.projects-grid {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 16px;
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

.query-actions,
.table-card__footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.table-card__footer {
  margin-top: 18px;
}

.create-alert {
  margin-bottom: 18px;
}

@media (max-width: 1180px) {
  .projects-grid,
  .projects-summary,
  .query-form {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .projects-hero {
    flex-direction: column;
    padding: 22px;
  }
}
</style>
