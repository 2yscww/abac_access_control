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
    ElMessage.warning('Current account cannot create a project under the active phase or security rules')
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
    ElMessage.warning('The selected phase or security level is not allowed for this account')
    return
  }

  createSubmitting.value = true

  try {
    await createProject({
      ...createForm,
      ownerId: createForm.ownerId ? Number(createForm.ownerId) : null,
    })
    ElMessage.success('Project created')
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
    ElMessage.warning('Current account cannot delete this project')
    return
  }

  try {
    await ElMessageBox.confirm(
      'Delete this project? Related project assets will be removed together.',
      'Delete Project',
      {
        type: 'warning',
        confirmButtonText: 'Delete',
        cancelButtonText: 'Cancel',
      },
    )
  } catch {
    return
  }

  try {
    await deleteProject(projectId)
    ElMessage.success('Project deleted')

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
        <p class="projects-hero__eyebrow">Project Overview</p>
        <h2 class="projects-hero__title">ABAC-filtered project workspace</h2>
        <p class="projects-hero__description">
          This page shows only the projects visible to the current account and exposes only the
          create and delete actions that still satisfy the backend phase and security rules.
        </p>
      </div>
      <div class="projects-hero__actions">
        <el-button plain @click="loadProjects" :loading="loading">Refresh</el-button>
        <el-button v-if="canOpenCreateDialog" type="primary" @click="openCreateDialog">
          Create Project
        </el-button>
      </div>
    </section>

    <section class="projects-summary">
      <el-card shadow="never" class="summary-card">
        <el-statistic title="Visible Projects" :value="visibleCount" />
        <p>Projects currently visible after the ABAC read decision is applied.</p>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <el-statistic title="Total Matches" :value="total" />
        <p>Total records returned by the current query window.</p>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <el-statistic title="High Security" :value="confidentialCount" />
        <p>Confidential and top-secret projects remain constrained by level and time rules.</p>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <el-statistic title="Missing Owner" :value="ownerMissingCount" />
        <p>Useful for identifying incomplete stage-owner data before a demo run.</p>
      </el-card>
    </section>

    <section class="projects-grid">
      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-card__header">
            <div>
              <h3>Filters</h3>
              <p>Query projects by name, phase, and security level.</p>
            </div>
          </div>
        </template>

        <el-form label-position="top" class="query-form">
          <el-form-item label="Project Name">
            <el-input v-model="filters.projectName" clearable placeholder="Supports fuzzy matching" />
          </el-form-item>
          <el-form-item label="Project Phase">
            <el-select v-model="filters.projectPhase" clearable placeholder="All phases">
              <el-option
                v-for="item in projectPhaseOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="Security Level">
            <el-select v-model="filters.securityLevel" clearable placeholder="All levels">
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
          <el-button type="primary" @click="filters.pageNum = 1; loadProjects()">Query</el-button>
          <el-button plain @click="resetFilters">Reset</el-button>
        </div>
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-card__header">
            <div>
              <h3>Phase Distribution</h3>
              <p>Quickly review which lifecycle stages dominate the current result set.</p>
            </div>
            <el-tag type="warning" effect="light" round>Bar</el-tag>
          </div>
        </template>
        <InsightChart :option="phaseChartOption" height="308px" />
      </el-card>
    </section>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="panel-card__header">
          <div>
            <h3>Projects</h3>
            <p>
              The delete entry point is hidden unless the current account still satisfies the same
              mutation conditions enforced by the backend.
            </p>
          </div>
          <el-tag type="info" effect="light">Total {{ total }}</el-tag>
        </div>
      </template>

      <el-table v-loading="loading" :data="projects" stripe>
        <el-table-column prop="projectName" label="Project Name" min-width="180" />
        <el-table-column label="Phase" min-width="130">
          <template #default="{ row }">
            <el-tag :type="getPhaseTagType(row.projectPhase)" effect="light">
              {{ getOptionLabel(projectPhaseOptions, row.projectPhase) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Security" min-width="120">
          <template #default="{ row }">
            <el-tag :type="getSecurityTagType(row.securityLevel)" effect="light">
              {{ getOptionLabel(securityLevelOptions, row.securityLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdByEmployeeId" label="Created By" min-width="110" />
        <el-table-column prop="ownerId" label="Current Owner" min-width="120" />
        <el-table-column prop="createdAt" label="Created At" min-width="180" />
        <el-table-column label="Action" width="180" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button
                type="primary"
                link
                @click="router.push({ name: 'project-detail', params: { id: row.projectId } })"
              >
                Details
              </el-button>
              <el-button
                v-if="canDeleteProject(row)"
                type="danger"
                link
                @click="handleDeleteProject(row.projectId)"
              >
                Delete
              </el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && projects.length === 0" description="No project is visible under the current filter." />

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
      title="Create Project"
      @closed="resetCreateForm"
    >
      <el-alert
        title="Only phases and security levels that still satisfy the current account rules are kept in this form."
        type="warning"
        show-icon
        :closable="false"
        class="create-alert"
      />

      <el-form label-position="top">
        <el-form-item label="Project Name">
          <el-input v-model="createForm.projectName" placeholder="Enter a project name" />
        </el-form-item>
        <el-form-item label="Initial Phase">
          <el-select v-model="createForm.projectPhase" placeholder="Select a phase">
            <el-option
              v-for="item in allowedCreatePhaseOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Security Level">
          <el-select v-model="createForm.securityLevel" placeholder="Select a security level">
            <el-option
              v-for="item in allowedCreateSecurityOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Current Owner ID">
          <el-input v-model="createForm.ownerId" placeholder="Enter the employee ID for the current phase owner" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createDialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="handleCreateProject">
          Submit
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
